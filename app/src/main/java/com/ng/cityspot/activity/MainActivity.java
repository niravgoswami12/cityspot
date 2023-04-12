package com.ng.cityspot.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;
import com.ng.cityspot.R;
import com.ng.cityspot.database.dao.CategoryDAO;
import com.ng.cityspot.database.model.CategoryModel;
import com.ng.cityspot.fragment.PoiListFragment;
import com.ng.cityspot.glide.GlideUtility;
import com.ng.cityspot.listener.OnSearchListener;

import java.sql.SQLException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnSearchListener {
	private static final String CATEGORY_IMAGE_ALL = "assets://categories/all.png";
	private static final String CATEGORY_IMAGE_FAVORITES = "assets://categories/favorites.png";
	private static final String SAVED_TITLE = "title";
	private static final String SAVED_NAVIGATION = "navigation";

	private DrawerLayout mDrawerLayout;
	private NavigationView mNavigationView;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mTitle;
	private List<CategoryModel> mCategoryList;

	public static Intent newIntent(Context context) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setupActionBar();
		setupDrawer(savedInstanceState);

		// restore saved state
		if (savedInstanceState != null) {
			handleSavedInstanceState(savedInstanceState);
		}

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
	public void onSaveInstanceState(@NonNull Bundle outState) {
		// save current instance state
		super.onSaveInstanceState(outState);

		if (mTitle != null) {
			outState.putCharSequence(SAVED_TITLE, mTitle);
		}

		if (mNavigationView != null && mNavigationView.getCheckedItem() != null) {
			outState.putInt(SAVED_NAVIGATION, mNavigationView.getCheckedItem().getItemId());
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		// open or close the drawer if home button is pressed
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfiguration) {
		super.onConfigurationChanged(newConfiguration);
		mDrawerToggle.onConfigurationChanged(newConfiguration);
	}

	@Override
	public void onBackPressed() {
		if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
			mDrawerLayout.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	@Override
	public void onSearch(String query) {
		Fragment fragment = PoiListFragment.newInstance(query);
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.container_drawer_content, fragment).commitAllowingStateLoss();
		checkMenuItem(getString(R.string.title_search) + ": " + query, 0);
	}

	private void handleSavedInstanceState(Bundle savedInstanceState) {
		CharSequence title = savedInstanceState.getCharSequence(SAVED_TITLE, getString(R.string.app_name));
		int navigation = savedInstanceState.getInt(SAVED_NAVIGATION);
		checkMenuItem(title, navigation);
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

	private void setupDrawer(Bundle savedInstanceState) {
		mTitle = getTitle();

		// reference
		mDrawerLayout = findViewById(R.id.main_drawer_layout);
		mNavigationView = findViewById(R.id.main_drawer_navigation);

		// load category list
		loadCategoryList();

		// add menu items
		MenuItem firstItem = setupMenu(mNavigationView.getMenu());

		// navigation listener
		mNavigationView.setNavigationItemSelectedListener(item -> {
			selectDrawerItem(item);
			return true;
		});

		// drawer toggle
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
			@Override
			public void onDrawerClosed(View view) {
				getSupportActionBar().setTitle(mTitle);
				supportInvalidateOptionsMenu();
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				getSupportActionBar().setTitle(mTitle);
				supportInvalidateOptionsMenu();
			}
		};
		mDrawerLayout.addDrawerListener(mDrawerToggle);

		// show initial fragment
		if (savedInstanceState == null) {
			selectDrawerItem(firstItem);
		}
	}

	private MenuItem setupMenu(Menu menu) {
		// clear menu
		menu.clear();

		// add menu items
		int groupId = 0;
		MenuItem firstItem = null;
		for (int i = 0; i < mCategoryList.size(); i++) {
			CategoryModel category = mCategoryList.get(i);
			MenuItem item = menu.add(groupId, i, i, category.getName());
			GlideUtility.loadMenuItemIcon(this, item, category.getImage());
			if (i == 1) groupId = 1; // categories from database in a separate group
			if (firstItem == null) firstItem = item;
		}

		return firstItem;
	}

	private void selectDrawerItem(MenuItem item) {
		int position = item.getItemId();

		Fragment fragment = PoiListFragment.newInstance(mCategoryList.get(position).getId());
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.container_drawer_content, fragment).commitAllowingStateLoss();

		checkMenuItem(mCategoryList.get(position).getName(), item);
		mDrawerLayout.closeDrawers();
	}

	private void checkMenuItem(CharSequence title, int navigationId) {
		MenuItem menuItem = mNavigationView.getMenu().findItem(navigationId);
		checkMenuItem(title, menuItem);
	}

	private void checkMenuItem(CharSequence title, MenuItem menuItem) {
		menuItem.setCheckable(true);
		mNavigationView.setCheckedItem(menuItem);
		setTitle(title);
	}

	private void loadCategoryList() {
		try {
			mCategoryList = CategoryDAO.readAll(-1L, -1L);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		CategoryModel all = new CategoryModel();
		all.setId(PoiListFragment.CATEGORY_ID_ALL);
		all.setName(getResources().getString(R.string.menu_navigation_all));
		all.setImage(CATEGORY_IMAGE_ALL);

		CategoryModel favorites = new CategoryModel();
		favorites.setId(PoiListFragment.CATEGORY_ID_FAVORITES);
		favorites.setName(getResources().getString(R.string.menu_navigation_favorites));
		favorites.setImage(CATEGORY_IMAGE_FAVORITES);

		mCategoryList.add(0, all);
		mCategoryList.add(1, favorites);
	}
}
