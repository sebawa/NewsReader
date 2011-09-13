package de.farw.newsreader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

public class RSSHandler extends Thread {
	private ArrayList<Feed> feeds;
	private ArrayList<String> urls;
	private Feed feed;
	private Context ctx;
	private RSSLoader loader;
	private ProgressDialog dialog;
	private static int currentAction;
	private static AtomicBoolean running = new AtomicBoolean(false);
	public static final int ACTION_UPDATE_SINGLE_FEED = 0;
	public static final int ACTION_UPDATE_ALL_FEEDS = 1;
	public static final int ACTION_INSERT_SINGE_FEED = 2;
	public static final int ACTION_IMPORT_FEEDS = 3;

	public RSSHandler(Feed f, Context c, ProgressDialog d) {
		currentAction = ACTION_UPDATE_SINGLE_FEED;
		feed = f;
		ctx = c;
		dialog = d;
		loader = new RSSLoader();
	}

	public RSSHandler(ArrayList<Feed> f, Context c, ProgressDialog d) {
		currentAction = ACTION_UPDATE_ALL_FEEDS;
		feeds = f;
		ctx = c;
		dialog = d;
		loader = new RSSLoader();
	}

	public RSSHandler(ArrayList<String> u, ProgressDialog d, Context c) {
		currentAction = ACTION_IMPORT_FEEDS;
		ctx = c;
		urls = u;
		dialog = d;
		loader = new RSSLoader();
	}

	public RSSHandler(Context c) {
		currentAction = ACTION_INSERT_SINGE_FEED;
		ctx = c;
		loader = new RSSLoader();
	}

	@Override
	public void run() {
		if (!running.compareAndSet(false, true)) {
			handler.sendEmptyMessage(2);
			return;
		}

		if (hasInternet() == false) {
			handler.sendEmptyMessage(1);
			return;
		}

		switch (currentAction) {
		case ACTION_IMPORT_FEEDS:
			importFeed();
			break;
		case ACTION_UPDATE_ALL_FEEDS:
			for (Feed f : feeds)
				loader.updateArticles(ctx, f);
			break;
		case ACTION_UPDATE_SINGLE_FEED:
			loader.updateArticles(ctx, feed);
			break;
		}
		handler.sendEmptyMessage(0);
	}

	public void createFeed(URL url) {
		loader.createFeed(ctx, url);
	}

	private void importFeed() {
		for (String url : urls) {
			if (hasInternet() == false) {
				handler.sendEmptyMessage(1);
				return;
			}
			try {
				loader.createFeed(ctx, new URL(url));
			} catch (MalformedURLException e) {
				Log.e("NewsDroid", e.toString());
			}
		}
		handler.sendEmptyMessage(0);
	}

