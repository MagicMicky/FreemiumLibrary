package com.magicmicky.freemiumlibrary;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.magicmicky.freemiumlibrary.util.IabHelper;
import com.magicmicky.freemiumlibrary.util.IabResult;
import com.magicmicky.freemiumlibrary.util.Inventory;
import com.magicmicky.freemiumlibrary.util.Purchase;

import java.util.Set;

/**
 * TODO: allow a personalisation of the MenuButton (let the user personalize the icon) see #setPremiumMenuButton
 * @author Mickael Goubin
 */
public abstract class AdsFragmentActivity extends Activity {

	private static final int MENU_PREMIUM = Menu.FIRST + 100;
	private static final String TAG = "UpdateToPremiumAct";
	//private final String developerPayLoad="";
	private IabHelper mHelper;
	private static final int RC_REQUEST = 10001;
	private boolean mIsPremium;
	/*
	 * Drawer
	 */
	private boolean mDrawerButton =false;
	private int mDrawerButtonContainerRes;
	private View mDrawerButtonView;

	/*
	 * Ads
	 */
	private boolean mDoAdsForNonPremium =false;
	private View mAdsReplacement=null;
	private int mAdsContainerRes;
	private boolean mUpgradeLinkOnFailure =false;
	private String mAdUnitId;

	/*
	 * MenuButton
	 */
	private boolean mPremiumMenuButton =false;

	/*
	 * Other
	 */
	private String mBase64EncodedPublicKey;
	private String mSkuPremium =null;
	private Set<String> mTestDevices;


	/**
	 * Is the user in premium mode?
	 * @return true if the user is a premium user, false otherwise
	 */
	public boolean isPremium() {
		return mIsPremium;
	}

	/**
	 * Whether or not you want to create a menu button to let the user see he can update to premium. Default is false.
	 * <strong>Warning</strong>: When setting up this mode, you *NEED* to call {@code super.onCreateOptionsMenu()} and {@code super.onOptionsItemSelected()} at some points.
	 * Otherwise the menu won't be populated with the message.
	 * @param menuButton whether or not you want this menu button to appear
	 */
	public void setPremiumMenuButton(boolean menuButton) {
		this.mPremiumMenuButton =menuButton;
	}



	/**
	 * Whether or not you want to show the user that he can update to premium in the drawer.<br/>
	 * You can personalize the buttons view via {@link #setDrawerButtonView}
	 * @param drawerButton whether or not you want the link in the drawer
	 * @param drawerButtonViewGroupRes the name of the viewgroup that will contain this link
	 * @see #setDrawerButtonView(android.view.View)
	 * @see #isDrawerButton()
	 * @see #setDrawerButtonContainerRes(int)
	 */
	public void setDrawerButton(boolean drawerButton, int drawerButtonViewGroupRes) {
		this.mDrawerButton =drawerButton;
		this.mDrawerButtonContainerRes =drawerButtonViewGroupRes;
	}

	/**
	 * Set the drawer buttons view
	 * @param drawerButtonView
	 * @see #setDrawerButton(boolean, int)
	 */
	public void setDrawerButtonView(View drawerButtonView) {
		this.mDrawerButtonView = drawerButtonView;
	}

	/**
	 * @return If there is a link in the drawer menu to update to premium
	 * @see #setDrawerButton(boolean, int)
	 */
	public boolean isDrawerButton() {
		return mDrawerButton;
	}

	/**
	 * Sets the DrawerButtonContainer resource file
	 * @param drawerButtonContainerRes the drawer button container resource file
	 * @see #setDrawerButton(boolean, int)
	 */
	public void setDrawerButtonContainerRes(int drawerButtonContainerRes) {
		this.mDrawerButtonContainerRes = drawerButtonContainerRes;
	}

	/**
	 * @return the current drawer button viewgroup resource file
	 * @see #setDrawerButton(boolean, int)
	 * @see #setDrawerButtonContainerRes(int)
	 */
	public int getDrawerButtonViewGroupRes() {
		return this.mDrawerButtonContainerRes;
	}
	/**
	 * Whether or not you want to show ads for non premium user. Requires a viewgroup named "adView".
	 * @param doAdsForNonPremium whether or not you want to do ads for non premium user
	 * @param adsViewGroupRes the viewgroup you want the ads in.
	 * @param upgradeLinkOnFailure whether or not you want to put a textview to replace the ad and link to the upgrade
	 * @see #isDoAdsForNonPremium()
	 * @see #setAdsContainerRes(int)
	 * @see #setAdsViewReplacement(android.view.View)
	 */
	public void setDoAdsForNonPremium(boolean doAdsForNonPremium, int adsViewGroupRes, boolean upgradeLinkOnFailure) {
		this.mDoAdsForNonPremium = doAdsForNonPremium;
		this.mAdsContainerRes = adsViewGroupRes;
		this.mUpgradeLinkOnFailure = upgradeLinkOnFailure;
	}

