package com.ng.cityspot.database;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;

import androidx.collection.ArraySet;

import com.ng.cityspot.CitySpotApplication;
import com.ng.cityspot.database.dao.PoiDAO;
import com.ng.cityspot.database.model.PoiModel;

import org.alfonz.utility.Logcat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DatabaseMigrationUtility {
	private static final String PREFS_KEY_DATABASE_VERSION = "database_version";
	private static final String PREFS_KEY_FAVORITES_BACKUP = "favorites_backup";

	private DatabaseMigrationUtility() {}

	public static boolean copyPrepopulatedDatabase(String name, String path) {
		// copy database from assets
		try {
			// create directories
			File dir = new File(path).getParentFile();
			dir.mkdirs();

			// input file name
			String inputFileName = name;
			String lang = Locale.getDefault().getLanguage();
			String translatedFileName = lang + "_" + name;
			if (assetExists(translatedFileName)) {
				inputFileName = translatedFileName;
			}
			Logcat.d("lang = %s", lang);
			Logcat.d("inputFileName = %s", inputFileName);

			// output file name
			Logcat.d("outputFileName = %s", path);

			// create streams
			InputStream inputStream = CitySpotApplication.getContext().getAssets().open(inputFileName);
			OutputStream outputStream = new FileOutputStream(path);

			// write input to output
			byte[] buffer = new byte[1024];
			int length;
			while ((length = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, length);
			}

			// close streams
			outputStream.flush();
			outputStream.close();
			inputStream.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean fileExists(String filePath) {
		File file = new File(filePath);
		boolean exists = file.exists();
		Logcat.d("" + exists);
		return exists;
	}

	public static boolean assetExists(String fileName) {
		AssetManager assetManager = CitySpotApplication.getContext().getAssets();
		try {
			List<String> list = Arrays.asList(assetManager.list(""));
			return list.contains(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void backupFavorites() {
		Logcat.d("");
		try {
			List<PoiModel> list = PoiDAO.readFavorites(-1L, -1L);
			Set<String> set = new ArraySet<>();
			for (PoiModel poi : list) {
				Logcat.d("" + poi.getId());
				set.add("" + poi.getId());
			}
			setFavoritesBackup(set);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void restoreFavorites() {
		Logcat.d("");
		try {
			Set<String> set = getFavoritesBackup();
			if (set != null) {
				for (String id : set) {
					Logcat.d(id);
					PoiModel poi = PoiDAO.read(Long.parseLong(id));
					if (poi != null) {
						poi.setFavorite(true);
						PoiDAO.update(poi);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static int getVersion() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CitySpotApplication.getContext());
		return sharedPreferences.getInt(PREFS_KEY_DATABASE_VERSION, 0);
	}

	public static void setVersion(int version) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CitySpotApplication.getContext());
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putInt(PREFS_KEY_DATABASE_VERSION, version);
		editor.apply();
	}

	private static Set<String> getFavoritesBackup() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CitySpotApplication.getContext());
		return sharedPreferences.getStringSet(PREFS_KEY_FAVORITES_BACKUP, null);
	}

	private static void setFavoritesBackup(Set<String> ids) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CitySpotApplication.getContext());
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putStringSet(PREFS_KEY_FAVORITES_BACKUP, ids);
		editor.apply();
	}
}
