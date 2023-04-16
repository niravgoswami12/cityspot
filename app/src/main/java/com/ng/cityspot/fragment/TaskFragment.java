package com.ng.cityspot.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;

import java.util.LinkedList;
import java.util.List;

public class TaskFragment extends Fragment {
	private final Object mLock = new Object();
	private Boolean mReady = false;
	private List<Runnable> mPendingCallbacks = new LinkedList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		synchronized (mLock) {
			mReady = true;
			int pendingCallbacks = mPendingCallbacks.size();
			while (pendingCallbacks-- > 0) {
				Runnable runnable = mPendingCallbacks.remove(0);
				runNow(runnable);
			}
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		synchronized (mLock) {
			mReady = false;
		}
	}

	protected void runTaskCallback(Runnable runnable) {
		if (mReady) runNow(runnable);
		else addPending(runnable);
	}

	protected void executeTask(AsyncTask<Void, ?, ?> task) {
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void runNow(Runnable runnable) {
		getActivity().runOnUiThread(runnable);
	}

	private void addPending(Runnable runnable) {
		synchronized (mLock) {
			mPendingCallbacks.add(runnable);
		}
	}
}
