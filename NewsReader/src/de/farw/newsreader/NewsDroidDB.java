package de.farw.newsreader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
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
	private long time;

	public NewsDroidDB(Context ctx) {
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(GregorianCalendar.DATE, -4);
		time = cal.getTimeInMillis();
		context = ctx;
	}

	public NewsDroidDB open() throws SQLException {
		try {
			helper = new NewsDroidDBHelper(context);
			db = helper.getWritableDatabase();
		} catch (RuntimeException e) {
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

	public boolean insertArticle(Long feedId, String title, URL url,
			String description, Date date) {
		ContentValues values = new ContentValues();
		long insertTime = 0;
		if (date != null) {
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(date);
			insertTime = cal.getTimeInMillis();
			values.put("date", insertTime);
		}

		try {
			Cursor c = db.query(
					ARTICLES_TABLE, // check if article is already in database
					new String[] { "article_id", "date" }, "url=\"" + url + "\"",
					null, null, null, null);
			int count = c.getCount();
			if (count >= 1) {
				c.moveToFirst();
				long articles_id = c.getLong(0);
				long oldDate = c.getLong(1);
				c.close();
				if (oldDate < insertTime)
					db.delete(ARTICLES_TABLE, "article_id=" + articles_id, null);
				else
					return true;
			} else
				c.close();
		} catch (RuntimeException e) {
			Log.e("NewsDroid", e.toString());
		}

		values.put("feed_id", feedId);
		values.put("title", title);
		values.put("url", url.toString());
		values.put("read", 0);
		values.put("description", description);
		values.put("known", 0);

		return (db.insert(ARTICLES_TABLE, null, values) > 0);
	}

	public boolean deleteAricles(Long feedId) {
		return (db.delete(ARTICLES_TABLE, "feed_id=" + feedId.toString(), null) > 0);
	}

	public ArrayList<Feed> getFeeds() {
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
	
	public int getNumUnread(Long feedId) {
		try {
			Cursor c = null;
			if(feedId >= 0) {
				c = db.query(ARTICLES_TABLE, new String[] {},
						"feed_id=" + feedId.toString() + " and read=0", null, null, null, null);
			} else {
				c = db.query(ARTICLES_TABLE, new String[] {},
						"read=0", null, null, null, null);
			}
			int numRows = c.getCount();
			c.close();
			return numRows;
		} catch (SQLException e) {
			Log.e("NewsDroid", e.toString());
		}
		return 0;
	}

	public List<Article> getArticles(Long feedId) {
		ArrayList<Article> articles = new ArrayList<Article>();
		try {
			Cursor c = null;
			if (feedId >= 0) {
				c = db.query(ARTICLES_TABLE, new String[] { "article_id",
						"feed_id", "title", "url", "description", "date", "read" },
						"feed_id=" + feedId.toString(), null, null, null, null);
			} else {
				c = db.query(ARTICLES_TABLE, new String[] { "article_id",
						"feed_id", "title", "url", "description", "date", "read" },
						"read=0", null, null, null, null);
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
				article.read = c.getInt(6) == 0 ? false : true;
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
		GregorianCalendar cal = new GregorianCalendar();
		ContentValues values = new ContentValues();
		values.put("read", cal.getTimeInMillis());
		db.update(ARTICLES_TABLE, values, "article_id=" + articleId, null);
	}

	public ArrayList<String> getDescriptionById(ArrayList<Long> otherArticlesId) {
		ArrayList<String> descriptions = new ArrayList<String>();
		String inQuery = otherArticlesId.toString();
		inQuery = inQuery.replace('[', '(');
		inQuery = inQuery.replace(']', ')');
		try {
			Cursor c = null;
			c = db.query(ARTICLES_TABLE, new String[] { "description" },
					"article_id IN" + inQuery, null, null, null, null, null);
			c.moveToFirst();
			assert(c.getCount() == otherArticlesId.size());
			for (int i = 0; i < c.getCount(); ++i) {
				descriptions.add(c.getString(0)); 
				c.moveToNext();
			}
			c.close();
		} catch (SQLException e) {
			Log.e("NewsDroid", e.toString());
		}
		return descriptions;
	}

	public void markAsKnown(Long articleId) {
		ArrayList<Long> idsOfRead = new ArrayList<Long>();
		try {
			Cursor c = null;
			c = db.query(ARTICLES_TABLE, new String[] { "article_id" }, "read>"
					+ time, null, null, null, null);
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); ++i) {
				idsOfRead.add(c.getLong(0));
				c.moveToNext();
			}
			c.close();
		} catch (SQLException e) {
			Log.e("NewsDroid", e.toString());
		}
		ContentValues values = new ContentValues();
		values.put("known", idsOfRead.toString());
		db.update(ARTICLES_TABLE, values, "article_id=" + articleId, null);
	}

	public HashSet<Long> removeOldArticles() {
		HashSet<Long> oldArticleIds = new HashSet<Long>();
		try {
			Cursor c = db.query(ARTICLES_TABLE, new String[] {"article_id"}, "read<"+time, null, null, null, null);
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); ++i) {
				oldArticleIds.add(c.getLong(0));
				c.moveToNext();
			}
			c.close();
		} catch (SQLException e) {
			Log.e("NewsDroid", e.toString());
		}
		db.delete(ARTICLES_TABLE, "read<" + time, null);
		return oldArticleIds;
	}

	public int getArticleRead(long articleId) {
		Cursor c = db.query(ARTICLES_TABLE, new String[] { "known" },
				"article_id=" + articleId, null, null, null, null);
		if (c.getCount() == 0) {
			c.close();
			return 0;
		}

		c.moveToFirst();
		String known = c.getString(0);
		c.close();
		
		if (known.equals("0")) {
			return 0;
		}
		
		return 1;
	}
}
