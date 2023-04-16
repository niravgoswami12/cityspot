package com.ng.cityspot.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.ng.cityspot.R;
import com.ng.cityspot.activity.MapActivity;
import com.ng.cityspot.activity.PoiDetailActivity;
import com.ng.cityspot.database.DatabaseCallListener;
import com.ng.cityspot.database.DatabaseCallManager;
import com.ng.cityspot.database.DatabaseCallTask;
import com.ng.cityspot.database.dao.CategoryDAO;
import com.ng.cityspot.database.data.Data;
import com.ng.cityspot.database.model.CategoryModel;
import com.ng.cityspot.database.model.PoiModel;
import com.ng.cityspot.database.query.PoiReadAllQuery;
import com.ng.cityspot.database.query.Query;
import com.ng.cityspot.utility.PermissionRationaleHandler;
import com.ng.cityspot.utility.Preferences;

import org.alfonz.graphics.bitmap.BitmapScaler;
import org.alfonz.utility.Logcat;
import org.alfonz.utility.PermissionManager;
import org.alfonz.utility.VersionUtility;
import org.alfonz.view.StatefulLayout;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapFragment extends TaskFragment implements DatabaseCallListener {
	private static final int MAP_ZOOM = 14;

	private View mRootView;
	private StatefulLayout mStatefulLayout;
	private MapView mMapView;
	private DatabaseCallManager mDatabaseCallManager = new DatabaseCallManager();
	private PermissionManager mPermissionManager = new PermissionManager(new PermissionRationaleHandler());
	private List<PoiModel> mPoiList = new ArrayList<>();
	private ClusterManager<PoiModel> mClusterManager;
	private Map<Long, BitmapDescriptor> mBitmapDescriptorMap = new ArrayMap<>();
	private long mPoiId = -1L;
	private double mPoiLatitude = 0.0;
	private double mPoiLongitude = 0.0;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);

		Bundle extras = getActivity().getIntent().getExtras();
		if (extras != null) {
			handleExtras(extras);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_map, container, false);
		initMap();
		mMapView = mRootView.findViewById(R.id.map_mapview);
		mMapView.onCreate(savedInstanceState);
		return mRootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setupMap();
		setupClusterManager();

		setupStatefulLayout(savedInstanceState);

		if (mPoiList == null || mPoiList.isEmpty()) loadData();

		mPermissionManager.request(
				this,
				new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
				(requestable, permissionsResult) -> handlePermissionsResult(permissionsResult));
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mMapView != null) mMapView.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (mMapView != null) mMapView.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mRootView = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mMapView != null) mMapView.onDestroy();

		mDatabaseCallManager.cancelAllTasks();
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();

		if (mMapView != null) mMapView.onLowMemory();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mStatefulLayout != null) mStatefulLayout.saveInstanceState(outState);

		if (mMapView != null) mMapView.onSaveInstanceState(outState);
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_map, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_map_layers_normal:
				setMapType(GoogleMap.MAP_TYPE_NORMAL);
				return true;

			case R.id.menu_map_layers_satellite:
				setMapType(GoogleMap.MAP_TYPE_SATELLITE);
				return true;

			case R.id.menu_map_layers_hybrid:
				setMapType(GoogleMap.MAP_TYPE_HYBRID);
				return true;

			case R.id.menu_map_layers_terrain:
				setMapType(GoogleMap.MAP_TYPE_TERRAIN);
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		mPermissionManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
	}

	@Override
	public void onDatabaseCallRespond(final DatabaseCallTask task, final Data<?> data) {
		runTaskCallback(() -> {
			if (mRootView == null) return;

			if (task.getQuery().getClass().equals(PoiReadAllQuery.class)) {
				Logcat.d("PoiReadAllQuery");

				Data<List<PoiModel>> poiReadAllData = (Data<List<PoiModel>>) data;
				List<PoiModel> poiList = poiReadAllData.getDataObject();
				mPoiList.clear();
				mPoiList.addAll(poiList);
			}

			if (mPoiList != null && !mPoiList.isEmpty()) mStatefulLayout.showContent();
			else mStatefulLayout.showEmpty();

			mDatabaseCallManager.finishTask(task);
		});
	}

	@Override
	public void onDatabaseCallFail(final DatabaseCallTask task, final Exception exception) {
		runTaskCallback(() -> {
			if (mRootView == null) return;

			if (task.getQuery().getClass().equals(PoiReadAllQuery.class)) {
				Logcat.d("PoiReadAllQuery / exception " + exception.getClass().getSimpleName() + " / " + exception.getMessage());
			}

			if (mPoiList != null && !mPoiList.isEmpty()) mStatefulLayout.showContent();
			else mStatefulLayout.showEmpty();

			handleFail();

			mDatabaseCallManager.finishTask(task);
		});
	}

	private void handlePermissionsResult(PermissionManager.PermissionsResult permissionsResult) {
		Logcat.d(String.format("granted = %b", permissionsResult.isGranted()));
	}

	private void handleFail() {
		Toast.makeText(getActivity(), R.string.global_database_fail_toast, Toast.LENGTH_LONG).show();
	}

	private void handleExtras(Bundle extras) {
		if (extras.containsKey(MapActivity.EXTRA_POI_ID)) {
			mPoiId = extras.getLong(MapActivity.EXTRA_POI_ID);
		}
		if (extras.containsKey(MapActivity.EXTRA_POI_LATITUDE)) {
			mPoiLatitude = extras.getDouble(MapActivity.EXTRA_POI_LATITUDE);
		}
		if (extras.containsKey(MapActivity.EXTRA_POI_LONGITUDE)) {
			mPoiLongitude = extras.getDouble(MapActivity.EXTRA_POI_LONGITUDE);
		}
	}

	private void loadData() {
		if (!mDatabaseCallManager.hasRunningTask(PoiReadAllQuery.class)) {
			mStatefulLayout.showProgress();

			Query query = new PoiReadAllQuery();
			mDatabaseCallManager.executeTask(query, this);
		}
	}

	private void setupView() {
		((MapView) mRootView.findViewById(R.id.map_mapview)).getMapAsync(googleMap -> {
			googleMap.clear();
			mClusterManager.clearItems();
			for (PoiModel poi : mPoiList) {
				mClusterManager.addItem(poi);
			}
			mClusterManager.cluster();
		});

	}

	private void initMap() {
		if (!VersionUtility.isSupportedOpenGlEs2(getActivity())) {
			Toast.makeText(getActivity(), R.string.global_map_fail_toast, Toast.LENGTH_LONG).show();
		}

		try {
			MapsInitializer.initialize(getActivity());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setupStatefulLayout(Bundle savedInstanceState) {
		mStatefulLayout = (StatefulLayout) mRootView;

		mStatefulLayout.setOnStateChangeListener((view, state) -> {
			Logcat.d(String.valueOf(state));

			if (state == StatefulLayout.CONTENT) {
				if (mPoiList != null && !mPoiList.isEmpty()) setupView();
			}
		});

		mStatefulLayout.restoreInstanceState(savedInstanceState);
	}

	private void setupMap() {
		((MapView) mRootView.findViewById(R.id.map_mapview)).getMapAsync(googleMap -> {
			Preferences preferences = new Preferences();

			googleMap.setMapType(preferences.getMapType());

			if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				googleMap.setMyLocationEnabled(true);
			}

			UiSettings settings = googleMap.getUiSettings();
			settings.setAllGesturesEnabled(true);
			settings.setMyLocationButtonEnabled(true);
			settings.setZoomControlsEnabled(true);

			LatLng latLng = null;
			if (mPoiLatitude == 0.0 && mPoiLongitude == 0.0 &&
					ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
				Location location = getLastKnownLocation(locationManager);
				if (location != null) latLng = new LatLng(location.getLatitude(), location.getLongitude());
			} else {
				latLng = new LatLng(mPoiLatitude, mPoiLongitude);
			}

			if (latLng != null) {
				CameraPosition cameraPosition = new CameraPosition.Builder()
						.target(latLng)
						.zoom(MAP_ZOOM)
						.bearing(0)
						.tilt(0)
						.build();
				googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
			}
		});
	}

	private void setupClusterManager() {
		((MapView) mRootView.findViewById(R.id.map_mapview)).getMapAsync(new OnMapReadyCallback() {
			@Override
			public void onMapReady(GoogleMap googleMap) {
				mClusterManager = new ClusterManager<>(getActivity(), googleMap);
				mClusterManager.setRenderer(new DefaultClusterRenderer<PoiModel>(getActivity(), googleMap, mClusterManager) {
					@Override
					protected void onBeforeClusterItemRendered(PoiModel poi, MarkerOptions markerOptions) {
						CategoryModel category = poi.getCategory();
						BitmapDescriptor bitmapDescriptor = loadBitmapDescriptor(category);

						markerOptions.title(poi.getName());
						markerOptions.icon(bitmapDescriptor);

						super.onBeforeClusterItemRendered(poi, markerOptions);
					}
				});
				mClusterManager.setOnClusterItemInfoWindowClickListener(poiModel -> startPoiDetailActivity(poiModel.getId()));
				googleMap.setOnCameraIdleListener(mClusterManager);
				googleMap.setOnInfoWindowClickListener(mClusterManager);
			}
		});
	}

	@SuppressLint("MissingPermission")
	private Location getLastKnownLocation(LocationManager locationManager) {
		Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location locationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		long timeNet = 0L;
		long timeGps = 0L;

		if (locationNet != null) {
			timeNet = locationNet.getTime();
		}

		if (locationGps != null) {
			timeGps = locationGps.getTime();
		}

		if (timeNet > timeGps) return locationNet;
		else return locationGps;
	}

	private void setMapType(final int type) {
		((MapView) mRootView.findViewById(R.id.map_mapview)).getMapAsync(googleMap -> {
			googleMap.setMapType(type);

			Preferences preferences = new Preferences();
			preferences.setMapType(type);
		});
	}

	private BitmapDescriptor loadBitmapDescriptor(CategoryModel category) {
		BitmapDescriptor bitmapDescriptor = mBitmapDescriptorMap.get(category.getId());
		if (bitmapDescriptor == null) {
			try {
				CategoryDAO.refresh(category);
				bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(loadMarkerBitmap(category.getMarker()));
			} catch (SQLException | IOException | IllegalArgumentException e) {
				bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(getColorAccentHue());
			}
			mBitmapDescriptorMap.put(category.getId(), bitmapDescriptor);
		}
		return bitmapDescriptor;
	}

	private Bitmap loadMarkerBitmap(String path) throws IOException, IllegalArgumentException {
		int size = getActivity().getResources().getDimensionPixelSize(R.dimen.map_marker_size);
		InputStream inputStream = getActivity().getAssets().open(path);
		Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
		Bitmap bitmap = BitmapScaler.scaleToFill(originalBitmap, size, size);
		if (originalBitmap != bitmap) originalBitmap.recycle();
		inputStream.close();
		return bitmap;
	}

	private float getColorAccentHue() {

		TypedValue typedValue = new TypedValue();
		getActivity().getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
		int markerColor = typedValue.data;

		float[] hsv = new float[3];
		Color.colorToHSV(markerColor, hsv);
		return hsv[0];
	}

	private void startPoiDetailActivity(long poiId) {
		Intent intent = PoiDetailActivity.newIntent(getActivity(), poiId);
		startActivity(intent);
	}
}