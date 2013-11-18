package org.ledyba.orangebox;

import android.content.Context;
import android.content.SharedPreferences;

final class BooleanConfigElement{
	private final String name_;
	private final boolean default_;
	public BooleanConfigElement(String name, boolean default_) {
		this.name_ = name;
		this.default_ = default_;
	}
	public boolean read(final SharedPreferences sp){
		return sp.getBoolean(name_, default_);
	}
	public boolean put(final SharedPreferences sp, boolean val){
		sp.edit().putBoolean(name_, val).apply();
		return val;
	}
}

public class ConfigMaster {
	private final SharedPreferences sp;
	public ConfigMaster(Context ctx) {
		this.sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
	}
	
	private final BooleanConfigElement notificationEnabled_ = new BooleanConfigElement("notification enabled", true);
	public boolean isNotificationEnabled(){
		return notificationEnabled_.read(sp);
	}
	public boolean setNotificationEnabled(boolean val){
		return notificationEnabled_.put(sp, val);
	}

}
