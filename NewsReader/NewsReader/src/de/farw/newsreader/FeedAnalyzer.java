package de.farw.newsreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

public class FeedAnalyzer {
	private String mHTMLString;
	private String feedUrl;
	
	public FeedAnalyzer(String url) {
		feedUrl = url;
	}
	
	 public void fetchHTML() throws ClientProtocolException, IOException {
	     HttpClient httpClient = new DefaultHttpClient();
	     HttpContext localContext = new BasicHttpContext();
	     HttpGet httpGet = new HttpGet(feedUrl);
	     HttpResponse response = httpClient.execute(httpGet, localContext);
	     mHTMLString = "";

	     BufferedReader reader = new BufferedReader(new InputStreamReader(
	           response.getEntity().getContent()));

	     String line = null;
	     while ((line = reader.readLine()) != null) {
	       mHTMLString += line + "\n";
	     }
	     return;
	 }
}
