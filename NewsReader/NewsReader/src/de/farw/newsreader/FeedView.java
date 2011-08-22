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
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mWebView = new WebView(this);
	    mWebView.setWebViewClient(new FeedWebViewClient());
	    setContentView(mWebView);
	    if (savedInstanceState != null) {
	    	url = savedInstanceState.getString("url");
	    } else {
	    	Bundle b = getIntent().getExtras();
	    	url = b.getString("url");
	    }
	    mWebView.loadUrl(url);
	}
}
