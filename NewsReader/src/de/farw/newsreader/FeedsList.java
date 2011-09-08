package de.farw.newsreader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FeedsList extends ListActivity {

	private static final int ACTIVITY_DELETE = 1;
	private static final int ACTIVITY_INSERT = 2;
	private static final int ACTIVITY_VIEW = 3;
	private static final int ACTIVITY_UPDATE = 4;
	private static final int ACTIVITY_IMPORT = 5;

	private NewsDroidDB droidDB;
	private ArrayList<Feed> feeds;
	private ProgressDialog dialog;

	@Override
	protected void onCreate(Bundle icicle) {
		try {
			super.onCreate(icicle);
			setContentView(R.layout.feeds_list);
			droidDB = new NewsDroidDB(this);
			droidDB.open();
			fillData();
		} catch (Throwable e) {
			Log.e("NewsDroid", e.toString());
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (droidDB != null) {
			droidDB.close();
		}
		BleuAlgorithm.saveBleuData(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, ACTIVITY_INSERT, android.view.Menu.NONE,
				R.string.menu_insert);
		menu.add(0, ACTIVITY_DELETE, android.view.Menu.NONE,
				R.string.menu_delete);
		menu.add(0, ACTIVITY_UPDATE, android.view.Menu.NONE,
				R.string.menu_refresh_articles);
		menu.add(1, ACTIVITY_IMPORT, android.view.Menu.NONE,
				R.string.menu_import);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		switch (item.getItemId()) {
		case ACTIVITY_INSERT:
			createFeed();
			break;
		case ACTIVITY_DELETE:
			droidDB.deleteFeed(feeds.get((int) getSelectedItemId() - 1).feedId);
			fillData();
			break;
		case ACTIVITY_UPDATE:
			feeds = droidDB.getFeeds();
			dialog = ProgressDialog.show(this, "", getString(R.string.loading_dialog));
			RSSHandler updateThread = new RSSHandler(feeds, this, dialog);
			updateThread.start();
			break;
		case ACTIVITY_IMPORT:
			final OPMLHandler handler = new OPMLHandler();
			final Context context = this.getApplicationContext();
			try {
				SAXParserFactory factory = SAXParserFactory.newInstance();
				SAXParser saxParser = factory.newSAXParser();
				File dir = Environment.getExternalStorageDirectory();
				File file = new File(dir, "download/google-reader-subscriptions.xml");
				saxParser.parse(file, handler);
			} catch(Exception e) {
				Log.e("NewsReader", e.toString());
			}
			
			for(String feed: handler.feeds) {
				RSSHandler rh = new RSSHandler(context);
				try {
					rh.createFeed(new URL(feed));
				} catch (MalformedURLException e) {
					Log.e("NewsReader", e.toString());
				}
			}
				
		}

		return true;
	}

	private void createFeed() {
		Intent i = new Intent(this, URLEditor.class);
		startActivityForResult(i, ACTIVITY_INSERT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data); // , extras);
		fillData();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, ArticlesList.class);
		if (position == 0) {
			i.putExtra("feed_id", -1l);
			i.putExtra("title", "Unread News");
			i.putExtra("url", "http://www.foobar.com");
		} else {
			position -= 1;
			i.putExtra("feed_id", feeds.get(position).feedId);
			i.putExtra("title", feeds.get(position).title);
			i.putExtra("url", feeds.get(position).url.toString());
		}
		startActivityForResult(i, ACTIVITY_VIEW);
	}

	private void fillData() {
		List<String> items = new ArrayList<String>();

		feeds = droidDB.getFeeds();
		int numUnread = droidDB.getNumUnread(-1L);
		items.add(getString(R.string.unread) + " (" + numUnread + ")");
		for (Feed feed : feeds) {
			items.add(feed.title);
		}

		ArrayAdapter<String> notes = new ArrayAdapter<String>(this,
				R.layout.feeds_row, items);
		setListAdapter(notes);
	}

}
