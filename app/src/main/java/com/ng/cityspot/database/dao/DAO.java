package com.ng.cityspot.database.dao;

import com.ng.cityspot.database.DatabaseHelper;

import org.alfonz.utility.Logcat;

import java.sql.SQLException;

public class DAO {
	public static void printDatabaseInfo() {
		DatabaseHelper databaseHelper = DatabaseHelper.getInstance();
		try {
			Logcat.d("%d categories", databaseHelper.getCategoryDao().countOf());
			Logcat.d("%d pois", databaseHelper.getPoiDao().countOf());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
