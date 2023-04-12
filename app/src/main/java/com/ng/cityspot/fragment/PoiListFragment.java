package com.ng.cityspot.fragment;

import android.Manifest;
import android.animation.Animator;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ng.cityspot.R;
import com.ng.cityspot.activity.MapActivity;
import com.ng.cityspot.activity.PoiDetailActivity;
import com.ng.cityspot.adapter.PoiListAdapter;
import com.ng.cityspot.database.DatabaseCallListener;
import com.ng.cityspot.database.DatabaseCallManager;
import com.ng.cityspot.database.DatabaseCallTask;
import com.ng.cityspot.database.data.Data;
import com.ng.cityspot.database.model.PoiModel;
import com.ng.cityspot.database.query.PoiReadAllQuery;
import com.ng.cityspot.database.query.PoiReadByCategoryQuery;
import com.ng.cityspot.database.query.PoiReadFavoritesQuery;
import com.ng.cityspot.database.query.PoiSearchQuery;
import com.ng.cityspot.database.query.Query;
import com.ng.cityspot.dialog.AboutDialogFragment;
import com.ng.cityspot.geolocation.Geolocation;
import com.ng.cityspot.geolocation.GeolocationListener;
import com.ng.cityspot.listener.OnSearchListener;
import com.ng.cityspot.utility.LocationUtility;
import com.ng.cityspot.utility.PermissionRationaleHandler;
import com.ng.cityspot.widget.GridSpacingItemDecoration;

