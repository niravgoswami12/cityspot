package com.ng.cityspot.widget;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
	private int mSpacing;

	public GridSpacingItemDecoration(int spacingPixelSize) {
		mSpacing = spacingPixelSize;
	}

	@Override
	public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.State state) {
		super.getItemOffsets(outRect, view, recyclerView, state);

		int position = recyclerView.getChildLayoutPosition(view);
		int spanCount = getSpanCount(recyclerView);
		int itemCount = recyclerView.getAdapter().getItemCount();

		// top offset
		if (position < spanCount) {
			outRect.top = mSpacing;
		} else {
			outRect.top = mSpacing / 2;
		}

		// bottom offset
		if (itemCount % spanCount == 0 && position >= itemCount - spanCount) {
			outRect.bottom = mSpacing;
		} else if (itemCount % spanCount != 0 && position >= itemCount - itemCount % spanCount) {
			outRect.bottom = mSpacing;
		} else {
			outRect.bottom = mSpacing / 2;
		}

		// left offset
		if (position % spanCount == 0) {
			outRect.left = mSpacing;
		} else {
			outRect.left = mSpacing / 2;
		}

		// right offset
		if (position % spanCount == spanCount - 1) {
			outRect.right = mSpacing;
		} else {
			outRect.right = mSpacing / 2;
		}
	}

	private int getSpanCount(RecyclerView recyclerView) {
		if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
			GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
			return gridLayoutManager.getSpanCount();
		} else {
			throw new IllegalStateException(this.getClass().getSimpleName() + " can only be used with a " + GridLayoutManager.class.getSimpleName());
		}
	}
}
