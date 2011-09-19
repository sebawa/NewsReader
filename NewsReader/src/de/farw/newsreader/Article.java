package de.farw.newsreader;

import java.net.URL;
import java.util.Date;

class Article extends Object {
	public long articleId;
	public long feedId;
	public String title;
	public URL url;
	public String description;
	public Date date;
	public boolean read;
	BleuData bleuData;
}