import org.alfonz.utility.Logcat;
import org.alfonz.utility.PermissionManager;
import org.alfonz.view.StatefulLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PoiListFragment extends TaskFragment implements DatabaseCallListener, GeolocationListener, PoiListAdapter.PoiViewHolder.OnItemClickListener {
	public static final long CATEGORY_ID_ALL = -1L;
	public static final long CATEGORY_ID_FAVORITES = -2L;
	public static final long CATEGORY_ID_SEARCH = -3L;

	private static final String ARGUMENT_CATEGORY_ID = "category_id";
	private static final String ARGUMENT_SEARCH_QUERY = "search_query";
	private static final String DIALOG_ABOUT = "about";
	private static final long TIMER_DELAY = 60000L; // in milliseconds
	private static final int LAZY_LOADING_TAKE = 10000;
	private static final int LAZY_LOADING_OFFSET = 4;

	private boolean mLazyLoading = false;
	private View mRootView;
	private StatefulLayout mStatefulLayout;
	private PoiListAdapter mAdapter;
	private OnSearchListener mSearchListener;
	private ActionMode mActionMode;
	private DatabaseCallManager mDatabaseCallManager = new DatabaseCallManager();
	private PermissionManager mPermissionManager = new PermissionManager(new PermissionRationaleHandler());
	private Geolocation mGeolocation = null;
	private Location mLocation = null;
	private Handler mTimerHandler;
	private Runnable mTimerRunnable;
	private long mCategoryId;
	private String mSearchQuery;
	private List<PoiModel> mPoiList = new ArrayList<>();
	private List<Object> mFooterList = new ArrayList<>();

	public static PoiListFragment newInstance(long categoryId) {
		PoiListFragment fragment = new PoiListFragment();

		// arguments
		Bundle arguments = new Bundle();
		arguments.putLong(ARGUMENT_CATEGORY_ID, categoryId);
		fragment.setArguments(arguments);

		return fragment;
	}

	public static PoiListFragment newInstance(String searchQuery) {
		PoiListFragment fragment = new PoiListFragment();

		// arguments
		Bundle arguments = new Bundle();
		arguments.putLong(ARGUMENT_CATEGORY_ID, CATEGORY_ID_SEARCH);
		arguments.putString(ARGUMENT_SEARCH_QUERY, searchQuery);
		fragment.setArguments(arguments);

		return fragment;
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);

		// set search listener
		try {
			mSearchListener = (OnSearchListener) getActivity();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().getClass().getName() + " must implement " + OnSearchListener.class.getName());
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);

		// handle fragment arguments
		Bundle arguments = getArguments();
		if (arguments != null) {
			handleArguments(arguments);
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_poi_list, container, false);
		setupRecyclerView();
		return mRootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// setup stateful layout
		setupStatefulLayout(savedInstanceState);

		// load data
		if (mPoiList == null || mPoiList.isEmpty()) loadData();

		// lazy loading progress
		if (mLazyLoading) showLazyLoadingProgress(true);

		// show toolbar if hidden
		showToolbar(true);

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

		// stop adapter
		if (mAdapter != null) mAdapter.stop();

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
		inflater.inflate(R.menu.fragment_poi_list, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// action bar menu behavior
		switch (item.getItemId()) {
			case R.id.menu_poi_list_map:
				startMapActivity();
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
	public void onItemClick(View view, int position, long id, int viewType) {
		// position
		int poiPosition = mAdapter.getPoiPosition(position);

		// start activity
		PoiModel poi = mPoiList.get(poiPosition);
		startPoiDetailActivity(view, poi.getId());


	}

	@Override
	public void onDatabaseCallRespond(final DatabaseCallTask task, final Data<?> data) {
		runTaskCallback(() -> {
			if (mRootView == null) return; // view was destroyed

			if (task.getQuery().getClass().equals(PoiReadAllQuery.class)) {
				Logcat.d("PoiReadAllQuery");

				// get data
				Data<List<PoiModel>> poiReadAllData = (Data<List<PoiModel>>) data;
				List<PoiModel> poiList = poiReadAllData.getDataObject();
				mPoiList.addAll(poiList);
			} else if (task.getQuery().getClass().equals(PoiReadFavoritesQuery.class)) {
				Logcat.d("PoiReadFavoritesQuery");

				// get data
				Data<List<PoiModel>> poiReadFavoritesData = (Data<List<PoiModel>>) data;
				List<PoiModel> poiList = poiReadFavoritesData.getDataObject();
				mPoiList.addAll(poiList);
			} else if (task.getQuery().getClass().equals(PoiSearchQuery.class)) {
				Logcat.d("PoiSearchQuery");

				// get data
				Data<List<PoiModel>> poiSearchData = (Data<List<PoiModel>>) data;
				List<PoiModel> poiList = poiSearchData.getDataObject();
				mPoiList.addAll(poiList);
			} else if (task.getQuery().getClass().equals(PoiReadByCategoryQuery.class)) {
				Logcat.d("PoiReadByCategoryQuery");

				// get data
				Data<List<PoiModel>> poiReadByCategoryData = (Data<List<PoiModel>>) data;
				List<PoiModel> poiList = poiReadByCategoryData.getDataObject();
				mPoiList.addAll(poiList);
			}

			// calculate distances and sort
			calculatePoiDistances();
			sortPoiByDistance();

			// show content
			if (mPoiList != null && !mPoiList.isEmpty()) mStatefulLayout.showContent();
			else mStatefulLayout.showEmpty();
			showLazyLoadingProgress(false);

			// finish query
			mDatabaseCallManager.finishTask(task);
		});
	}

	@Override
	public void onDatabaseCallFail(final DatabaseCallTask task, final Exception exception) {
		runTaskCallback(() -> {
			if (mRootView == null) return; // view was destroyed

			if (task.getQuery().getClass().equals(PoiReadAllQuery.class)) {
				Logcat.d("PoiReadAllQuery / exception " + exception.getClass().getSimpleName() + " / " + exception.getMessage());
			} else if (task.getQuery().getClass().equals(PoiReadFavoritesQuery.class)) {
				Logcat.d("PoiReadFavoritesQuery / exception " + exception.getClass().getSimpleName() + " / " + exception.getMessage());
			} else if (task.getQuery().getClass().equals(PoiSearchQuery.class)) {
				Logcat.d("PoiSearchQuery / exception " + exception.getClass().getSimpleName() + " / " + exception.getMessage());
			} else if (task.getQuery().getClass().equals(PoiReadByCategoryQuery.class)) {
				Logcat.d("PoiReadByCategoryQuery / exception " + exception.getClass().getSimpleName() + " / " + exception.getMessage());
			}

			// hide progress
			if (mPoiList != null && !mPoiList.isEmpty()) mStatefulLayout.showContent();
			else mStatefulLayout.showEmpty();
			showLazyLoadingProgress(false);

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

			// calculate distances and sort
			calculatePoiDistances();
			sortPoiByDistance();
			if (mAdapter != null && mLocation != null && mPoiList != null && !mPoiList.isEmpty())
				mAdapter.notifyDataSetChanged();
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

	private void handleArguments(Bundle arguments) {
		mCategoryId = arguments.getLong(ARGUMENT_CATEGORY_ID, CATEGORY_ID_ALL);
		mSearchQuery = arguments.getString(ARGUMENT_SEARCH_QUERY, "");
	}

	private void loadData() {
		if (!mDatabaseCallManager.hasRunningTask(PoiReadAllQuery.class) &&
				!mDatabaseCallManager.hasRunningTask(PoiReadFavoritesQuery.class) &&
				!mDatabaseCallManager.hasRunningTask(PoiSearchQuery.class) &&
				!mDatabaseCallManager.hasRunningTask(PoiReadByCategoryQuery.class)) {
			// show progress
			mStatefulLayout.showProgress();

			// run async task
			Query query;
			if (mCategoryId == CATEGORY_ID_ALL) {
				query = new PoiReadAllQuery(0, LAZY_LOADING_TAKE);
			} else if (mCategoryId == CATEGORY_ID_FAVORITES) {
				query = new PoiReadFavoritesQuery(0, LAZY_LOADING_TAKE);
			} else if (mCategoryId == CATEGORY_ID_SEARCH) {
				query = new PoiSearchQuery(mSearchQuery, 0, LAZY_LOADING_TAKE);
			} else {
				query = new PoiReadByCategoryQuery(mCategoryId, 0, LAZY_LOADING_TAKE);
			}
			mDatabaseCallManager.executeTask(query, this);
		}
	}

	private void lazyLoadData() {
		// show lazy loading progress
		showLazyLoadingProgress(true);

		// run async task
		Query query;
		if (mCategoryId == CATEGORY_ID_ALL) {
			query = new PoiReadAllQuery(mPoiList.size(), LAZY_LOADING_TAKE);
		} else if (mCategoryId == CATEGORY_ID_FAVORITES) {
			query = new PoiReadFavoritesQuery(mPoiList.size(), LAZY_LOADING_TAKE);
		} else if (mCategoryId == CATEGORY_ID_SEARCH) {
			query = new PoiSearchQuery(mSearchQuery, mPoiList.size(), LAZY_LOADING_TAKE);
		} else {
			query = new PoiReadByCategoryQuery(mCategoryId, mPoiList.size(), LAZY_LOADING_TAKE);
		}
		mDatabaseCallManager.executeTask(query, this);
	}

	private void showLazyLoadingProgress(boolean visible) {
		if (visible) {
			mLazyLoading = true;

			// show footer
			if (mFooterList.size() <= 0) {
				mFooterList.add(new Object());
				getRecyclerView().post(() -> mAdapter.notifyItemInserted(mAdapter.getRecyclerPositionByFooter(0)));
			}
		} else {
			// hide footer
			if (mFooterList.size() > 0) {
				mFooterList.remove(0);
				getRecyclerView().post(() -> mAdapter.notifyItemRemoved(mAdapter.getRecyclerPositionByFooter(0)));
			}

			mLazyLoading = false;
		}
	}

	private void showToolbar(boolean visible) {
		final Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
		if (visible) {
			toolbar.animate()
					.translationY(0)
					.setDuration(200)
					.setInterpolator(new AccelerateDecelerateInterpolator())
					.setListener(new Animator.AnimatorListener() {
						@Override
						public void onAnimationStart(Animator animator) {
							toolbar.setVisibility(View.VISIBLE);
							toolbar.setEnabled(false);
						}

						@Override
						public void onAnimationEnd(Animator animator) {
							toolbar.setEnabled(true);
						}

						@Override
						public void onAnimationCancel(Animator animator) {
						}

						@Override
						public void onAnimationRepeat(Animator animator) {
						}
					});
		} else {
			toolbar.animate()
					.translationY(-toolbar.getBottom())
					.setDuration(200)
					.setInterpolator(new AccelerateDecelerateInterpolator())
					.setListener(new Animator.AnimatorListener() {
						@Override
						public void onAnimationStart(Animator animator) {
							toolbar.setEnabled(false);
						}

						@Override
						public void onAnimationEnd(Animator animator) {
							toolbar.setVisibility(View.GONE);
							toolbar.setEnabled(true);
						}

						@Override
						public void onAnimationCancel(Animator animator) {
						}

						@Override
						public void onAnimationRepeat(Animator animator) {
						}
					});
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
		// reference
		final RecyclerView recyclerView = getRecyclerView();
		final FloatingActionButton floatingActionButton = getActivity().findViewById(R.id.fab);

		// content
		if (recyclerView.getAdapter() == null) {
			// create adapter
			mAdapter = new PoiListAdapter(mPoiList, mFooterList, this, getGridSpanCount());

			// add decoration
			RecyclerView.ItemDecoration itemDecoration = new GridSpacingItemDecoration(getResources().getDimensionPixelSize(R.dimen.poi_list_recycler_item_padding));
			recyclerView.addItemDecoration(itemDecoration);
		} else {
			// refill adapter
			mAdapter.refill(mPoiList, mFooterList, this, getGridSpanCount());
		}

		// set fixed size
		recyclerView.setHasFixedSize(false);

		// set animator
		recyclerView.setItemAnimator(new DefaultItemAnimator());

		// set adapter
		recyclerView.setAdapter(mAdapter);

		// lazy loading
		recyclerView.clearOnScrollListeners();
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			private static final int THRESHOLD = 100;

			private int mCounter = 0;
			private Toolbar mToolbar = getActivity().findViewById(R.id.toolbar);

			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);

				// reset counter
				if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
					mCounter = 0;
				}

				// disable item animation in adapter
				if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
					mAdapter.setAnimationEnabled(false);
				}
			}

			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);

				GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
				int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
				int visibleItemCount = layoutManager.getChildCount();
				int totalItemCount = layoutManager.getItemCount();
				int lastVisibleItem = firstVisibleItem + visibleItemCount;

				// lazy loading
				if (totalItemCount - lastVisibleItem <= LAZY_LOADING_OFFSET && mPoiList.size() % LAZY_LOADING_TAKE == 0 && !mPoiList.isEmpty()) {
					if (!mLazyLoading) lazyLoadData();
				}

				// toolbar and FAB animation
				mCounter += dy;
				if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING || recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_SETTLING) {
					// scroll down
					if (mCounter > THRESHOLD && firstVisibleItem > 0) {
						// hide toolbar
						if (mToolbar.getVisibility() == View.VISIBLE && mToolbar.isEnabled()) {
							showToolbar(false);
						}

						// hide FAB
						showFloatingActionButton(false);

						mCounter = 0;
					}

					// scroll up
					else if (mCounter < -THRESHOLD || firstVisibleItem == 0) {
						// show toolbar
						if (mToolbar.getVisibility() == View.GONE && mToolbar.isEnabled()) {
							showToolbar(true);
						}

						// show FAB
						showFloatingActionButton(true);

						mCounter = 0;
					}
				}
			}
		});

		// floating action button
		floatingActionButton.setOnClickListener(v -> mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(new SearchActionModeCallback()));


	}


	private RecyclerView getRecyclerView() {
		return mRootView != null ? (RecyclerView) mRootView.findViewById(R.id.poi_list_recycler) : null;
	}

	private void setupStatefulLayout(Bundle savedInstanceState) {
		// reference
		mStatefulLayout = (StatefulLayout) mRootView;

		// state change listener
		mStatefulLayout.setOnStateChangeListener((view, state) -> {
			Logcat.d(String.valueOf(state));

			// bind data
			if (state == StatefulLayout.CONTENT) {
				if (mLazyLoading && mAdapter != null) {
					mAdapter.notifyDataSetChanged();
				} else {
					if (mPoiList != null && !mPoiList.isEmpty()) setupView();
				}
			}

			// floating action button
			showFloatingActionButton(state == StatefulLayout.CONTENT);
		});

		// restore state
		mStatefulLayout.restoreInstanceState(savedInstanceState);
	}

	private void setupRecyclerView() {
		GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), getGridSpanCount());
		gridLayoutManager.setOrientation(GridLayoutManager.VERTICAL);
		RecyclerView recyclerView = getRecyclerView();
		recyclerView.setLayoutManager(gridLayoutManager);
	}

	private int getGridSpanCount() {
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		DisplayMetrics displayMetrics = new DisplayMetrics();
		display.getMetrics(displayMetrics);
		float screenWidth = displayMetrics.widthPixels;
		float cellWidth = getResources().getDimension(R.dimen.poi_list_recycler_item_size);
		return Math.round(screenWidth / cellWidth);
	}

	private void calculatePoiDistances() {
		if (mLocation != null && mPoiList != null && !mPoiList.isEmpty()) {
			for (int i = 0; i < mPoiList.size(); i++) {
				PoiModel poi = mPoiList.get(i);
				LatLng myLocation = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
				LatLng poiLocation = new LatLng(poi.getLatitude(), poi.getLongitude());
				int distance = LocationUtility.getDistance(myLocation, poiLocation);
				poi.setDistance(distance);
			}
		}
	}

	private void sortPoiByDistance() {
		if (mLocation != null && mPoiList != null && !mPoiList.isEmpty()) {
			Collections.sort(mPoiList);
		}
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
					mGeolocation = new Geolocation((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE), PoiListFragment.this);
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

	private void showAboutDialog() {
		// create and show the dialog
		DialogFragment newFragment = AboutDialogFragment.newInstance();
		newFragment.setTargetFragment(this, 0);
		newFragment.show(getFragmentManager(), DIALOG_ABOUT);
	}

	private void startPoiDetailActivity(View view, long poiId) {
		Intent intent = PoiDetailActivity.newIntent(getActivity(), poiId);
		ActivityOptions options = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight());
		getActivity().startActivity(intent, options.toBundle());
	}

	private void startMapActivity() {
		Intent intent = MapActivity.newIntent(getActivity());
		startActivity(intent);
	}

	private void startWebActivity(String url) {
		try {
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(intent);
		} catch (android.content.ActivityNotFoundException e) {
			// can't start activity
		}
	}

	private class SearchActionModeCallback implements ActionMode.Callback {
		private SearchView mSearchView;

		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			// search view
			mSearchView = new SearchView(((AppCompatActivity) getActivity()).getSupportActionBar().getThemedContext());
			setupSearchView(mSearchView);

			// search menu item
			Drawable drawable = AppCompatResources.getDrawable(getContext(), R.drawable.ic_menu_search);
			MenuItem searchMenuItem = menu.add(Menu.NONE, Menu.NONE, 1, getString(R.string.menu_search));
			searchMenuItem.setIcon(drawable);
			searchMenuItem.setActionView(mSearchView);
			searchMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
			showFloatingActionButton(false);
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode actionMode) {
			showFloatingActionButton(true);
		}

		private void setupSearchView(SearchView searchView) {
			// expand action view
			searchView.setIconifiedByDefault(true);
			searchView.setIconified(false);
			searchView.onActionViewExpanded();

			// search hint
			searchView.setQueryHint(getString(R.string.menu_search_hint));

			// text color
			AutoCompleteTextView searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
			searchText.setTextColor(ContextCompat.getColor(getActivity(), R.color.global_text_primary_inverse));
			searchText.setHintTextColor(ContextCompat.getColor(getActivity(), R.color.global_text_secondary_inverse));

			// suggestion listeners
			searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
				@Override
				public boolean onQueryTextSubmit(String query) {
					// listener
					mSearchListener.onSearch(query);

					// close action mode
					mActionMode.finish();

					return true;
				}

				@Override
				public boolean onQueryTextChange(String query) {

					return true;
				}
			});
		}


	}
}
