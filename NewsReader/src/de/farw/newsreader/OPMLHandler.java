package de.farw.newsreader;

import java.net.URL;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class OPMLHandler extends DefaultHandler {
	public ArrayList<String> feeds;
	public OPMLHandler() {
		feeds = new ArrayList<String>();
	}
	public void startElement(String uri, String localName,String qName, 
            Attributes attributes) throws SAXException {
		if(qName.equals("outline")) {
			String feedUrl = attributes.getValue("xmlUrl");
			feeds.add(feedUrl);
			
		}
	}
}
