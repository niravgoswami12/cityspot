package com.ng.cityspot.database;

import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.ng.cityspot.CitySpotApplication;
import com.ng.cityspot.CitySpotConfig;
import com.ng.cityspot.database.model.CategoryModel;
import com.ng.cityspot.database.model.PoiModel;

import org.alfonz.utility.Logcat;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
	private static final String DATABASE_NAME = CitySpotConfig.DATABASE_NAME;
	private static final String DATABASE_PATH = CitySpotApplication.getContext().getDatabasePath(CitySpotConfig.DATABASE_NAME).getPath();
	private static final int DATABASE_VERSION = CitySpotConfig.DATABASE_VERSION;

	private static DatabaseHelper sInstance;

	private Dao<CategoryModel, Long> mCategoryDao = null;
	private Dao<PoiModel, Long> mPoiDao = null;
	private boolean mMigration = false;

	private DatabaseHelper() {
		super(CitySpotApplication.getContext(), DATABASE_PATH, null, DATABASE_VERSION);

		boolean exists = DatabaseMigrationUtility.fileExists(DATABASE_PATH);
		boolean update = DATABASE_VERSION > DatabaseMigrationUtility.getVersion();

		if (exists && update) {
			mMigration = true;
		}

		if (!exists || update) {
			synchronized (this) {
				boolean success = DatabaseMigrationUtility.copyPrepopulatedDatabase(DATABASE_NAME, DATABASE_PATH);
				if (success) {
					DatabaseMigrationUtility.setVersion(DATABASE_VERSION);
				}
			}
		}
	}

	public static synchronized DatabaseHelper getInstance() {
		if (sInstance == null) sInstance = new DatabaseHelper();
		return sInstance;
	}

	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		try {
			Logcat.d("");

			if (mMigration) {
				DatabaseMigrationUtility.restoreFavorites();
				mMigration = false;
			}
		} catch (android.database.SQLException e) {
			Logcat.e(e, "can't create database");
			e.printStackTrace();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {
			Logcat.d("");
		} catch (android.database.SQLException e) {
			Logcat.e(e, "can't upgrade database");
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		super.close();
		mCategoryDao = null;
		mPoiDao = null;
	}

	public synchronized void clearDatabase() {
		try {
			Logcat.d("");

			TableUtils.dropTable(getConnectionSource(), CategoryModel.class, true);
			TableUtils.dropTable(getConnectionSource(), PoiModel.class, true);

			TableUtils.createTable(getConnectionSource(), CategoryModel.class);
			TableUtils.createTable(getConnectionSource(), PoiModel.class);
		} catch (android.database.SQLException | java.sql.SQLException e) {
			Logcat.e(e, "can't clear database");
			e.printStackTrace();
		}
	}

	public synchronized Dao<CategoryModel, Long> getCategoryDao() throws java.sql.SQLException {
		if (mCategoryDao == null) {
			mCategoryDao = getDao(CategoryModel.class);
		}
		return mCategoryDao;
	}

	public synchronized Dao<PoiModel, Long> getPoiDao() throws java.sql.SQLException {
		if (mPoiDao == null) {
			mPoiDao = getDao(PoiModel.class);
		}
		return mPoiDao;
	}
}
