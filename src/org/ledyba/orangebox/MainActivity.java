package org.ledyba.orangebox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.ledyba.orangebox.donation.DonationActivity;
import org.ledyba.orangebox.watch.WatchService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class MainActivity extends Activity {
	private final String TAG="MainActivity";
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.JAPAN);

	private ConfigMaster master_;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		master_ = new ConfigMaster(this);
		if(master_.isNotificationEnabled()){
			WatchService.startResident(this);
		}
		setStatus();
		try {
			final PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
			
			String version = info.versionName;
			if( Build.VERSION.SDK_INT >= 9) {
				version += "(" + sdf.format(new Date(info.lastUpdateTime)) + ")";
			}

			((TextView)findViewById(R.id.appversion)).setText(version);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Cannot load package info.", e);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void setStatus(){
		((CheckBox)findViewById(R.id.notification_button)).setChecked(master_.isNotificationEnabled());
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
	
	public void onNotificationChanged(final View v){
		final CheckBox box = (CheckBox) v;
		if(!box.isChecked()){
			master_.setNotificationEnabled(false);
			WatchService.stopResidentIfActive(this);
		}else{
			master_.setNotificationEnabled(true);
			WatchService.startResident(this);
		}
	}
	
	public void onUrlClick(final View v){
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(((Button)v).getText().toString())));
	}
	
	public void onMakeDonationButtonClick(final View v) {
		final Intent it = new Intent(this, DonationActivity.class);
		startActivity(it);
	}
	public void onViewSourceClick(final View v){
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ledyba/OrangeBox/")));
	}
	public void onLicenseButtonClick(final View v) {
		final Intent it = new Intent(this, LicenseActivity.class);
		startActivity(it);
	}
}
