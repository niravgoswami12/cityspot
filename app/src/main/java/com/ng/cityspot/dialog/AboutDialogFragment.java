package com.ng.cityspot.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.DialogFragment;

import com.ng.cityspot.R;

public class AboutDialogFragment extends DialogFragment {
	public static AboutDialogFragment newInstance() {
		return new AboutDialogFragment();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setCancelable(true);
		setRetainInstance(true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (getDialog() != null) getDialog().setCanceledOnTouchOutside(true);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onDestroyView() {

		if (getDialog() != null && getRetainInstance()) getDialog().setDismissMessage(null);
		super.onDestroyView();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder
				.setTitle(R.string.dialog_about_title)
				.setMessage(Html.fromHtml(getResources().getString(R.string.dialog_about_message)))
				.setPositiveButton(android.R.string.ok, null);

		final AppCompatDialog dialog = builder.create();

		dialog.setOnShowListener(dialogInterface ->
				((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance())
		);

		return dialog;
	}
}
