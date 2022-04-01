package org.ledyba.orangebox;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.webkit.WebView;

public class LicenseActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_license);
		WebView wv = (WebView) findViewById(R.id.web_view);
		wv.loadUrl("file:///android_asset/agpl.html");
		wv.setVerticalScrollbarOverlay(true);
		wv.setHorizontalScrollBarEnabled( false );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

}
