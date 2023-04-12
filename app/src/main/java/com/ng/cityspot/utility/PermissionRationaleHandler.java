package com.ng.cityspot.utility;

import android.Manifest;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.ng.cityspot.CitySpotApplication;
import com.ng.cityspot.R;

import org.alfonz.utility.PermissionManager;

public class PermissionRationaleHandler implements PermissionManager.RationaleHandler {
	@Override
	public String getRationaleMessage(@NonNull String permission) {
		int resId;

		switch (permission) {
			case Manifest.permission.ACCESS_COARSE_LOCATION:
			case Manifest.permission.ACCESS_FINE_LOCATION:
				resId = R.string.permission_access_location;
				break;
			default:
				resId = R.string.permission_unknown;
		}

		return CitySpotApplication.getContext().getString(resId);
	}

	@Override
	public void showRationale(@NonNull View rootView, @NonNull String rationaleMessage, @NonNull PermissionManager.ConfirmAction confirmAction) {
		Snackbar
				.make(rootView, rationaleMessage, Snackbar.LENGTH_INDEFINITE)
				.setAction(android.R.string.ok, view -> confirmAction.run())
				.show();
	}
}