	/**
	 * Sets the test devices for the ad
	 * @param testDevices a Set of the testdevices
	 */
	public void setTestDevice(Set<String> testDevices) {
		this.mTestDevices = testDevices;
	}

	/**
	 * @return If a non premium user will see ads or not
	 * @see #setDoAdsForNonPremium(boolean, int, boolean)
	 */
	public boolean isDoAdsForNonPremium() {
		return this.mDoAdsForNonPremium;
	}

	/**
	 * Sets the ads view replacement, when the ads or blocked/ the user has no internet.<br/>
	 * If {@code isUpgradeLinkOnFailure()==true} then this will link to the upgrade menu
	 * @param adsReplacement the view to be set as an ad replacement
	 * @see #setDoAdsForNonPremium(boolean, int, boolean)
	 * @see #isUpgradeLinkOnFailure()
	 */
	public void setAdsViewReplacement(View adsReplacement) {
		this.mAdsReplacement = adsReplacement;
	}
	/**
	 * @return If there is a link in the ActionBar Menu to update to premium
	 * @see #setDoAdsForNonPremium(boolean, int, boolean)
	 */
	public boolean isPremiumMenuButton() {
		return this.mPremiumMenuButton;
	}

	/**
	 * @return whether or not we should display an upgrade link on ads failure
	 * @see #setDoAdsForNonPremium(boolean, int, boolean)
	 */
	public boolean isUpgradeLinkOnFailure() {
		return this.mUpgradeLinkOnFailure;
	}

	/**
	 * Sets the ads container resource file
	 * @param adsContainerRes the new ad container resource file
	 * @see #setDoAdsForNonPremium(boolean, int, boolean)
	 */
	public void setAdsContainerRes(int adsContainerRes) {
		this.mAdsContainerRes = adsContainerRes;
	}

	/**
	 * @return the current ads Container resource file
	 * @see #setDoAdsForNonPremium(boolean, int, boolean)
	 */
	public int getAdsContainerRes() {
		return mAdsContainerRes;
	}

	/**
	 * Your application's public key, encoded in base64. This is used for verification of purchase signatures. You can find your app's base64-encoded public key in your application's page on Google Play Developer Console. Note that this is NOT your "developer public key".
	 * @param base64EncodedPublicKey the public key
	 */
	public void setAppPublicKey(String base64EncodedPublicKey) {
		this.mBase64EncodedPublicKey = base64EncodedPublicKey;
	}

	/**
	 * set the premium mode package
	 * @param skuPremium Your application's premium in-app item that allows the user to get premium
	 */
	public void setPremiumPackageSKU(String skuPremium) {
		this.mSkuPremium = skuPremium;
	}

