package com.ng.cityspot.adapter;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ng.cityspot.CitySpotApplication;
import com.ng.cityspot.R;
import com.ng.cityspot.database.model.PoiModel;
import com.ng.cityspot.glide.GlideUtility;
import com.ng.cityspot.utility.LocationUtility;

import java.util.List;

public class PoiListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private static final int VIEW_TYPE_POI = 1;
	private static final int VIEW_TYPE_FOOTER = 2;

	private List<PoiModel> mPoiList;
	private List<Object> mFooterList;
	private PoiViewHolder.OnItemClickListener mListener;
	private int mGridSpanCount;
	private boolean mAnimationEnabled = true;
	private int mAnimationPosition = -1;

	public PoiListAdapter(List<PoiModel> poiList, List<Object> footerList, PoiViewHolder.OnItemClickListener listener, int gridSpanCount) {
		mPoiList = poiList;
		mFooterList = footerList;
		mListener = listener;
		mGridSpanCount = gridSpanCount;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());

		// inflate view and create view holder
		if (viewType == VIEW_TYPE_POI) {
			View view = inflater.inflate(R.layout.fragment_poi_list_item, parent, false);
			return new PoiViewHolder(view, mListener);
		} else {
			throw new RuntimeException("There is no view type that matches the type " + viewType);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
		// bind data
		if (viewHolder instanceof PoiViewHolder) {
			// entity
			PoiModel poi = mPoiList.get(getPoiPosition(position));

			// bind data
			if (poi != null) {
				((PoiViewHolder) viewHolder).bindData(poi);
			}
		}

		// set item margins
		setItemMargins(viewHolder.itemView, position);

		// set animation
		setAnimation(viewHolder.itemView, position);
	}

	@Override
	public int getItemCount() {
		int size = 0;
		if (mPoiList != null) size += mPoiList.size();
		return size;
	}

	@Override
	public int getItemViewType(int position) {
		int pois = mPoiList.size();

		if (position < pois) return VIEW_TYPE_POI;
		else return -1;
	}

	public int getPoiCount() {
		if (mPoiList != null) return mPoiList.size();
		return 0;
	}


	public int getPoiPosition(int recyclerPosition) {
		return recyclerPosition;
	}

	public int getFooterPosition(int recyclerPosition) {
		return recyclerPosition - getPoiCount();
	}

	public int getRecyclerPositionByFooter(int footerPosition) {
		return footerPosition + getPoiCount();
	}

	public void refill(List<PoiModel> poiList, List<Object> footerList, PoiViewHolder.OnItemClickListener listener, int gridSpanCount) {
		mPoiList = poiList;
		mFooterList = footerList;
		mListener = listener;
		mGridSpanCount = gridSpanCount;
		notifyDataSetChanged();
	}

	public void stop() {
	}

	public void setAnimationEnabled(boolean animationEnabled) {
		mAnimationEnabled = animationEnabled;
	}

	private void setAnimation(final View view, int position) {
		if (mAnimationEnabled && position > mAnimationPosition) {
			view.setScaleX(0F);
			view.setScaleY(0F);
			view.animate()
					.scaleX(1F)
					.scaleY(1F)
					.setDuration(300)
					.setInterpolator(new DecelerateInterpolator());

			mAnimationPosition = position;
		}
	}

	private void setItemMargins(View view, int position) {
		int marginTop = 0;

		if (position < mGridSpanCount) {
			TypedArray a = CitySpotApplication.getContext().obtainStyledAttributes(null, new int[]{android.R.attr.actionBarSize}, 0, 0);
			marginTop = (int) a.getDimension(0, 0);
			a.recycle();
		}

		ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
		marginParams.setMargins(0, marginTop, 0, 0);
	}

	public static final class PoiViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		private TextView mNameTextView;
		private TextView mDistanceTextView;
		private ImageView mImageView;
		private Drawable mImagePlaceholder;
		private OnItemClickListener mListener;

		public interface OnItemClickListener {
			void onItemClick(View view, int position, long id, int viewType);
		}

		public PoiViewHolder(View itemView, OnItemClickListener listener) {
			super(itemView);
			mListener = listener;

			// set listener
			itemView.setOnClickListener(this);

			// find views
			mNameTextView = itemView.findViewById(R.id.poi_list_item_name);
			mDistanceTextView = itemView.findViewById(R.id.poi_list_item_distance);
			mImageView = itemView.findViewById(R.id.poi_list_item_image);

			// placeholder
			mImagePlaceholder = ContextCompat.getDrawable(itemView.getContext(), R.drawable.placeholder_photo);
		}

		@Override
		public void onClick(View view) {
			int position = getAdapterPosition();
			if (position != RecyclerView.NO_POSITION) {
				mListener.onItemClick(view, position, getItemId(), getItemViewType());
			}
		}

		public void bindData(PoiModel poi) {
			mNameTextView.setText(poi.getName());
			GlideUtility.loadImage(mImageView, poi.getImage(), null, mImagePlaceholder);

			if (poi.getDistance() > 0) {
				String distance = LocationUtility.getDistanceString(poi.getDistance(), LocationUtility.isMetricSystem());
				mDistanceTextView.setText(distance);
				mDistanceTextView.setVisibility(View.VISIBLE);
			} else {
				mDistanceTextView.setVisibility(View.GONE);
			}
		}
	}

	public static final class FooterViewHolder extends RecyclerView.ViewHolder {
		public FooterViewHolder(View itemView) {
			super(itemView);
		}

		public void bindData(Object object) {
			// do nothing
		}
	}
}