	private boolean hasInternet() {
		NetworkInfo info = ((ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		if (info == null || !info.isConnected()) {
			return false;
		}
		return true;
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			dialog.dismiss();
			if (msg.what == 1)
				Toast.makeText(ctx, ctx.getString(R.string.no_internet),
						Toast.LENGTH_SHORT).show();
			if (msg.what == 2)
				Toast.makeText(ctx, ctx.getString(R.string.already_updating),
						Toast.LENGTH_SHORT).show();

			running.set(false);
		}
	};

	private class RSSLoader extends DefaultHandler {

		// Used to define what elements we are currently in
		private boolean inItem = false;
		private boolean inTitle = false;
		private boolean inLink = false;
		private boolean inDescription = false;
		private boolean inDate = false;
		private boolean inOldDate = false;

		// Feed and Article objects to use for temporary storage
		private Article currentArticle = new Article();
		private Feed currentFeed = new Feed();

		// Number of articles added so far
		private int articlesAdded = 0;

		// Number of articles to download
		private static final int ARTICLES_LIMIT = 15; // TODO make it variable

		// The possible values for targetFlag
		private static final int TARGET_FEED = 0;
		private static final int TARGET_ARTICLES = 1;

		// A flag to know if looking for Articles or Feed name
		private int targetFlag;

		private NewsDroidDB droidDB = null;
		private boolean noDate = false;
		private boolean brokenURL = false;

		public RSSLoader() {
			currentFeed.title = "";
			currentArticle.title = "";
			currentArticle.description = "";
		}

		public void startElement(String uri, String name, String qName,
				Attributes atts) {
			if (name.trim().equals("title"))
				inTitle = true;
			else if (name.trim().equals("item"))
				inItem = true;
			else if (name.trim().equals("link"))
				inLink = true;
			else if (name.trim().equals("description"))
				inDescription = true;
			else if (name.trim().equals("pubDate"))
				inDate = true;
			else if (name.trim().equals("date"))
				inOldDate = true;
		}

		public void endElement(String uri, String name, String qName)
				throws SAXException {
			if (name.trim().equals("title"))
				inTitle = false;
			else if (name.trim().equals("item"))
				inItem = false;
			else if (name.trim().equals("link"))
				inLink = false;
			else if (name.trim().equals("description"))
				inDescription = false;
			else if (name.trim().equals("pubDate"))
				inDate = false;
			else if (name.trim().equals("date"))
				inOldDate = false;

			// Check if looking for feed, and if feed is complete
			if (targetFlag == TARGET_FEED && currentFeed.url != null
					&& currentFeed.title != "") {

				// We know everything we need to know, so insert feed and exit
				droidDB.insertFeed(currentFeed.title, currentFeed.url);
				currentFeed.title = "";
				throw new SAXException();
			}

			// Check if looking for article, and if article is complete
			if (targetFlag == TARGET_ARTICLES && currentArticle.url != null
					&& currentArticle.title != ""
					&& currentArticle.description != ""
					&& (noDate || currentArticle.date != null)) {
				if (!brokenURL) {
					currentArticle.title = Html.fromHtml(currentArticle.title)
							.toString();
					droidDB.insertArticle(currentFeed.feedId,
							currentArticle.title, currentArticle.url,
							currentArticle.description, currentArticle.date);
				}
				currentArticle.title = "";
				currentArticle.url = null;
				currentArticle.description = "";
				currentArticle.date = null;
				noDate = false;
				brokenURL = false;

				// Lets check if we've hit our limit on number of articles
				articlesAdded++;
				if (articlesAdded >= ARTICLES_LIMIT) {
					throw new SAXException();
				}
			}

		}

		public void characters(char ch[], int start, int length) {

			String chars = (new String(ch).substring(start, start + length));

			try {
				// If not in item, then title/link refers to feed
				if (!inItem) {
					if (inTitle)
						currentFeed.title += chars;
				} else {
					if (inLink)
						currentArticle.url = new URL(chars);
					if (inTitle)
						currentArticle.title += chars;
					if (inDescription)
						currentArticle.description += chars;
					if (inDate) { // date formated like: Tue, 23 Aug 2011
									// 12:56:35 +0200
						try {
							currentArticle.date = new SimpleDateFormat(
									"EEE, dd MMM yyyy HH:mm:ss Z").parse(chars);
						} catch (ParseException e) {
							try {
								currentArticle.date = new SimpleDateFormat(
										"EEE, dd MMM yyyy HH:mm:ss z")
										.parse(chars);
							} catch (ParseException f) {
								currentArticle.date = null;
								noDate = true;
							}
						}
					}
					if (inOldDate) { // date formated like:
										// 2011-08-31T18:12:03+00:00
						try {
							final int splitPoint = chars.length() - 3;
							String correctedDate = chars.substring(0,
									splitPoint);
							correctedDate += chars.substring(splitPoint + 1);
							currentArticle.date = new SimpleDateFormat(
									"yyyy-MM-dd'T'hh:mm:ssZ")
									.parse(correctedDate);
						} catch (ParseException e) {
							currentArticle.date = null;
							noDate = true;
						}
					}
				}
			} catch (MalformedURLException e) {
				brokenURL = true;
				Log.e("NewsDroid", e.toString());
			}

		}

		public void createFeed(Context ctx, URL url) {
			try {
				targetFlag = TARGET_FEED;
				droidDB = new NewsDroidDB(ctx);
				droidDB.open();
				currentFeed.url = url;

				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				xr.setContentHandler(this);
				xr.parse(new InputSource(url.openStream()));
				// droidDB.close();

			} catch (IOException e) {
				Log.e("NewsDroid", e.toString());
			} catch (SAXException e) {
				Log.e("NewsDroid", e.toString());
			} catch (ParserConfigurationException e) {
				Log.e("NewsDroid", e.toString());
			}
		}

		public void updateArticles(Context ctx, Feed feed) {
			try {
				targetFlag = TARGET_ARTICLES;
				droidDB = new NewsDroidDB(ctx);
				droidDB.open();
				currentFeed = feed;
				currentFeed.title = "";
				currentArticle.title = "";
				currentArticle.description = "";
				articlesAdded = 0;
				inItem = false;
				inTitle = false;
				inLink = false;
				inDescription = false;
				inDate = false;
				inOldDate = false;
				articlesAdded = 0;

				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				xr.setContentHandler(this);
				xr.parse(new InputSource(currentFeed.url.openStream()));
				// droidDB.close();

			} catch (IOException e) {
				Log.e("NewsDroid", e.toString());
			} catch (SAXException e) {
				Log.e("NewsDroid", e.toString());
			} catch (ParserConfigurationException e) {
				Log.e("NewsDroid", e.toString());
			} catch (RuntimeException e) {
				Log.e("NewsDroid", e.toString());
			}
		}
	}
}
