package org.ledyba.meso.battery;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class SensorService extends Service {
	private final static String TAG = "BatteryService";
	private static class SensorWatcher implements SensorEventListener{
		private final Sensor sensor;
		final SensorManager manager;
		private BufferedWriter bw;
		private String getFilename(){
			return
			Environment.getExternalStorageDirectory().getPath()+File.separator+
			String.format(Locale.JAPANESE, "%s_%d.txt", sensor.getName(), new Date().getTime());
		}
		public SensorWatcher(Context ctx, Sensor sensor) {
			this.sensor = sensor;
			this.manager = (SensorManager)ctx.getSystemService(SENSOR_SERVICE);
		}
		public void start() throws IOException{
			manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
			bw = new BufferedWriter(new FileWriter(getFilename()));;
		}
		public void stop(){
			manager.unregisterListener(this, this.sensor);
			try {
				bw.flush();
			} catch (IOException e) {
				Log.e(TAG, "file flush error on stop sensor:", e);
			}
			try {
				bw.close();
			} catch (IOException e) {
				Log.e(TAG, "file close error on stop sensor:", e);
			}
			bw = null;
		}
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			try {
				bw.write(
						String.format(
								"%d-%d/%d/%d",
								event.timestamp,
								event.values[0],
								event.values[1],
								event.values[2]));
			} catch (IOException e) {
				Log.e(TAG, "file write error: ", e);
			}
			
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};

	public SensorService() {
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

	List<SensorWatcher> watchers = new ArrayList<SensorWatcher>();
	@Override
	public void onCreate() {
		super.onCreate();
		final SensorManager manager = (SensorManager)getSystemService(SENSOR_SERVICE);
		List<Sensor> accs = manager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
		List<Sensor> gravs =  manager.getSensorList(Sensor.TYPE_GRAVITY);
		Log.d(TAG, accs.size() +" Acceleration sensors found.");
		Log.d(TAG, gravs.size()+" Gravity sensors found.");
		for( Sensor sensor : accs ) {
			SensorWatcher watcher = new SensorWatcher(this, sensor);
			watchers.add(watcher);
		}
		for( Sensor sensor : gravs ) {
			SensorWatcher watcher = new SensorWatcher(this, sensor);
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

	@Override
	public void onDestroy() {
		super.onDestroy();
		for( SensorWatcher watcher : watchers ) {
			watcher.stop();
		}
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

	public SensorService startResident(Context context) {
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
		Intent it = new Intent(context, SensorService.class);
		it.setAction(STOP_INTENT);
		context.startService(it);
	}
}
