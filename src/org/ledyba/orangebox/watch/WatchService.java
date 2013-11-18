package org.ledyba.orangebox.watch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class WatchService extends Service {
	private final static String TAG = "WatchService";

	public WatchService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "bind from: " + intent.toString());
		return binder;
	}

	protected final IBinder binder = new Binder() {
		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			return super.onTransact(code, data, reply, flags);
		}
	};
	
	private void startSensors() {
		final SensorManager manager = (SensorManager)getSystemService(SENSOR_SERVICE);
		Sensor grav = null;
		Sensor acc = null;
		{
			List<Sensor> accs = manager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
			List<Sensor> gravs =  manager.getSensorList(Sensor.TYPE_GRAVITY);
			Log.d(TAG, accs.size() +" Acceleration sensors found.");
			Log.d(TAG, gravs.size()+" Gravity sensors found.");
			for( Sensor sensor : accs ) {
				Log.d(TAG, sensor.getName()+" -> "+sensor.getPower()+"mA");
				if( acc == null ) {
					acc = sensor;
				}else if( acc.getPower() > sensor.getPower() ) {
					acc = sensor;
				}
			}
			for( Sensor sensor : gravs ) {
				Log.d(TAG, sensor.getName()+" -> "+sensor.getPower()+"mA");
				if( grav == null ) {
					grav = sensor;
				}else if( acc.getPower() > sensor.getPower() ) {
					grav = sensor;
				}
			}
		}
		if(grav != null){
			SensorWatcher watcher = new SensorWatcher(this, grav);
			watchers.add(watcher);
		}
		if(acc != null){
			SensorWatcher watcher = new SensorWatcher(this, acc);
			watchers.add(watcher);
		}
		int registered = 0;
		for( SensorWatcher watcher : watchers ) {
			try {
				watcher.start();
				++registered;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		Log.d(TAG, registered + " of " + watchers.size() + " sensors registered");
	}
	
	private void stopSensors(){
		for( SensorWatcher watcher : watchers ) {
			watcher.stop();
		}
		watchers.clear();
	}
	
	private LocationWatcher locationWatcher;
	private void startLocation(){
		locationWatcher = new LocationWatcher(this);
		try {
			locationWatcher.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	private void stopLocation(){
		locationWatcher.stop();
		locationWatcher = null;
	}

	List<SensorWatcher> watchers = new ArrayList<SensorWatcher>();
	@Override
	public void onCreate() {
		super.onCreate();
		this.startSensors();
		this.startLocation();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.stopSensors();
		this.stopLocation();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String action = intent == null ? "(null)" : intent.getAction();
		Log.d(TAG, "onStartCommand: "+action);
		if(STOP_INTENT.equals(action)){
			stopResident();
		}
		return super.onStartCommand(intent, flags, startId);
	}

	public WatchService startResident(Context context) {
		Log.d(TAG, "Service has been started from: " + context.toString());
		Intent intent = new Intent(context, this.getClass());
		intent.putExtra("type", "start");
		context.startService(intent);

		return this;
	}

	public void stopResident() {
		// サービス自体を停止
		stopSelf();
	}
	private final static String STOP_INTENT = "org.ledyba.meso.StopService";
	public static void stopResidentIfActive(Context context){
		Intent it = new Intent(context, WatchService.class);
		it.setAction(STOP_INTENT);
		context.startService(it);
	}
}
