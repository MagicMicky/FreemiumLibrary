package com.magicmicky.freemiumlibrary;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import java.util.Set;

/**
 * @author Mickael Goubin
 */
public class AdsInstantiator implements AdListener {
	private static final String TAG = "AdsInstantiator";
	private final AdsFragmentActivity mContext;
	private final String mAdUnitId;
	private final Set<String> mTestDevices;
	private ViewGroup mContainer;
	private View mViewToHide;

	public AdsInstantiator(AdsFragmentActivity context, String adUnitId, View viewToHide, Set<String> testDevices) {
		this.mContext = context;
		this.mViewToHide = viewToHide;
		this.mAdUnitId=adUnitId;
		this.mTestDevices = testDevices;
	}
//TODO: let the user personalize the ad id
	public void addAdsTo(ViewGroup container) {
		this.mContainer =container;
		mContainer.setVisibility(View.VISIBLE);
		View v = mContainer.findViewById(R.id.ad_banner);
		if(v==null) {

			//LayoutInflater inflater = LayoutInflater.from(mContext);
			//mAd = (AdView) inflater.inflate(R.layout.ads, container, false);
			AdView ad = new AdView(mContext,AdSize.BANNER, mAdUnitId);

			/*if(mAd!=null) {
				//mAd.setAdListener(this);
				container.addView(mAd);
			}*/
			ad.setAdListener(this);
			AdRequest request = new AdRequest();
			request.setTestDevices(mTestDevices);
			container.addView(ad);
			request.addTestDevice(AdRequest.TEST_EMULATOR);
			ad.loadAd(request);
		}

	}

	@Override
	public void onReceiveAd(Ad ad) {


	}

	@Override
	public void onFailedToReceiveAd(Ad ad, AdRequest.ErrorCode errorCode) {
		Log.v(TAG, "FAILED TO RECEIVE AD");
		if(mViewToHide!=null) {
			if(mContainer.getChildCount()>0) mContainer.removeAllViews();
			this.mContainer.addView(mViewToHide);
		}
	}

	@Override
	public void onPresentScreen(Ad ad) {

	}

	@Override
	public void onDismissScreen(Ad ad) {

	}

	@Override
	public void onLeaveApplication(Ad ad) {

	}
}
