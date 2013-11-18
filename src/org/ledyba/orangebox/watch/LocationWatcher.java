package org.ledyba.orangebox.watch;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import android.content.Context;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

class LocationWatcher extends AbstractWatcher implements LocationListener {
	private LocationManager manager;
	private boolean alive;
	Handler handler = new Handler();
	static private final String TAG = "LocationWatcher";

	public LocationWatcher(Context ctx) {
		manager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public void start() throws IOException {
		super.start();
		Log.d(TAG, "GPS require "+manager.getProvider(LocationManager.GPS_PROVIDER).getPowerRequirement()+" mA");
		Log.d(TAG, "Network require "+manager.getProvider(LocationManager.NETWORK_PROVIDER).getPowerRequirement()+" mA");
		alive = true;
		requestUpdate();
	}

	@Override
	public void stop() {
		manager.removeUpdates(this);
		alive = false;
		super.stop();
	}

	@Override
	protected String getFilename() {
		final String base = Environment.getExternalStorageDirectory().getPath();
		final String fname = String.format(Locale.JAPANESE, "%s_%d.txt", "GPS", new Date().getTime());
		return base + File.separator + fname;
	}
	
	public void requestUpdate() {
		Iterator<GpsSatellite> it = manager.getGpsStatus(null).getSatellites().iterator();
		boolean hasGps = false;
		while( it.hasNext() ){
			GpsSatellite sat = it.next();
			Log.d(TAG, "Sat: "+sat.toString());
			hasGps = true;
		}
		if(hasGps){
			Log.d(TAG, "GPS registering...");
			manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60*1000, 100, this);
		}else{
			Log.d(TAG, "GPS not registerd");
		}
		Log.d(TAG, "Network registering...");
		manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60*1000, 100, this);
		
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(alive) {
					Log.d(TAG, "refreshing!");
					manager.removeUpdates(LocationWatcher.this);
					requestUpdate();
				}else{
					handler.removeCallbacks(this);
				}
			}
		}, 10*60*1000);
	}
	
	public void reloadUpdate() {
		
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
