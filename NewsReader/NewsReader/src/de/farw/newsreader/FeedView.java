package de.farw.newsreader;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class FeedView extends Activity {
	private class FeedWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	        view.loadUrl(url);
	        return true;
	    }
	}
	
	private WebView mWebView;
	private String url;
	private String content;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mWebView = new WebView(this);
	    mWebView.setWebViewClient(new FeedWebViewClient());
	    String title;
	    setContentView(mWebView);
	    if (savedInstanceState != null) {
	    	url = savedInstanceState.getString("url");
	    	content = savedInstanceState.getString("description");
	    	title = savedInstanceState.getString("title");
	    } else {
	    	Bundle b = getIntent().getExtras();
	    	url = b.getString("url");
	    	content = b.getString("description");
	    	title = b.getString("title");
	    }
	    setTitle(title);
	    content = "<html><body>" + encodeHTML(content) + "</body></html>";
//	    mWebView.loadUrl(url);
	    mWebView.loadData(content, "text/html", "utf-8");
	}
	
	private String encodeHTML(String s) {
	    StringBuffer out = new StringBuffer();
	    for (int i = 0; i < s.length(); i++) {
	        char c = s.charAt(i);
	        if (c > 127 || c == '"') { //  || c == '<' || c == '>') {
	           out.append("&#" + (int)c + ";");
	        } else {
	            out.append(c);
	        }
	    }
	    return out.toString();
	}
}
