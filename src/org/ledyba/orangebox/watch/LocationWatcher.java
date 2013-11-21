package org.ledyba.orangebox.watch;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;

class LocationWatcher extends AbstractWatcher implements LocationListener {
	private final LocationManager manager;
	private static final int TIME_SPAN=60*60*1000; //一時間
	private static final int TIME_SEARCH=5*60*1000; //5分
	private final Handler handler = new Handler();
	static private final String TAG = "LocationWatcher";

	public LocationWatcher(Context ctx) {
		manager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public void start() throws IOException {
		super.start();
		Log.d(TAG, "GPS require "+manager.getProvider(LocationManager.GPS_PROVIDER).getPowerRequirement()+" mA");
		Log.d(TAG, "Network require "+manager.getProvider(LocationManager.NETWORK_PROVIDER).getPowerRequirement()+" mA");
		startUpdate();
	}

	@Override
	public void stop() {
		stopUpdate();
		killTimer();
		super.stop();
	}

	@Override
	protected String getFilename() {
		final String base = Environment.getExternalStorageDirectory().getPath();
		final String fname = String.format(Locale.JAPANESE, "%s_%d.txt", "GPS", new Date().getTime());
		return base + File.separator + fname;
	}
	private final Runnable stopTask = new Runnable() {
		@Override
		public void run() {
			Log.d(TAG, "Location update stop!");
			stopUpdate();
		}
	};
	private final Runnable startTask = new Runnable() {
		@Override
		public void run() {
			Log.d(TAG, "Location update start!");
			if( timer != null ) {
				stopUpdate();
				manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, LocationWatcher.this);
				manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, LocationWatcher.this);
			}
		}
	};
	private class LoopTask extends TimerTask {
		@Override
		public void run() {
			Log.d(TAG, "Loop task invoked.");
			handler.postDelayed(stopTask, TIME_SEARCH);
			handler.post(startTask);
		}
		
	}
	private Timer timer;
	public void startUpdate() {
		stopUpdate();
		killTimer();
		this.timer = new Timer(true);
		this.timer.scheduleAtFixedRate(new LoopTask(), 0, TIME_SPAN);
	}
	private void killTimer(){
		if( timer != null ) {
			timer.cancel();
		}
		timer = null;
		handler.removeCallbacks(startTask);
		handler.removeCallbacks(stopTask);
	}
	private void stopUpdate(){
		try {
			manager.removeUpdates(this);
		} catch (Exception e){
			Log.d(TAG, "Error while remove listener: ", e);
		}
	}

	private void recordLocation(final Location location){
		if( out == null || location == null) {
			return;
		}
		Log.d(TAG, "--------------------");
		Log.d(TAG, "Provider : " +location.getProvider());
		Log.d(TAG, "Latitude : " + String.valueOf(location.getLatitude()));
		Log.d(TAG, "Longitude: " + String.valueOf(location.getLongitude()));
		Log.d(TAG, "Accuracy : " + String.valueOf(location.getAccuracy()));
		Log.d(TAG, "Time     : " + String.valueOf(location.getTime()));
		
		try {
			out.write(
					String.format(
							Locale.JAPANESE,
							"%d,%f,%f,%f,%s\n",
							location.getTime(),
							location.getLatitude(),
							location.getLongitude(),
							location.getAccuracy(),
							location.getProvider()));
			out.flush();
		} catch (IOException e) {
			Log.e(TAG, "file write error: ", e);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		recordLocation(location);
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(TAG, provider + " disabled.");
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(TAG, provider + " enabled");
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d(TAG, provider + " status changed.");
	}
}
