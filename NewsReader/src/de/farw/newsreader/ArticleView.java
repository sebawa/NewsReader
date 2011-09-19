package de.farw.newsreader;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ArticleView extends Activity {
	private class FeedWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}

	private static final int ACTIVITY_READ = 0;

	private WebView mWebView;
	private String url;
	private long id;
	private String title;
	private String content;
	private boolean learned = false;
	private boolean read;
	private ArrayList<String> commonWords;
	private long feedId;
	private double bleuVal;
	private long timeDiff;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, ACTIVITY_READ, android.view.Menu.NONE, R.string.menu_read);

		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		switch (item.getItemId()) {
		case ACTIVITY_READ:
			if (read == false && learned == false) {
				Perceptron perceptron = Perceptron.getInstance(this);
				TIntDoubleHashMap pX = perceptron.generateX(bleuVal, feedId, commonWords.size(), timeDiff);
				perceptron.learnArticle(pX, 1);
				learned = true;
			}
			NewsDroidDB db = new NewsDroidDB(getApplicationContext());
			db.open();
			db.markAsKnown(id);
			db.close();
			break;
		}
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mWebView = new WebView(this);
		mWebView.setWebViewClient(new FeedWebViewClient());
		setContentView(mWebView);
		if (savedInstanceState != null) {
			url = savedInstanceState.getString("url");
			content = savedInstanceState.getString("description");
			title = savedInstanceState.getString("title");
			id = savedInstanceState.getLong("id");
			read = savedInstanceState.getBoolean("read");
			commonWords = savedInstanceState.getStringArrayList("words");
			feedId = savedInstanceState.getLong("feedId");
			bleuVal = savedInstanceState.getDouble("bleu");
			timeDiff = savedInstanceState.getLong("time");
		} else {
			Bundle b = getIntent().getExtras();
			url = b.getString("url");
			content = b.getString("description");
			title = b.getString("title");
			id = b.getLong("id");
			read = b.getBoolean("read");
			commonWords = b.getStringArrayList("words");
			feedId = b.getLong("feedId");
			bleuVal = b.getDouble("bleu");
			timeDiff = b.getLong("time");
		}
		setTitle(title);
		String toDisplay = generateHTMLContent(content, commonWords);
		try {
		mWebView.loadData(toDisplay, "text/html", "utf-8");
		} catch (Exception e) {
			Log.e("NewsDroid", e.toString());
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("url", url.toString());
		outState.putString("description", content);
		outState.putString("title", title);
		outState.putLong("id", id);
		outState.putBoolean("read", read);
		outState.putStringArrayList("words", commonWords);
		outState.putLong("feedId", feedId);
		outState.putDouble("bleu", bleuVal);
		outState.putLong("time", timeDiff);
	}

	@Override
	public void onDestroy() { 
		super.onDestroy();
		if (read == false && learned == false) {
			Perceptron perceptron = Perceptron.getInstance(this);
			TIntDoubleHashMap pX = perceptron.generateX(bleuVal, feedId, commonWords.size(), timeDiff);
			perceptron.learnArticle(pX, -1);
		}
		try {
			NewsDroidDB db = new NewsDroidDB(this);
			db.open();
			db.close();
		} catch (RuntimeException e) {
			Log.e("NewsDroid", e.toString());
		}
	}

	private String generateHTMLContent(String in, ArrayList<String> commonWords) {
		ArrayList<String> tags = new ArrayList<String>();
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		final int imageSize = (int) (metrics.widthPixels * 0.90);

		in = in.replaceAll("\n", " ");
		// this pattern should match _all_ HTML tags
		Pattern pTag = Pattern
				.compile("</?\\w+((\\s+\\w+(\\s*=\\s*(?:\".*?\"|'.*?'|[^'\">\\s]+))?)+\\s*|\\s*)/?>|&[\\p{Alnum}]*?;");
		Matcher mTag = pTag.matcher(in);
		StringBuffer fromHTML = new StringBuffer();
		while (mTag.find()) {
			tags.add(mTag.group(0));
			mTag.appendReplacement(fromHTML, ":@:");
		}
		mTag.appendTail(fromHTML);

		StringBuffer sb = new StringBuffer(fromHTML);
		String buffer = fromHTML.toString();
		for (String match : commonWords) {
			sb = new StringBuffer();
			Pattern p = Pattern.compile("(?i)\\b" + match + "[\\p{Punct}\\p{Space}]+?");
			Matcher m = p.matcher(buffer);
			while (m.find()) {
				String replacement = "<font color=\"#00FF00\">" + m.group()
						+ "</font>";
				m.appendReplacement(sb, replacement);
			}
			m.appendTail(sb);
			buffer = sb.toString();
		}

		String data = new String(sb);
		Iterator<String> iter = tags.iterator();
		while (iter.hasNext()) {
			data = data.replaceFirst(":@:", iter.next());
		}

		data = URLEncoder.encode(data).replaceAll("\\+", " ");
		String out = "<html><head><style type=\"text/css\">img {max-width:"
				+ imageSize + "px;}</style></head>";
		out += "<body>" + data + "<br><br>" + "<a href=" + url + ">"
				+ getString(R.string.read_more) + "</a></body></html>";

		return out;
	}
}
