package org.ledyba.orangebox.watch;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import android.util.Log;

abstract class AbstractWatcher {
	protected BufferedWriter out;
	private final String TAG;
	public AbstractWatcher() {
		TAG=getClass().getSimpleName();
	}
	abstract protected String getFilename();
	public void start() throws IOException{
		out = new BufferedWriter(new FileWriter(getFilename()));;
	}
	public void stop(){
		try {
			out.flush();
		} catch (IOException e) {
			Log.e(TAG, "file flush error on stop sensor:", e);
		}
		try {
			out.close();
		} catch (IOException e) {
			Log.e(TAG, "file close error on stop sensor:", e);
		}
		out = null;
	}

}
