package com.bignerdranch.android.photogallery;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class PhotoPageFragment extends VisibleFragment {
	private String mUrl;
	private WebView mWebView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		mUrl = getActivity().getIntent().getData().toString();
	}

	@SuppressLint("SetJavaScriptEnabled") @Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_photo_page, container, false);
		
		mWebView = (WebView)v.findViewById(R.id.webView);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				return true;
			}
			
		});
		
		mWebView.loadUrl(mUrl);
		
		return v;
	}

}
