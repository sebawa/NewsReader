package de.farw.newsreader;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class NewsDroidDBHelper extends SQLiteOpenHelper {
	private static final String CREATE_TABLE_FEEDS = "create table feeds (feed_id integer primary key autoincrement, "
		+ "title text not null, url text not null);";

	private static final String CREATE_TABLE_ARTICLES = "create table articles (article_id integer primary key autoincrement, "
		+ "feed_id int not null, title text not null, url text not null, read integer not null, description text, date integer, known string, unique(url) on conflict replace);";

	private static final String DATABASE_NAME = "newdroid";
	private static final int DATABASE_VERSION = 8;

	public NewsDroidDBHelper(Context ctx) {
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(CREATE_TABLE_FEEDS);
		database.execSQL(CREATE_TABLE_ARTICLES);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(NewsDroidDB.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		database.execSQL("DROP TABLE feeds");
		database.execSQL("DROP TABLE articles");
		onCreate(database);
	}
}
