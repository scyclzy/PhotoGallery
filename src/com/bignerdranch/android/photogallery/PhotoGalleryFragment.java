package com.bignerdranch.android.photogallery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;

public class PhotoGalleryFragment extends Fragment {
	private static final String TAG = "PhotoGalleryFragment";
	GridView mGridView;
	ArrayList<GalleryItem> mItems;
	Map<String, Bitmap> photoCache = Collections.synchronizedMap(new HashMap<String, Bitmap>());
	ThumbnailDownloader<ImageView> mThumbnailThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
		mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {

			@Override
			public void onThumbnailDownloaded(ImageView imageView, String url, Bitmap thumbnail) {
				photoCache.put(url, thumbnail);
				if(isVisible()) {
					imageView.setImageBitmap(thumbnail);
				}
			}
		});
		mThumbnailThread.start();
		mThumbnailThread.getLooper();
		Log.i(TAG, "Background thread started");
		
		updateItems();
		
		Intent i = new Intent(getActivity(), PollService.class);
		getActivity().startService(i);
	}


	public void updateItems() {
		mThumbnailThread.clearQueue();
		photoCache.clear();
		new FetchItemsTask().execute();
	}
	

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		
		mGridView = (GridView)v.findViewById(R.id.gridView);
		setupAdapter();
		
		return v;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_photo_gallery, menu);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Pull out the SearchView
			MenuItem searchItem = menu.findItem(R.id.menu_item_search);
			SearchView searchView = (SearchView)searchItem.getActionView();
			
			// Get the data from our searchable.xml as a SearchableInfo
			SearchManager searchManager = (SearchManager)getActivity()
					.getSystemService(Context.SEARCH_SERVICE);
			ComponentName name = getActivity().getComponentName();
			SearchableInfo searchInfo = searchManager.getSearchableInfo(name);
			
			searchView.setSearchableInfo(searchInfo);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_item_search:
			getActivity().onSearchRequested();
			return true;
		case R.id.menu_item_clear:
			PreferenceManager.getDefaultSharedPreferences(getActivity())
				.edit()
				.putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
				.commit();
			updateItems();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mThumbnailThread.quit();
		Log.i(TAG, "Background thread destroyed");
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mThumbnailThread.clearQueue();
	}

	void setupAdapter() {
		if( getActivity() == null || mGridView == null ) {
			return ;
		}
		
		if( mItems != null ) {
			mGridView.setAdapter(new GalleryItemAdapter(mItems));
		} else {
			mGridView.setAdapter(null);
		}
	}
	
	private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>>{

		@Override
		protected ArrayList<GalleryItem> doInBackground(Void... params) {
			Activity activity = getActivity();
			if(activity == null) {
				return new ArrayList<GalleryItem>();
			}
			
			String query = PreferenceManager.getDefaultSharedPreferences(activity)
					.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
			
			if(query != null) {
				return new FlickrFetchr().search(query);
			} else {
				return new FlickrFetchr().fetchItems();
			}
		}

		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			mItems = items;
			setupAdapter();
		}
		
	}

	private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {

		public GalleryItemAdapter(ArrayList<GalleryItem> items) {
			super(getActivity(), 0, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = getActivity().getLayoutInflater()
						.inflate(R.layout.gallery_item, parent, false);
			}
			
			ImageView imageView = (ImageView)convertView
					.findViewById(R.id.gallery_item_imageView);
			
			GalleryItem item = getItem(position);
			if(photoCache.get(item.getUrl()) == null) { 
				imageView.setImageResource(R.drawable.brian_up_close);						
				mThumbnailThread.queueThumbnail(imageView, item.getUrl());
			} else {
				imageView.setImageBitmap(photoCache.get(item.getUrl()));
			}
			
			return convertView;
		}
		
	}
}
