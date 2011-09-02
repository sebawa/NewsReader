package de.farw.newsreader;


import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class URLEditor extends Activity  {

	EditText mText;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.url_editor);

        // Set up click handlers for the text field and button
        mText = (EditText) this.findViewById(R.id.url);
        
        if (icicle != null)
        	mText.setText(icicle.getString("url"));
        
        Button ok = (Button) findViewById(R.id.ok);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
            	okClicked();
            }          
        });
        
        Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
            	finish();
            }          
        });
        
    }

    protected void okClicked() {
    	try {
    		RSSHandler rh = new RSSHandler(this.getApplicationContext());
    		rh.createFeed(new URL(mText.getText().toString()));
//    		rh.createFeed(this, new URL(mText.getText().toString()));
    		finish();
    	} catch (MalformedURLException e) {
//    		showAlert("Invalid URL", "The URL you have entered is invalid.", "Ok", false);
    		  AlertDialog about = new AlertDialog.Builder(URLEditor.this)
              	//.setIcon(R.drawable.icon)
              	.setTitle(R.string.invalid_url)
              	.setPositiveButton(R.string.ok, null)
              	.setMessage(R.string.invalid_url_content)
              	.create();
    		  about.show(); 
    	}
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("url", mText.getText().toString());		
	}
}
