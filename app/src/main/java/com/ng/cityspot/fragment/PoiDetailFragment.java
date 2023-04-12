package com.ng.cityspot.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ng.cityspot.R;
import com.ng.cityspot.activity.MapActivity;
import com.ng.cityspot.activity.PoiDetailActivity;
import com.ng.cityspot.database.DatabaseCallListener;
import com.ng.cityspot.database.DatabaseCallManager;
import com.ng.cityspot.database.DatabaseCallTask;
import com.ng.cityspot.database.DatabaseMigrationUtility;
import com.ng.cityspot.database.dao.PoiDAO;
import com.ng.cityspot.database.data.Data;
import com.ng.cityspot.database.model.PoiModel;
import com.ng.cityspot.database.query.PoiReadQuery;
import com.ng.cityspot.database.query.Query;
import com.ng.cityspot.dialog.AboutDialogFragment;
import com.ng.cityspot.geolocation.Geolocation;
import com.ng.cityspot.geolocation.GeolocationListener;
import com.ng.cityspot.glide.GlideUtility;
import com.ng.cityspot.utility.LocationUtility;
import com.ng.cityspot.utility.PermissionRationaleHandler;
import com.ng.cityspot.view.ObservableStickyScrollView;

import org.alfonz.utility.Logcat;
import org.alfonz.utility.PermissionManager;
import org.alfonz.utility.ResourcesUtility;
import org.alfonz.view.StatefulLayout;

import java.sql.SQLException;
import java.util.Date;

public class PoiDetailFragment extends TaskFragment implements DatabaseCallListener, GeolocationListener {
	private static final String DIALOG_ABOUT = "about";
	private static final long TIMER_DELAY = 60000L; // in milliseconds
	private static final int MAP_ZOOM = 14;

	private View mRootView;
	private StatefulLayout mStatefulLayout;
	private DatabaseCallManager mDatabaseCallManager = new DatabaseCallManager();
	private PermissionManager mPermissionManager = new PermissionManager(new PermissionRationaleHandler());
	private Geolocation mGeolocation = null;
	private Location mLocation = null;
	private Handler mTimerHandler;
	private Runnable mTimerRunnable;
	private long mPoiId;
	private PoiModel mPoi;

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

		// handle intent extras
		Bundle extras = getActivity().getIntent().getExtras();
		if (extras != null) {
			handleExtras(extras);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_poi_detail, container, false);
		return mRootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// setup stateful layout
		setupStatefulLayout(savedInstanceState);

		// load data
		if (mPoi == null) loadData();

		// check permissions
		mPermissionManager.request(
				this,
				new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
				(requestable, permissionsResult) -> handlePermissionsResult(permissionsResult));

		// init timer task
		setupTimer();
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();

