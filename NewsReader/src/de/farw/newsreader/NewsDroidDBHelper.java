package de.farw.newsreader;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class NewsDroidDBHelper extends SQLiteOpenHelper {
	private static final String CREATE_TABLE_FEEDS = "create table feeds (feed_id integer primary key autoincrement, "
		+ "title text not null, url text not null);";

	private static final String CREATE_TABLE_ARTICLES = "create table articles (article_id integer primary key autoincrement, "
		+ "feed_id int not null, title text not null, url text not null, read integer not null, description text, date integer, known string, unique(url) on conflict ignore);";
	
	private static final String CREATE_TABLE_NGRAMS = "create table ngrams (ngram_id integer primary key autoincrement, "
		+ "content text not null, appears_in integer not null, length integer not null);";

	private static final String DATABASE_NAME = "newdroid";
	private static final int DATABASE_VERSION = 10;

	public NewsDroidDBHelper(Context ctx) {
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(CREATE_TABLE_FEEDS);
		database.execSQL(CREATE_TABLE_ARTICLES);
		database.execSQL(CREATE_TABLE_NGRAMS);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(NewsDroidDB.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		database.execSQL("DROP TABLE feeds");
		database.execSQL("DROP TABLE articles");
		database.execSQL("DROP TABLE ngrams");
		onCreate(database);
	}
}
