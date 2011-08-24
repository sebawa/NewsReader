package de.farw.newsreader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ArticlesList extends ListActivity {

	private List<Article> articles;
	private Feed feed;
	private NewsDroidDB droidDB;

	@Override
	protected void onCreate(Bundle icicle) {
		try {
			super.onCreate(icicle);
			droidDB = new NewsDroidDB(this);
			droidDB.open();
			setContentView(R.layout.articles_list);

			feed = new Feed();

			if (icicle != null) {
				feed.feedId = icicle.getLong("feed_id");
				feed.title = icicle.getString("title");
				feed.url = new URL(icicle.getString("url"));
			} else {
				Bundle extras = getIntent().getExtras();
				feed.feedId = extras.getLong("feed_id");
				feed.title = extras.getString("title");
				feed.url = new URL(extras.getString("url"));

				// droidDB.deleteAricles(feed.feedId); we don't want to delete
				// all old articles
				if (feed.feedId != -1) {
					RSSHandler rh = new RSSHandler();
					rh.updateArticles(this, feed);
				}
			}
			setTitle(feed.title);

			fillData();

		} catch (MalformedURLException e) {
			Log.e("NewsDroid", e.toString());
		}

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong("feed_id", feed.feedId);
		outState.putString("title", feed.title);
		outState.putString("url", feed.url.toString());
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Article currentArticle = articles.get(position);
		String uri = currentArticle.url.toString();
		Long articleId = currentArticle.articleId;
		String description = currentArticle.description;
		String title = currentArticle.title;
		droidDB.setRead(articleId);

//		FeedAnalyzer feedanalyzer = new FeedAnalyzer(uri);
//		try {
//			feedanalyzer.fetchHTML();
//		} catch (IOException e) {
//			Log.e("NewsDroid", e.toString());
//		}
		try {
			Intent i = new Intent(this, FeedView.class);
			i.putExtra("url", uri);
			i.putExtra("description", description);
			i.putExtra("title", title);
			startActivity(i);
		} catch (Exception e) {
			Log.e("NewsDroid", e.toString());
		}
	}

	private void fillData() {
		List<String> items = new ArrayList<String>();
		articles = droidDB.getArticles(feed.feedId);
		Collections.sort(articles, new Comparator<Article>() {
			public int compare(Article a1, Article a2) { return a2.date.compareTo(a1.date); }
		});

		for (Article article : articles) {
			items.add(article.title);
		}

		ArrayAdapter<String> notes = new ArrayAdapter<String>(this,
				R.layout.article_row, items);
		setListAdapter(notes);
	}
}
