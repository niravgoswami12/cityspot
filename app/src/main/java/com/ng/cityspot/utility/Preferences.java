package com.ng.cityspot.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.GoogleMap;
import com.ng.cityspot.CitySpotApplication;
import com.ng.cityspot.R;

public class Preferences {
	private Context mContext;
	private SharedPreferences mSharedPreferences;

	public Preferences() {
		mContext = CitySpotApplication.getContext();
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
	}

	public void clearPreferences() {
		mSharedPreferences.edit().clear().apply();
	}

	public int getMapType() {
		String key = mContext.getString(R.string.prefs_key_map_type);
		return mSharedPreferences.getInt(key, GoogleMap.MAP_TYPE_NORMAL);
	}

	public void setMapType(int mapType) {
		String key = mContext.getString(R.string.prefs_key_map_type);
		mSharedPreferences.edit().putInt(key, mapType).apply();
	}


}
