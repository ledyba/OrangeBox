package org.ledyba.orangebox.watch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ledyba.orangebox.MainActivity;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
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

	final Handler handler = new Handler();
	final Runnable toForeground = new Runnable() {
		@Override
		public void run() {
			final Intent notificationIntent = new Intent(WatchService.this, MainActivity.class);
			final NotificationCompat.Builder builder =
			new NotificationCompat.Builder(WatchService.this)
			.setContentIntent( PendingIntent.getActivity(WatchService.this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT) )
			.setAutoCancel(false)
			.setSmallIcon( org.ledyba.orangebox.R.drawable.ic_launcher )
			.setDefaults(0)
			.setPriority(NotificationCompat.PRIORITY_MAX)
			.setOngoing(true)
			.setWhen(System.currentTimeMillis())
			.setContentTitle( "Now sensoring..." )
			.setContentText( watchers.size()+" sensors is working." );
			final Notification notification = builder.build();
			startForeground(1, notification);
			handler.postDelayed(toForeground, 1000*60*10);
		}
	};

	NotificationManager manager;
	final List<SensorWatcher> watchers = new ArrayList<SensorWatcher>();
	@Override
	public void onCreate() {
		super.onCreate();
		this.startSensors();
		this.startLocation();
		manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		handler.post(toForeground);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.stopSensors();
		this.stopLocation();
		if(stopByUser){
			Log.d(TAG, "destroyed by user.");
		}else{
			Log.e(TAG, "Oh... why destroyed?");
			startResident(this);
		}
	}
	
	private boolean stopByUser = false;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String action = intent == null ? "(null)" : intent.getAction();
		Log.d(TAG, "onStartCommand: "+action);
		if(STOP_INTENT.equals(action)){
			this.stopForeground(true);
			stopByUser = true;
			this.stopSelf();
		}
		return super.onStartCommand(intent, flags, startId);
	}

	public static void startResident(Context context) {
		Log.d(TAG, "Service has been started from: " + context.toString());
		Intent intent = new Intent(context, WatchService.class);
		intent.putExtra("type", "start");
		context.startService(intent);
	}

	private final static String STOP_INTENT = "org.ledyba.meso.StopService";
	public static void stopResidentIfActive(Context context){
		Intent it = new Intent(context, WatchService.class);
		it.setAction(STOP_INTENT);
		context.startService(it);
	}
}
