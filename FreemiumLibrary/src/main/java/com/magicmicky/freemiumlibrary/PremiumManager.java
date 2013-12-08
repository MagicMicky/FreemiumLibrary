package com.magicmicky.freemiumlibrary;


import android.app.Activity;
import android.content.Intent;
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
public class PremiumManager {

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
	private int mdrawerButtonLayoutReference;

	/*
	 * Ads
	 */
    private boolean mDoAds=false;
    private int adsReplacementLayoutRes;
	private int mAdsContainerRes;
	private String mAdUnitId;
    private boolean mUpgradeLinkOnFailure;


    /*
	 * MenuButton
	 */
	private boolean mPremiumMenuButton =false;
    private Menu mMenu;
    private String mMenuButtonTextResource;

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

    /**
     * Instantiate a PremiumManager.
     * @param activity the activity you start the instantiator from
     * @param premiumPackageId your app's premium package "Sku" id
     * @param appPublicKey your app's publicKey
     * @param adId your adId
     * @param testDevices a Set of your test devices ids
     */
    public PremiumManager(Activity activity, String premiumPackageId,String appPublicKey, String adId, Set<String> testDevices) {
        this.mActivity = activity;
        this.mSkuPremium = premiumPackageId;
        this.mBase64EncodedPublicKey = appPublicKey;
        this.mAdUnitId = adId;
        this.mTestDevices = testDevices;

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
     * Show ads for non-premium users.
     * @param adsViewGroupRes the Res reference to the container of your ads.
     * @param upgradeLinkOnFailure whether or not you want to have an "upgrade" link if the ad requests fail (i.e. the user has an ad blocker, or don't have internet)
     * @param adsReplacementLayoutRes the resources to the layout you want to use when you can't show ads
     */
	public void doAdsForNonPremium(int adsViewGroupRes, boolean upgradeLinkOnFailure, int adsReplacementLayoutRes) {
        this.mDoAds = true;
        this.mAdsContainerRes = adsViewGroupRes;
        this.mUpgradeLinkOnFailure = upgradeLinkOnFailure;
        this.adsReplacementLayoutRes = adsReplacementLayoutRes;
        ViewGroup adsContainer = (ViewGroup) mActivity.findViewById(adsViewGroupRes);
        if(!isPremium()) {
            Log.d(TAG, "user is not premium: instantiating adds");
            View adsReplacement=null;
            if (adsContainer == null) {
                throw (new PremiumModeException.WrongLayoutException(false));
            }
            adsContainer.removeAllViews();
            if (upgradeLinkOnFailure) {
                try {
                    adsReplacement = mActivity.getLayoutInflater().inflate(adsReplacementLayoutRes, adsContainer, true);
                } catch(InflateException e) {
                    Log.w(TAG, "Catched Exception while trying to get layout " + adsReplacementLayoutRes + ": "+ e.getMessage());
                    e.printStackTrace();
                }
                if(adsReplacement==null) {
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
            AdsInstantiator customAdsInstantiator = new AdsInstantiator(mActivity,mAdUnitId, adsReplacement, mTestDevices);
            adsContainer.setVisibility(View.VISIBLE);
            customAdsInstantiator.addAdsTo(adsContainer);
        } else {
            hideAdsContainer();
        }

	}

    /**
     * Show a "Update to Premium" button for non premium users
     * @param drawerButtonViewGroupRes the Resource Layout for the button container
     * @param drawerButtonLayoutReference the Resource Layout for the button. Will be ignored if you want default look.
     * @throws PremiumModeException.WrongLayoutException
     */
    public void doUpdateButtonForNonPremium(int drawerButtonViewGroupRes, int drawerButtonLayoutReference) throws PremiumModeException.WrongLayoutException{
        this.mDrawerButton = true;
        this.mDrawerButtonContainerRes = drawerButtonViewGroupRes;
        this.mdrawerButtonLayoutReference = drawerButtonLayoutReference;
        if(!isPremium()) {
            ViewGroup messageContainer = (ViewGroup) mActivity.findViewById(mDrawerButtonContainerRes);
            if(messageContainer== null) {
                throw(new PremiumModeException.WrongLayoutException(true));
            }
            messageContainer.removeAllViews();
            View upgradeMessage =null;
            try {
                upgradeMessage = mActivity.getLayoutInflater().inflate(drawerButtonLayoutReference, messageContainer, true);

            } catch(InflateException e) {
                Log.w(TAG, "Catched Exception while trying to get layout " + adsReplacementLayoutRes + ": "+ e.getMessage());
                e.printStackTrace();
            }
            if(upgradeMessage==null) {
                upgradeMessage = mActivity.getLayoutInflater().inflate(R.layout.drawer_update_to_premium_default, messageContainer, false);
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
            hideUpgradeButtonContainer();
        }
    }

    /**
     * Show an "Upgrade to premium" button in the menu.
     * @param menu the menu instance to modify.
     * @param menuButtonText the text to show in the menu.
     */
    public void doPremiumButtonInMenu(Menu menu, String menuButtonText) {
        this.mPremiumMenuButton = true;
        this.mMenu = menu;
        this.mMenuButtonTextResource = menuButtonText;
        if(!isPremium() && isInAppBillingSupported()) {
            MenuItem updateMenuItem;
            if(menuButtonText!=null) {
                updateMenuItem = menu.add(0, MENU_PREMIUM, 0, menuButtonText);
            } else {
                updateMenuItem= menu.add(0, MENU_PREMIUM, 0, R.string.action_premium);
            }

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

    /**
     * Clean everything. Should be called in your activities onDestroy()
     */
    public void clean() {
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) mHelper.dispose();
        mHelper = null;

    }

    /**
     * Upgrade the user to premium, or downgrade him to not premium.
     * @param isPremium whether or not the user is premium
     */
    private void updatePremium(boolean isPremium) {
        this.mIsPremium=isPremium;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        prefs.edit().putBoolean(mActivity.getString(R.string.SP_is_premium),isPremium).commit();

        //	this.doStuff();
        if(this.mPremiumMenuButton) {
            doPremiumButtonInMenu(mMenu, mMenuButtonTextResource);
        }
        if(this.mDrawerButton) {
            doUpdateButtonForNonPremium(mDrawerButtonContainerRes, mdrawerButtonLayoutReference);
        }
        if(this.mDoAds) {
            doAdsForNonPremium(mAdsContainerRes,mUpgradeLinkOnFailure, adsReplacementLayoutRes);
        }
    }


    /**
     * set whether or not in app billing is supported
     * @param inAppBillingNotSupported whether or not in app billing is supported
     */
	private void setInAppBillingNotSupported(boolean inAppBillingNotSupported) {
		this.mInAppBillingSupported = !inAppBillingNotSupported;
	}

    /**
     * Hide the ads container
     */
	private void hideAdsContainer() {
		mActivity.findViewById(this.mAdsContainerRes).setVisibility(View.GONE);
	}

    /**
     * Hide the Upgrade button container
     */
	protected  void hideUpgradeButtonContainer() {
        mActivity.findViewById(this.mDrawerButtonContainerRes).setVisibility(View.GONE);
	}

    /**
     * Launch the Upgrade workflow
     * @throws PremiumModeException.PremiumPackageIdError
     */
	protected void onUpgrade() throws PremiumModeException.PremiumPackageIdError{
		if(mSkuPremium ==null) {
			throw new PremiumModeException.PremiumPackageIdError();
		}

		Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
		mHelper.launchPurchaseFlow(mActivity, mSkuPremium, RC_REQUEST,
				mPurchaseFinishedListener, "");
	}


    /**
     * Callback when a purchase is finished.
     */
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

    /**
     * Listener that's called when we finish querying the items and subscriptions we own
     */
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


    /**
     * Retrieve whether or not the user is premium from the preferences
     * @return whether or not the user is premium
     */
	private boolean getPremiumFromPrefs() {
		this.mIsPremiumInitialized=true;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
		return prefs.getBoolean(mActivity.getString(R.string.SP_is_premium),false);
	}

	/**
     * This method needs to be called from your onActivityResult so that the transaction can happen.
     * @param requestCode the requestCode from Activity#onActivityResult
	 * @param resultCode the resultCode from Activity#onActivityResult
	 * @param data the data from Activity#onActivityResult
     * @return Returns true if the result was related to a purchase flow and was handled; false if the result was not related to a purchase, in which case you should handle it normally.
	 */
	public boolean handleResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

		// Pass on the activity result to the helper for handling
        // not handled, so handle it ourselves (here's where you'd
        // perform any handling of activity results not related to in-app
        // billing...
        //mActivity.onActivityResult(requestCode, resultCode, data);
        return !mHelper.handleActivityResult(requestCode, resultCode, data);
	}

}