	/**
	 * set the ad Unit Id
	 * @param adUnitId the ad id
	 */
	public void setAdUnitId(String adUnitId) {
		this.mAdUnitId=adUnitId;
	}




	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.updatetopremium);

		// Create the helper, passing it our context and the public key to verify signatures with
		Log.d(TAG, "Creating IAB helper.");
		mHelper = new IabHelper(this, mBase64EncodedPublicKey);

		// enable debug logging (for a production application, you should set this to false).
		mHelper.enableDebugLogging(true);

		// Start setup. This is asynchronous and the specified listener
		// will be called once setup completes.
		Log.d(TAG, "Starting setup.");
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				Log.d(TAG, "Setup finished.");

				if (!result.isSuccess()) {
					// Oh noes, there was a problem.
					Log.e(TAG, "Problem setting up in-app billing: " + result);
					return;
				}

				// Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
				Log.d(TAG, "Setup successful. Querying inventory.");
				mHelper.queryInventoryAsync(mGotInventoryListener);

			}
		});
	}
	@Override
	protected void onResume() {
		super.onResume();
		this.mIsPremium=getPremiumFromPrefs();
		if(!this.isPremium()) {
			if(this.isPremiumMenuButton()) {
				//Nothing to do
				//Should check if calls to the super methods were done, but no way to do it properly...
			}

			if(this.isDoAdsForNonPremium()) {
				this.doAds();
			}

			if(this.isDrawerButton()) {
				this.showDrawerButton();
			}
		}
		else {
			if(isDoAdsForNonPremium())
				this.hideAdsContainer();
			if(isDrawerButton())
				this.hideDrawerButtonContainer();
		}
	}



	private void hideAdsContainer() {
		this.findViewById(this.mAdsContainerRes).setVisibility(View.GONE);
	}

	protected  void hideDrawerButtonContainer() {
		this.findViewById(this.mDrawerButtonContainerRes).setVisibility(View.GONE);
	}



	private void doAds() throws PremiumModeException.WrongLayoutException {
		ViewGroup adsContainer = (ViewGroup) this.findViewById(this.getAdsContainerRes());
		View adsReplacement=null;
		if (adsContainer == null) {
			throw (new PremiumModeException.WrongLayoutException(false));
		}
		adsContainer.removeAllViews();
		if (this.isUpgradeLinkOnFailure()) {
			if(mAdsReplacement!=null) {
				adsReplacement = mAdsReplacement;
			} else {
				adsReplacement = getLayoutInflater().inflate(R.layout.ads_replacement_default,adsContainer, false);
			}

			if(adsReplacement!=null) {
				adsContainer.removeAllViews();
				adsContainer.addView(adsReplacement);
				adsReplacement.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View view) {
						onUpgrade();
					}
				});
			}
		}
		AdsInstantiator customAdsInstantiator = new AdsInstantiator(this,mAdUnitId, adsReplacement, mTestDevices);
		adsContainer.setVisibility(View.VISIBLE);
		customAdsInstantiator.addAdsTo(adsContainer);
		Log.d(TAG, "user is not premium: instantiating adds");
	}

	private void showDrawerButton() throws PremiumModeException.WrongLayoutException{
		ViewGroup messageContainer = (ViewGroup) this.findViewById(this.getDrawerButtonViewGroupRes());
		if(messageContainer== null) {
			throw(new PremiumModeException.WrongLayoutException(true));
		}
		messageContainer.removeAllViews();
		View upgradeMessage ;
		if(this.mDrawerButtonView == null) {
			upgradeMessage = getLayoutInflater().inflate((R.layout.drawer_update_to_premium_default), messageContainer, false);
		} else {
			upgradeMessage = mDrawerButtonView;
		}
		if(upgradeMessage!=null) {
			messageContainer.addView(upgradeMessage);

			upgradeMessage.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View view) {
						onUpgrade();
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(this.mPremiumMenuButton) {
			menu.add(0, MENU_PREMIUM, 0, R.string.action_premium);
		}
		return true;
	}


	@Override
	public  boolean onOptionsItemSelected(MenuItem item) {
		Log.d("AdsFragmentActivity", "super.onOptionsItemSelected called!");
		if(this.mPremiumMenuButton && !isPremium()) {
			Log.d("AdsFragmentActivity", "and user not premium + premiummenubutton is on");

			switch (item.getItemId()) {
				case MENU_PREMIUM:
						onUpgrade();
					break;
			}
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Destroying helper.");
		if (mHelper != null) mHelper.dispose();
		mHelper = null;

	}

	// User clicked the "Upgrade to Premium" button.
	protected void onUpgrade() throws PremiumModeException.PremiumPackageIdError{
		if(mSkuPremium ==null) {
			throw new PremiumModeException.PremiumPackageIdError();
		}

		Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
		//TODO:		setWaitScreen(true);

		mHelper.launchPurchaseFlow(this, mSkuPremium, RC_REQUEST,
				mPurchaseFinishedListener, "");
	}



	// Callback for when a purchase is finished
	private final IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
			if (result.isFailure()) {
				Toast.makeText(AdsFragmentActivity.this, getString(R.string.premium_failed), Toast.LENGTH_LONG).show();
				return;
			}

			Log.d(TAG, "Purchase successful.");

			if (purchase.getSku().equals(mSkuPremium)) {
				// bought the premium upgrade!
				Toast.makeText(AdsFragmentActivity.this, getString(R.string.premium_thanks),Toast.LENGTH_LONG).show();
				//Change settings.
				updatePremium(true);
			}

		}
	};

	// Listener that's called when we finish querying the items and subscriptions we own
	private final IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {

		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			Log.d(TAG, "Query inventory finished.");
			if (result.isFailure()) {
				Log.e(TAG, "Failed to query inventory: " + result);
				return;
			}
			Log.d(TAG, "Query inventory was successful.");

			// Do we have the premium upgrade?
			Purchase premiumPurchase = inventory.getPurchase(mSkuPremium);
			mIsPremium = (premiumPurchase != null);
			Log.d(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM") + ". Updating prefs.");
			updatePremium(mIsPremium);
			Log.d(TAG, "Initial inventory query finished;");
		}
	};



	private void updatePremium(boolean isPremium) {
		this.mIsPremium=isPremium;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AdsFragmentActivity.this);
		prefs.edit().putBoolean(getString(R.string.SP_is_premium),isPremium).commit();
		if(isPremium) {
			ViewGroup adsContainer = (ViewGroup) this.findViewById(this.getAdsContainerRes());
			adsContainer.removeAllViews();
		}
	}

	private boolean getPremiumFromPrefs() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AdsFragmentActivity.this);
		return prefs.getBoolean(getString(R.string.SP_is_premium),false);
	}
	/**
	 * If you override onActivityResult, don't forget to call super.onActivityResult() at some point
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

		// Pass on the activity result to the helper for handling
		if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			super.onActivityResult(requestCode, resultCode, data);
		}
		else {
			Log.d(TAG, "onActivityResult handled by IABUtil.");
		}
	}



}