package com.magicmicky.freemiumlibrary;


import android.app.Activity;
import android.content.SharedPreferences;
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
public abstract class PremiumManager {

	private static final int MENU_PREMIUM = Menu.FIRST + 100;
	private static final String TAG = "UpdateToPremiumAct";
    //private final String developerPayLoad="";
	private IabHelper mHelper;
	private static final int RC_REQUEST = 10001;
	/*
	 * Drawer
	 */
	private boolean mDrawerButton =false;
	private int mDrawerButtonContainerRes;
	private View mDrawerButtonView;

	/*
	 * Ads
	 */
    private boolean mDoAds=false;
	private View mAdsReplacement=null;
	private int mAdsContainerRes;
	private String mAdUnitId;
    private boolean mUpgradeLinkOnFailure;


    /*
	 * MenuButton
	 */
	private boolean mPremiumMenuButton =false;
    private Menu mMenu;

	/*
	 * Other
	 */
	private boolean mInAppBillingSupported=true;
	private boolean mIsPremium;
	private boolean mIsPremiumInitialized=false;//To check if mIsPremium has been initialized
	private String mBase64EncodedPublicKey;
	private String mSkuPremium =null;
	private Set<String> mTestDevices;
    private final Activity mActivity;

    public PremiumManager(Activity activity, String premiumPackageId,String appPublicKey, String adId ) {
        this.mActivity = activity;
        this.mSkuPremium = premiumPackageId;
        this.mBase64EncodedPublicKey = appPublicKey;
        this.mAdUnitId = adId;

        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(mActivity, mBase64EncodedPublicKey);

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
                    PremiumManager.this.setInAppBillingNotSupported(true);
                    return;
                }

                // Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);

            }
        });


    }

	/**
	 * Is the user in premium mode?
	 * @return true if the user is a premium user, false otherwise
	 */
	public boolean isPremium() {
		if(!mIsPremiumInitialized) {
			this.mIsPremium = getPremiumFromPrefs();
		}
		return mIsPremium;
	}
	/**
	 * @return is in app billing supported for this device?
	 */
	public boolean isInAppBillingSupported() {
		return this.mInAppBillingSupported;
	}
	/**
	 * Whether or not you want to show ads for non premium user. Requires a viewgroup named "adView".
	 * @param adsViewGroupRes the viewgroup you want the ads in.
	 * @param upgradeLinkOnFailure whether or not you want to put a textview to replace the ad and link to the upgrade
	 */
	public void doAdsForNonPremium(int adsViewGroupRes, boolean upgradeLinkOnFailure) {
        this.mAdsContainerRes = adsViewGroupRes;
        this.mUpgradeLinkOnFailure = upgradeLinkOnFailure;
        ViewGroup adsContainer = (ViewGroup) mActivity.findViewById(adsViewGroupRes);

        if(!isPremium()) {
            Log.d(TAG, "user is not premium: instantiating adds");
            View adsReplacement=null;
            if (adsContainer == null) {
                throw (new PremiumModeException.WrongLayoutException(false));
            }
            adsContainer.removeAllViews();
            if (upgradeLinkOnFailure) {
                if(mAdsReplacement!=null) {
                    adsReplacement = mAdsReplacement;
                } else {
                    adsReplacement = mActivity.getLayoutInflater().inflate(R.layout.ads_replacement_default,adsContainer, false);
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
            //TODO: check if it's ok
            AdsInstantiator customAdsInstantiator = new AdsInstantiator(mActivity,mAdUnitId, adsReplacement, mTestDevices);
            adsContainer.setVisibility(View.VISIBLE);
            customAdsInstantiator.addAdsTo(adsContainer);
        } else {
            hideAdsContainer();
        }

	}
    //TODO: use res instead of view?
    public void doDrawerButotnForNonPremium(int drawerButtonViewGroupRes, View drawerButtonView) throws PremiumModeException.WrongLayoutException{
        this.mDrawerButton = true;
        this.mDrawerButtonContainerRes = drawerButtonViewGroupRes;
        this.mDrawerButtonView = drawerButtonView;
        if(!isPremium()) {
            ViewGroup messageContainer = (ViewGroup) mActivity.findViewById(mDrawerButtonContainerRes);
            if(messageContainer== null) {
                throw(new PremiumModeException.WrongLayoutException(true));
            }
            messageContainer.removeAllViews();
            View upgradeMessage ;
            if(this.mDrawerButtonView == null) {
                upgradeMessage = mActivity.getLayoutInflater().inflate((R.layout.drawer_update_to_premium_default), messageContainer, false);
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
        } else {
            hideDrawerButtonContainer();
        }
    }
    public void doPremiumButtonInMenu(Menu menu) {
        this.mPremiumMenuButton = true;
        this.mMenu = menu;
        if(!isPremium() && isInAppBillingSupported()) {
            MenuItem updateMenuItem = menu.add(0, MENU_PREMIUM, 0, R.string.action_premium);
            updateMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    onUpgrade();
                    return false;
                }
            });
        } else {
            menu.removeItem(MENU_PREMIUM);
        }
    }

    private void updatePremium(boolean isPremium) {
        this.mIsPremium=isPremium;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        prefs.edit().putBoolean(mActivity.getString(R.string.SP_is_premium),isPremium).commit();

        //	this.doStuff();
        if(this.mPremiumMenuButton) {
            doPremiumButtonInMenu(mMenu);
        }
        if(this.mDrawerButton) {
            doDrawerButotnForNonPremium(mDrawerButtonContainerRes, mDrawerButtonView);
        }
        if(this.mDoAds) {
            doAdsForNonPremium(mAdsContainerRes,mUpgradeLinkOnFailure);
        }
    }


     /*
	 * Sets the test devices for the ad
	 * @param testDevices a Set of the testdevices
	 */
	/*public void setTestDevice(Set<String> testDevices) {
		this.mTestDevices = testDevices;
	}*/



	private void setInAppBillingNotSupported(boolean inAppBillingNotSupported) {
		this.mInAppBillingSupported = !inAppBillingNotSupported;
	}


	private void hideAdsContainer() {
		mActivity.findViewById(this.mAdsContainerRes).setVisibility(View.GONE);
	}

	protected  void hideDrawerButtonContainer() {
        mActivity.findViewById(this.mDrawerButtonContainerRes).setVisibility(View.GONE);
	}

	public void clean() {
		Log.d(TAG, "Destroying helper.");
		if (mHelper != null) mHelper.dispose();
		mHelper = null;

	}

	//TODO: we should modify the helper so that it don't request an activity, and sends result in an asynctask instead of an onActivityResult
	protected void onUpgrade() throws PremiumModeException.PremiumPackageIdError{
		if(mSkuPremium ==null) {
			throw new PremiumModeException.PremiumPackageIdError();
		}

		Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
		mHelper.launchPurchaseFlow(mActivity, mSkuPremium, RC_REQUEST,
				mPurchaseFinishedListener, "");
	}



	// Callback for when a purchase is finished
	private final IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
			if (result.isFailure()) {
				Toast.makeText(mActivity, mActivity.getString(R.string.premium_failed), Toast.LENGTH_LONG).show();
				return;
			}

			Log.d(TAG, "Purchase successful.");

			if (purchase.getSku().equals(mSkuPremium)) {
				// bought the premium upgrade!
				Toast.makeText(mActivity, mActivity.getString(R.string.premium_thanks),Toast.LENGTH_LONG).show();
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




	private boolean getPremiumFromPrefs() {
		this.mIsPremiumInitialized=true;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
		return prefs.getBoolean(mActivity.getString(R.string.SP_is_premium),false);
	}
	/**
	 * If you override onActivityResult, don't forget to call super.onActivityResult() at some point
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	/*@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

		// Pass on the activity result to the helper for handling
		if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
            mActivity.onActivityResult(requestCode, resultCode, data);
		}
		else {
			Log.d(TAG, "onActivityResult handled by IABUtil.");
		}
	}*/

}