package com.ng.cityspot.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ng.cityspot.R;

public class MapActivity extends AppCompatActivity {
	public static final String EXTRA_POI_ID = "poi_id";
	public static final String EXTRA_POI_LATITUDE = "poi_latitude";
	public static final String EXTRA_POI_LONGITUDE = "poi_longitude";

	public static Intent newIntent(Context context) {
		return new Intent(context, MapActivity.class);
	}

	public static Intent newIntent(Context context, long poiId, double poiLatitude, double poiLongitude) {
		Intent intent = new Intent(context, MapActivity.class);

		// extras
		intent.putExtra(EXTRA_POI_ID, poiId);
		intent.putExtra(EXTRA_POI_LATITUDE, poiLatitude);
		intent.putExtra(EXTRA_POI_LONGITUDE, poiLongitude);

		return intent;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		setupActionBar();
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void setupActionBar() {
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayUseLogoEnabled(false);
		bar.setDisplayShowTitleEnabled(true);
		bar.setDisplayShowHomeEnabled(true);
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setHomeButtonEnabled(true);
	}
}
