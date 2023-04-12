package com.ng.cityspot;

public class CitySpotConfig {

	// file name of the SQLite database, this file should be placed in assets folder
	public static final String DATABASE_NAME = "cityspot.db";

	// database version, should be incremented if database has been changed
	public static final int DATABASE_VERSION = 1;

	// debug logs, value is set via build config in build.gradle
	public static final boolean LOGS = BuildConfig.LOGS;

}
