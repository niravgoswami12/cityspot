package com.ng.cityspot.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public final class GlideUtility {
	private GlideUtility() {}

	public static void loadImage(ImageView imageView, String uri, Drawable placeholder, Drawable error) {
		Glide
				.with(imageView.getContext())
				.asBitmap()
				.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
				.transition(BitmapTransitionOptions.withCrossFade())
				.placeholder(placeholder)
				.error(error)
				.load(convertAssetsUri(uri))
				.into(imageView);
	}

	public static void loadMenuItemIcon(final Context context, final MenuItem menuItem, String uri) {
		Glide
				.with(context)
				.asBitmap()
				.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
				.transition(BitmapTransitionOptions.withCrossFade())
				.load(convertAssetsUri(uri))
				.into(new CustomTarget<Bitmap>() {
					@Override
					public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
						Drawable drawable = new BitmapDrawable(context.getResources(), resource);
						menuItem.setIcon(drawable);
					}

					@Override
					public void onLoadCleared(@Nullable Drawable placeholder) {
					}
				});
	}

	private static String convertAssetsUri(String uri) {
		if (uri != null) {
			return uri.replace("assets://", "file:///android_asset/");
		} else {
			return null;
		}
	}
}
