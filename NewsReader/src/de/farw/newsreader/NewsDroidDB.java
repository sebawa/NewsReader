package de.farw.newsreader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class NewsDroidDB {

	private static final String FEEDS_TABLE = "feeds";
	private static final String ARTICLES_TABLE = "articles";
	private SQLiteDatabase db;
	private Context context;
	private NewsDroidDBHelper helper;

	public NewsDroidDB(Context ctx) {
		context = ctx;
	}

	public NewsDroidDB open() throws SQLException {
		try {
		helper = new NewsDroidDBHelper(context);
		db = helper.getWritableDatabase();
		} catch(RuntimeException e) {
			Log.e("NewsDroid", e.toString());
		}
		return this;
	}

	public void close() {
		helper.close();
	}

	public boolean insertFeed(String title, URL url) {
		ContentValues values = new ContentValues();
		values.put("title", title);
		values.put("url", url.toString());
		return (db.insert(FEEDS_TABLE, null, values) > 0);
	}

	public boolean deleteFeed(Long feedId) {
		return (db.delete(FEEDS_TABLE, "feed_id=" + feedId.toString(), null) > 0);
	}

	public boolean insertArticle(Long feedId, String title, URL url, String description, Date date) {
		ContentValues values = new ContentValues();

		try {
			Cursor c = db.query(ARTICLES_TABLE, // check if feed is already in database
					new String[] { "title", "url" }, "url=\"" + url.toString() + "\"",
					null, null, null, null);
			int count = c.getCount();
			c.close();
			if (count >= 1)
				return true;
		} catch (RuntimeException e) {
			Log.e("NewsDroid", e.toString());
		}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(date);
		long epochTime = cal.getTimeInMillis();

		values.put("feed_id", feedId);
		values.put("title", title);
		values.put("url", url.toString());
		values.put("read", 0);
		values.put("description", description);
		values.put("date", epochTime);
		values.put("known", 0);
		
		return (db.insert(ARTICLES_TABLE, null, values) > 0);
	}

	public boolean deleteAricles(Long feedId) {
		return (db.delete(ARTICLES_TABLE, "feed_id=" + feedId.toString(), null) > 0);
	}

	public List<Feed> getFeeds() {
		ArrayList<Feed> feeds = new ArrayList<Feed>();
		try {
			Cursor c = db.query(FEEDS_TABLE, new String[] { "feed_id", "title",
					"url" }, null, null, null, null, null);

			int numRows = c.getCount();
			c.moveToFirst();
			for (int i = 0; i < numRows; ++i) {
				Feed feed = new Feed();
				feed.feedId = c.getLong(0);
				feed.title = c.getString(1);
				feed.url = new URL(c.getString(2));
				feeds.add(feed);
				c.moveToNext();
			}
			c.close();
		} catch (SQLException e) {
			Log.e("NewsDroid", e.toString());
		} catch (MalformedURLException e) {
			Log.e("NewsDroid", e.toString());
		}
		return feeds;
	}

	public List<Article> getArticles(Long feedId) {
		ArrayList<Article> articles = new ArrayList<Article>();
		try {
			Cursor c = null;
			if (feedId >= 0) {
				c = db.query(ARTICLES_TABLE, new String[] { "article_id",
						"feed_id", "title", "url", "description", "date"}, "feed_id="
						+ feedId.toString(), null, null, null, null);
			} else {
				c = db.query(ARTICLES_TABLE, new String[] { "article_id",
						"feed_id", "title", "url", "description", "date" }, "read=0", null, null,
						null, null);
			}
			int numRows = c.getCount();
			c.moveToFirst();
			for (int i = 0; i < numRows; ++i) {
				Article article = new Article();
				article.articleId = c.getLong(0);
				article.feedId = c.getLong(1);
				article.title = c.getString(2);
				article.url = new URL(c.getString(3));
				article.description = c.getString(4);
				article.date = new Date(c.getLong(5));
				articles.add(article);
				c.moveToNext();
			}
			c.close();
		} catch (SQLException e) {
			Log.e("NewsDroid", e.toString());
		} catch (MalformedURLException e) {
			Log.e("NewsDroid", e.toString());
		}
		return articles;
	}

	public void setRead(Long articleId) {
		ContentValues values = new ContentValues();
		values.put("read", 1);
		db.update(ARTICLES_TABLE, values, "article_id=" + articleId, null);
	}

	public ArrayList<String> getDescriptionById(ArrayList<Long> otherArticlesId) {
		ArrayList<String> descriptions = new ArrayList<String>();
		String inQuery = otherArticlesId.toString();
		inQuery = inQuery.replace('[', '(');
		inQuery = inQuery.replace(']', ')');
		try {
			Cursor c = null;
			c = db.query(ARTICLES_TABLE, new String[] { "description"} , "article_id IN" + inQuery, 
				null, null, null, null, null);
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); ++i) {
				descriptions.add(c.getString(i));
				c.moveToNext();
			}
			c.close();
		} catch (SQLException e) {
			Log.e("NewsDroid", e.toString());
		}
		return descriptions;
	}
	
	public void markAsKnown(Long articleId) {
		ContentValues values = new ContentValues();
		values.put("known", 1);
		db.update(ARTICLES_TABLE, values, "article_id=" + articleId, null);
	}
}
