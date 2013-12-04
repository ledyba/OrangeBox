package org.ledyba.orangebox.watch;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

class SensorWatcher extends AbstractWatcher implements SensorEventListener{
	private final static String TAG="SensorWatcher";
	private final Sensor sensor;
	final SensorManager manager;

	public SensorWatcher(Context ctx, Sensor sensor) {
		this.sensor = sensor;
		this.manager = (SensorManager)ctx.getSystemService(Context.SENSOR_SERVICE);
	}
	@Override
	protected String getFilename(){
		final String base = Environment.getExternalStorageDirectory().getPath();
		final String fname = String.format(Locale.JAPANESE, "%s_%d.txt", sensor.getName(), new Date().getTime());
		return base+File.separator+fname;
	}

	@Override
	public void start() throws IOException{
		super.start();
		manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
	}
	
	@Override
	public void stop(){
		manager.unregisterListener(this, this.sensor);
		super.stop();
	}
	
	private int flushCnt = 0;
	@Override
	public void onSensorChanged(SensorEvent event) {
		if( out == null ) {
			return;
		}
		try {
			final String log = 
					String.format(
					Locale.JAPANESE,
					"%d,%f,%f,%f\n",
					event.timestamp,
					event.values[0],
					event.values[1],
					event.values[2]);
			out.write(log);
			//Log.d(TAG, sensor.getName() + " -> " + log);
			++flushCnt;
			if(flushCnt > 30){
				flushCnt = 0;
				out.flush();
			}
		} catch (IOException e) {
			Log.e(TAG, "file write error: ", e);
		}
		
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
};
