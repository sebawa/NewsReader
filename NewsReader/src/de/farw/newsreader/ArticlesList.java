package de.farw.newsreader;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ArticlesList extends ListActivity implements IList {

	private static final int ACTIVITY_REFRESH = 1;
	private static final int ACTIVITY_READ = 2;
	private List<Article> articles;
	private Feed feed;
	private NewsDroidDB droidDB;
	private ArticleAdapter adapter;
	private ArrayList<Article> items;
	private ProgressDialog dialog;

	@Override
	protected void onCreate(Bundle icicle) {
		try {
			super.onCreate(icicle);
			droidDB = new NewsDroidDB(this);
			droidDB.open();
			setContentView(R.layout.articles_list);
			feed = new Feed();
			items = new ArrayList<Article>();
			adapter = new ArticleAdapter(this, R.layout.article_row, items);
			setListAdapter(adapter);

			if (icicle != null) {
				feed.feedId = icicle.getLong("feed_id");
				feed.title = icicle.getString("title");
				feed.url = new URL(icicle.getString("url"));
			} else {
				Bundle extras = getIntent().getExtras();
				feed.feedId = extras.getLong("feed_id");
				feed.title = extras.getString("title");
				feed.url = new URL(extras.getString("url"));

				if (feed.feedId != -1) {
					dialog = ProgressDialog.show(this, "",
							getString(R.string.loading_dialog), true, false);
					RSSHandler updateThread = new RSSHandler(feed, this, dialog, this.getApplicationContext());
					updateThread.start();
//					updateThread.join();
				}
			}
			setTitle(feed.title);

			fillData();
		} catch (MalformedURLException e) {
			Log.e("NewsDroid", e.toString());
//		} catch (InterruptedException e) {
//			Log.e("NewsDroid", e.toString());
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
		TextView titleText = (TextView) v.findViewById(R.id.title);
		TextView timeText = (TextView) v.findViewById(R.id.time);
		titleText.setTextColor(Color.DKGRAY);
		timeText.setTextColor(Color.DKGRAY);
		if (currentArticle.bleuData == null)
			currentArticle.bleuData = new BleuData();
		
		try {
			Intent i = new Intent(this, ArticleView.class);
			i.putExtra("url", uri);
			i.putExtra("description", description);
			i.putExtra("title", title);
			i.putExtra("id", articleId);
			i.putExtra("read", currentArticle.read);
			i.putStringArrayListExtra("words", new ArrayList<String>(currentArticle.bleuData.matchingNGrams));
			i.putExtra("feedId", currentArticle.feedId);
			i.putExtra("bleu", currentArticle.bleuData.bleuValue);
			i.putExtra("time", currentArticle.bleuData.timeDiff);
			startActivity(i);
		} catch (Exception e) {
			Log.e("NewsDroid", e.toString());
		}
		currentArticle.read = true;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (dialog != null && dialog.isShowing())
			dialog.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, ACTIVITY_REFRESH, android.view.Menu.NONE,
				R.string.menu_refresh_articles);
		menu.add(0, ACTIVITY_READ, android.view.Menu.NONE, R.string.all_read);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		switch (item.getItemId()) {
		case ACTIVITY_REFRESH:
			if (feed.feedId != -1) {
				dialog = ProgressDialog.show(this, "", getString(R.string.loading_dialog));
				RSSHandler updateThread = new RSSHandler(feed, this, dialog, this.getApplicationContext());
				updateThread.start();
			} else {
				ArrayList<Feed> feeds = droidDB.getFeeds();
				dialog = ProgressDialog.show(this, "", getString(R.string.loading_dialog));
				RSSHandler updateThread = new RSSHandler(feeds, this, dialog, this.getApplicationContext());
				updateThread.start();
			}
			break;
		case ACTIVITY_READ:
			droidDB.setAllRead(feed.feedId);
			fillData();
		}

		return true;
	}

	public void fillData() {
		articles = droidDB.getArticles(feed.feedId);
		Collections.sort(articles, new Comparator<Article>() {
			public int compare(Article a1, Article a2) {
				return a2.date.compareTo(a1.date);
			}
		});

		items.clear();
		for (Article article : articles) {
			items.add(article);
		}
		adapter.notifyDataSetChanged();
	}

	private class ArticleAdapter extends ArrayAdapter<Article> {

		private ArrayList<Article> items;
		private SimpleDateFormat dayformat;
		private SimpleDateFormat timeformat;
		private BleuAlgorithm ba;
		private Perceptron perceptron;

		public ArticleAdapter(Context context, int textViewResourceId,
				ArrayList<Article> items) {
			super(context, textViewResourceId, items);
			this.items = items;
			dayformat = new SimpleDateFormat("dd.MMM");
			timeformat = new SimpleDateFormat("HH:mm");
			ba = BleuAlgorithm.getInstance(getApplicationContext());
			perceptron = Perceptron.getInstance(getApplicationContext());
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.article_row, null);
			}
			Article a = items.get(position);
			if (a != null) {
				TextView title = (TextView) v.findViewById(R.id.title);
				TextView time = (TextView) v.findViewById(R.id.time);
				title.setText(a.title);
				time.setText(generateTimeString(a.date));
				if (a.read) {
					title.setTextColor(Color.DKGRAY);
					time.setTextColor(Color.DKGRAY);
				} else {
					BleuData bd = ba.scanArticle(a.description, a.articleId);
					TIntDoubleHashMap x = perceptron.generateX(bd.bleuValue, a.feedId, bd.matchingNGrams.size(), bd.timeDiff);
					int pred = perceptron.getAssumption(x);
					a.bleuData = bd;
					if (pred == -1) {
						title.setTextColor(Color.YELLOW);
						time.setTextColor(Color.YELLOW);
					} else {
						title.setTextColor(Color.LTGRAY);
						time.setTextColor(Color.LTGRAY);
					}
				}
			}
			return v;
		}

		private String generateTimeString(Date date) {
			String formattedDate;
			Date now = new Date(); // just compare the day, not the time
			now.setHours(0);
			now.setMinutes(0);
			now.setSeconds(0);
			long ms = now.getTime();
			now.setTime(ms - ms % 1000);

			Date compDate = (Date) date.clone();
			compDate.setHours(0);
			compDate.setMinutes(0);
			compDate.setSeconds(0);

			if (compDate.before(now)) {
				formattedDate = dayformat.format(date);
			} else {
				formattedDate = timeformat.format(date);
			}

			return formattedDate;
		}
	}
}