		// timer
		startTimer();
	}

	@Override
	public void onPause() {
		super.onPause();

		// timer
		stopTimer();

		// stop geolocation
		if (mGeolocation != null) mGeolocation.stop();
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

		// cancel async tasks
		mDatabaseCallManager.cancelAllTasks();
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		// save current instance state
		super.onSaveInstanceState(outState);

		// stateful layout state
		if (mStatefulLayout != null) mStatefulLayout.saveInstanceState(outState);
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		// action bar menu
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_poi_detail, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// action bar menu behavior
		switch (item.getItemId()) {
			case R.id.menu_poi_detail_share:
				if (mPoi != null) {
					startShareActivity(getString(R.string.poi_detail_share_subject), getPoiText());
				}
				return true;


			case R.id.menu_about:
				showAboutDialog();
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
			if (mRootView == null) return; // view was destroyed

			if (task.getQuery().getClass().equals(PoiReadQuery.class)) {
				Logcat.d("PoiReadQuery");

				// get data
				Data<PoiModel> poiReadData = (Data<PoiModel>) data;
				mPoi = poiReadData.getDataObject();
			}

			// hide progress and bind data
			if (mPoi != null) mStatefulLayout.showContent();
			else mStatefulLayout.showEmpty();

			// finish query
			mDatabaseCallManager.finishTask(task);
		});
	}

	@Override
	public void onDatabaseCallFail(final DatabaseCallTask task, final Exception exception) {
		runTaskCallback(() -> {
			if (mRootView == null) return; // view was destroyed

			if (task.getQuery().getClass().equals(PoiReadQuery.class)) {
				Logcat.d("PoiReadQuery / exception " + exception.getClass().getSimpleName() + " / " + exception.getMessage());
			}

			// hide progress
			if (mPoi != null) mStatefulLayout.showContent();
			else mStatefulLayout.showEmpty();

			// handle fail
			handleFail();

			// finish query
			mDatabaseCallManager.finishTask(task);
		});
	}

	@Override
	public void onGeolocationRespond(Geolocation geolocation, final Location location) {
		runTaskCallback(() -> {
			if (mRootView == null) return; // view was destroyed

			Logcat.d("onGeolocationRespond() = " + location.getProvider() + " / " + location.getLatitude() + " / " + location.getLongitude() + " / " + new Date(location.getTime()).toString());
			mLocation = location;
			if (mPoi != null) setupInfoView();
		});
	}

	@Override
	public void onGeolocationFail(Geolocation geolocation) {
		runTaskCallback(() -> {
			if (mRootView == null) return; // view was destroyed

			Logcat.d("onGeolocationFail()");
		});
	}

	private void handlePermissionsResult(PermissionManager.PermissionsResult permissionsResult) {
		Logcat.d(String.format("granted = %b", permissionsResult.isGranted()));
		if (permissionsResult.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) {
			mGeolocation = null;
			mGeolocation = new Geolocation((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE), this);
		}
	}

	private void handleFail() {
		Toast.makeText(getActivity(), R.string.global_database_fail_toast, Toast.LENGTH_LONG).show();
	}

	private void handleExtras(Bundle extras) {
		mPoiId = extras.getLong(PoiDetailActivity.EXTRA_POI_ID);
	}

	private void loadData() {
		// load poi
		if (!mDatabaseCallManager.hasRunningTask(PoiReadQuery.class)) {
			// show progress
			mStatefulLayout.showProgress();

			// run async task
			Query query = new PoiReadQuery(mPoiId);
			mDatabaseCallManager.executeTask(query, this);
		}
	}

	private void showFloatingActionButton(boolean visible) {
		final FloatingActionButton fab = getActivity().findViewById(R.id.fab);
		if (visible) {
			fab.show();
		} else {
			fab.hide();
		}
	}

	private void setupView() {
		setupToolbarView();
		setupInfoView();
		setupMapView();
		setupDescriptionView();
		setupGapView();
	}

	private void setupToolbarView() {
		// reference
		final ObservableStickyScrollView observableStickyScrollView = mRootView.findViewById(R.id.container_content);
		final FloatingActionButton floatingActionButton = getActivity().findViewById(R.id.fab);
		final View panelTopView = mRootView.findViewById(R.id.toolbar_image_panel_top);
		final View panelBottomView = mRootView.findViewById(R.id.toolbar_image_panel_bottom);
		final ImageView imageView = mRootView.findViewById(R.id.toolbar_image_imageview);
		final TextView titleTextView = mRootView.findViewById(R.id.toolbar_image_title);

		// title
		titleTextView.setText(mPoi.getName());

		// image
		Drawable imagePlaceholder = ContextCompat.getDrawable(getContext(), R.drawable.placeholder_photo);
		GlideUtility.loadImage(imageView, mPoi.getImage(), null, imagePlaceholder);

		// scroll view
		observableStickyScrollView.setOnScrollViewListener(new ObservableStickyScrollView.OnScrollViewListener() {
			private final int THRESHOLD = PoiDetailFragment.this.getResources().getDimensionPixelSize(R.dimen.toolbar_image_gap_height);
			private final int PADDING_LEFT = PoiDetailFragment.this.getResources().getDimensionPixelSize(R.dimen.toolbar_image_title_padding_right);
			private final int PADDING_BOTTOM = PoiDetailFragment.this.getResources().getDimensionPixelSize(R.dimen.global_spacing_16);
			private final float SHADOW_RADIUS = 16;

			private int mPreviousY = 0;
			private ColorDrawable mTopColorDrawable = new ColorDrawable();
			private ColorDrawable mBottomColorDrawable = new ColorDrawable();

			@Override
			public void onScrollChanged(ObservableStickyScrollView scrollView, int x, int y, int oldx, int oldy) {
				// floating action button
				if (y > THRESHOLD) {
					if (floatingActionButton.getVisibility() == View.GONE) {
						showFloatingActionButton(true);
					}
				} else {
					if (floatingActionButton.getVisibility() == View.VISIBLE) {
						showFloatingActionButton(false);
					}
				}

				// do not calculate if header is hidden
				if (y > THRESHOLD && mPreviousY > THRESHOLD) return;

				// calculate panel alpha
				int alpha = (int) (y * (255F / (float) THRESHOLD));
				if (alpha > 255) alpha = 255;

				// set color drawables
				mTopColorDrawable.setColor(ResourcesUtility.getValueOfAttribute(getActivity(), R.attr.colorPrimary));
				mTopColorDrawable.setAlpha(alpha);
				mBottomColorDrawable.setColor(ResourcesUtility.getValueOfAttribute(getActivity(), R.attr.colorPrimary));
				mBottomColorDrawable.setAlpha(alpha);

				// set panel background
				panelTopView.setBackground(mTopColorDrawable);
				panelBottomView.setBackground(mBottomColorDrawable);

				// calculate image translation
				float translation = y / 2;

				// set image translation
				imageView.setTranslationY(translation);

				// calculate title padding
				int paddingLeft = (int) (y * (float) PADDING_LEFT / (float) THRESHOLD);
				if (paddingLeft > PADDING_LEFT) paddingLeft = PADDING_LEFT;

				int paddingRight = PADDING_LEFT - paddingLeft;

				int paddingBottom = (int) ((THRESHOLD - y) * (float) PADDING_BOTTOM / (float) THRESHOLD);
				if (paddingBottom < 0) paddingBottom = 0;

				// set title padding
				titleTextView.setPadding(paddingLeft, 0, paddingRight, paddingBottom);

				// calculate title shadow
				float radius = ((THRESHOLD - y) * SHADOW_RADIUS / (float) THRESHOLD);

				// set title shadow
				titleTextView.setShadowLayer(radius, 0F, 0F, ContextCompat.getColor(getActivity(), android.R.color.black));

				// previous y
				mPreviousY = y;
			}
		});

		// invoke scroll event because of orientation change toolbar refresh
		observableStickyScrollView.post(() -> observableStickyScrollView.scrollTo(0, observableStickyScrollView.getScrollY() - 1));

		// floating action button
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) floatingActionButton.getLayoutParams();
		params.topMargin = getResources().getDimensionPixelSize(R.dimen.toolbar_image_collapsed_height) - getResources().getDimensionPixelSize(R.dimen.fab_mini_size);
		floatingActionButton.setLayoutParams(params);
		Drawable fabDrawable = AppCompatResources.getDrawable(getContext(), mPoi.isFavorite() ? R.drawable.ic_menu_favorite_checked : R.drawable.ic_menu_favorite_unchecked);
		floatingActionButton.setImageDrawable(fabDrawable);
		floatingActionButton.setOnClickListener(v -> {
			try {
				mPoi.setFavorite(!mPoi.isFavorite());
				PoiDAO.update(mPoi);
				Drawable favoriteDrawable = AppCompatResources.getDrawable(getContext(), mPoi.isFavorite() ? R.drawable.ic_menu_favorite_checked : R.drawable.ic_menu_favorite_unchecked);
				floatingActionButton.setImageDrawable(favoriteDrawable);
				DatabaseMigrationUtility.backupFavorites();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}

	private void setupInfoView() {
		// reference
		TextView introTextView = mRootView.findViewById(R.id.poi_detail_info_intro);
		TextView addressTextView = mRootView.findViewById(R.id.poi_detail_info_address);
		TextView distanceTextView = mRootView.findViewById(R.id.poi_detail_info_distance);
		TextView linkTextView = mRootView.findViewById(R.id.poi_detail_info_link);
		TextView phoneTextView = mRootView.findViewById(R.id.poi_detail_info_phone);
		TextView emailTextView = mRootView.findViewById(R.id.poi_detail_info_email);

		// intro
		if (mPoi.getIntro() != null && !mPoi.getIntro().trim().equals("")) {
			introTextView.setText(mPoi.getIntro());
			introTextView.setVisibility(View.VISIBLE);
		} else {
			introTextView.setVisibility(View.GONE);
		}

		// address
		if (mPoi.getAddress() != null && !mPoi.getAddress().trim().equals("")) {
			addressTextView.setText(mPoi.getAddress());
			addressTextView.setVisibility(View.VISIBLE);
			addressTextView.setOnClickListener(v -> startMapActivity(mPoi));
		} else {
			addressTextView.setVisibility(View.GONE);
		}

		// distance
		if (mLocation != null) {
			LatLng myLocation = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
			LatLng poiLocation = new LatLng(mPoi.getLatitude(), mPoi.getLongitude());
			String distance = LocationUtility.getDistanceString(LocationUtility.getDistance(myLocation, poiLocation), LocationUtility.isMetricSystem());
			distanceTextView.setText(distance);
			distanceTextView.setVisibility(View.VISIBLE);
			distanceTextView.setOnClickListener(v -> startNavigateActivity(mPoi.getLatitude(), mPoi.getLongitude()));
		} else {
			distanceTextView.setVisibility(View.GONE);
		}

		// link
		if (mPoi.getLink() != null && !mPoi.getLink().trim().equals("")) {
			linkTextView.setText(mPoi.getLink());
			linkTextView.setVisibility(View.VISIBLE);
			linkTextView.setOnClickListener(v -> startWebActivity(mPoi.getLink()));
		} else {
			linkTextView.setVisibility(View.GONE);
		}

		// phone
		if (mPoi.getPhone() != null && !mPoi.getPhone().trim().equals("")) {
			phoneTextView.setText(mPoi.getPhone());
			phoneTextView.setVisibility(View.VISIBLE);
			phoneTextView.setOnClickListener(v -> startCallActivity(mPoi.getPhone()));
		} else {
			phoneTextView.setVisibility(View.GONE);
		}

		// email
		if (mPoi.getEmail() != null && !mPoi.getEmail().trim().equals("")) {
			emailTextView.setText(mPoi.getEmail());
			emailTextView.setVisibility(View.VISIBLE);
			emailTextView.setOnClickListener(v -> startEmailActivity(mPoi.getEmail()));
		} else {
			emailTextView.setVisibility(View.GONE);
		}
	}

	private void setupMapView() {
		// reference
		final ImageView imageView = mRootView.findViewById(R.id.poi_detail_map_image);
		final ViewGroup wrapViewGroup = mRootView.findViewById(R.id.poi_detail_map_image_wrap);
		final Button exploreButton = mRootView.findViewById(R.id.poi_detail_map_explore);
		final Button navigateButton = mRootView.findViewById(R.id.poi_detail_map_navigate);

		// image
		String key = getString(R.string.google_maps_api_key);
		String url = getStaticMapUrl(key, mPoi.getLatitude(), mPoi.getLongitude(), MAP_ZOOM);
		GlideUtility.loadImage(imageView, url, null, null);

		// wrap
		wrapViewGroup.setOnClickListener(v -> startMapActivity(mPoi));

		// explore
		exploreButton.setOnClickListener(v -> startMapActivity(mPoi));

		// navigate
		navigateButton.setOnClickListener(v -> startNavigateActivity(mPoi.getLatitude(), mPoi.getLongitude()));
	}

	private void setupDescriptionView() {
		// reference
		TextView descriptionTextView = mRootView.findViewById(R.id.poi_detail_description_text);

		// content
		if (mPoi.getDescription() != null && !mPoi.getDescription().trim().equals("")) {
			descriptionTextView.setText(Html.fromHtml(mPoi.getDescription()), TextView.BufferType.SPANNABLE);
			descriptionTextView.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

	private void setupGapView() {
		// reference
		final View gapView = mRootView.findViewById(R.id.poi_detail_gap);
		final CardView mapCardView = mRootView.findViewById(R.id.poi_detail_map);

		// add gap in scroll view so favorite floating action button can be shown on tablet
		if (gapView != null) {
			mapCardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					// cardview height
					int cardHeight = mapCardView.getHeight();

					// toolbar height
					int toolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_image_collapsed_height);

					// screen height
					Display display = getActivity().getWindowManager().getDefaultDisplay();
					Point size = new Point();
					display.getSize(size);
					int screenHeight = size.y;

					// calculate gap height
					int gapHeight = screenHeight - cardHeight - toolbarHeight;
					if (gapHeight > 0) {
						ViewGroup.LayoutParams params = gapView.getLayoutParams();
						params.height = gapHeight;
						gapView.setLayoutParams(params);
					}

					// remove layout listener
					mapCardView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				}
			});
		}
	}

	private void setupStatefulLayout(Bundle savedInstanceState) {
		// reference
		mStatefulLayout = (StatefulLayout) mRootView;

		// state change listener
		mStatefulLayout.setOnStateChangeListener((view, state) -> {
			Logcat.d(String.valueOf(state));

			// bind data
			if (state == StatefulLayout.CONTENT) {
				if (mPoi != null) setupView();
			}

			// floating action button
			if (state != StatefulLayout.CONTENT) {
				showFloatingActionButton(false);
			}

			// toolbar background and visibility
			Toolbar toolbar = getActivity().findViewById(R.id.toolbar);

			if (state == StatefulLayout.CONTENT) {
				toolbar.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
			} else {
				toolbar.setBackgroundColor(ResourcesUtility.getValueOfAttribute(getActivity(), R.attr.colorPrimary));
			}

			if (state == StatefulLayout.PROGRESS) {
				toolbar.setVisibility(View.GONE);
			} else {
				toolbar.setVisibility(View.VISIBLE);
			}
		});

		// restore state
		mStatefulLayout.restoreInstanceState(savedInstanceState);
	}

	private void setupTimer() {
		mTimerHandler = new Handler();
		mTimerRunnable = new Runnable() {
			@Override
			public void run() {
				Logcat.d("timer");

				// check access location permission
				if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					// start geolocation
					mGeolocation = null;
					mGeolocation = new Geolocation((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE), PoiDetailFragment.this);
				}

				mTimerHandler.postDelayed(this, TIMER_DELAY);
			}
		};
	}

	private void startTimer() {
		mTimerHandler.postDelayed(mTimerRunnable, 0);
	}

	private void stopTimer() {
		mTimerHandler.removeCallbacks(mTimerRunnable);
	}

	private String getPoiText() {
		StringBuilder builder = new StringBuilder();
		builder.append(mPoi.getName());
		builder.append("\n\n");
		if (mPoi.getAddress() != null && !mPoi.getAddress().trim().equals("")) {
			builder.append(mPoi.getAddress());
			builder.append("\n\n");
		}
		if (mPoi.getIntro() != null && !mPoi.getIntro().trim().equals("")) {
			builder.append(mPoi.getIntro());
			builder.append("\n\n");
		}
		if (mPoi.getDescription() != null && !mPoi.getDescription().trim().equals("")) {
			builder.append(stripHtml(mPoi.getDescription()));
			builder.append("\n\n");
		}
		if (mPoi.getLink() != null && !mPoi.getLink().trim().equals("")) {
			builder.append(mPoi.getLink());
		}
		return builder.toString();
	}

	private String getStaticMapUrl(String key, double lat, double lon, int zoom) {
		TypedValue typedValue = new TypedValue();
		getActivity().getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
		int markerColor = typedValue.data;
		String markerColorHex = String.format("0x%06x", (0xffffff & markerColor));

		return "https://maps.googleapis.com/maps/api/staticmap" +
				"?key=" +
				key +
				"&size=320x320" +
				"&scale=2" +
				"&maptype=roadmap" +
				"&zoom=" +
				zoom +
				"&center=" +
				lat +
				"," +
				lon +
				"&markers=color:" +
				markerColorHex +
				"%7C" +
				lat +
				"," +
				lon;
	}

	@SuppressWarnings("deprecation")
	private String stripHtml(String html) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
		} else {
			return Html.fromHtml(html).toString();
		}
	}

	private void showAboutDialog() {
		// create and show the dialog
		DialogFragment newFragment = AboutDialogFragment.newInstance();
		newFragment.setTargetFragment(this, 0);
		newFragment.show(getFragmentManager(), DIALOG_ABOUT);
	}

	private void startMapActivity(PoiModel poi) {
		Intent intent = MapActivity.newIntent(getActivity(), poi.getId(), poi.getLatitude(), poi.getLongitude());
		startActivity(intent);
	}

	private void startShareActivity(String subject, String text) {
		try {
			Intent intent = new Intent(android.content.Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
			intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
			startActivity(intent);
		} catch (android.content.ActivityNotFoundException e) {
			// can't start activity
		}
	}

	private void startWebActivity(String url) {
		try {
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(intent);
		} catch (android.content.ActivityNotFoundException e) {
			// can't start activity
		}
	}

	private void startCallActivity(String phoneNumber) {
		try {
			String uri = "tel:" + phoneNumber;
			Intent intent = new Intent(android.content.Intent.ACTION_DIAL, Uri.parse(uri));
			startActivity(intent);
		} catch (android.content.ActivityNotFoundException e) {
			// can't start activity
		}
	}

	private void startEmailActivity(String email) {
		try {
			String uri = "mailto:" + email;
			Intent intent = new Intent(android.content.Intent.ACTION_SENDTO, Uri.parse(uri));
			startActivity(intent);
		} catch (android.content.ActivityNotFoundException e) {
			// can't start activity
		}
	}

	private void startNavigateActivity(double lat, double lon) {
		try {
			String uri = String.format("http://maps.google.com/maps?daddr=%s,%s", Double.toString(lat), Double.toString(lon));
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
			startActivity(intent);
		} catch (android.content.ActivityNotFoundException e) {
			// can't start activity
		}
	}
}
