package com.ng.cityspot;

import android.app.Application;
import android.content.Context;


import org.alfonz.utility.Logcat;

public class CitySpotApplication extends Application {
	private static CitySpotApplication sInstance;

	public CitySpotApplication() {
		sInstance = this;
	}

	public static Context getContext() {
		return sInstance;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// init logcat
		Logcat.init(CitySpotConfig.LOGS, "CitySpot");
	}
}
