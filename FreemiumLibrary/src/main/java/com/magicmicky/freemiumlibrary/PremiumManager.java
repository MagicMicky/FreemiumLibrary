package com.magicmicky.freemiumlibrary;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
 * This class allows you to control the Premium features inside your application. You can set up
 * Ads for non premium, and show different "upgrade" buttons. You also have access to the current status of the user.
 * @author Mickael Goubin
 */
public class PremiumManager {

	private static final int MENU_PREMIUM = Menu.FIRST + 100;
	private static final String TAG = "FreemiumLibrary/PremiumManager";
	//private final String developerPayLoad="";
	private IabHelper mHelper;
	private static final int RC_REQUEST = 11337;


	/*
	 * Drawer
	 */
	private boolean mUpgradeButton =false;
	private int mUpgradeButtonContainerRes;
	private int mUpgradeButtonLayoutReference;

	/*
	 * Ads
	 */
	private boolean mDoAds=false;
	private int mAdsReplacementLayoutRes;
	private int mAdsContainerRes;
	private String mAdUnitId;
	private boolean mUpgradeLinkOnFailure;


	/*
	 * MenuButton
	 */
	private boolean mPremiumMenuButton =false;
	private Menu mMenu;
	private String mMenuButtonText;

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
	 * Check if the user is premium or not.
	 * @return true if the user is a premium user, false otherwise
	 */
	public boolean isPremium() {
		if(!mIsPremiumInitialized) {
			this.mIsPremium = getPremiumFromPrefs();
		}
		return mIsPremium;
	}
	/**
     * Check if in-app billing is supported for this device.
	 * @return is in app billing supported for this device?
	 */
	public boolean isInAppBillingSupported() {
		return this.mInAppBillingSupported;
	}

    /**
     * Show ads for non-premium users.
     * @param adsViewGroupRes the Res reference to the container of your ads.
     */
    public void doAdsForNonPremium(int adsViewGroupRes) {
        this.doAdsForNonPremium(adsViewGroupRes,false,null);
    }


	/**
	 * Show ads for non-premium users.
	 * @param adsViewGroupRes the Res reference to the container of your ads.
	 * @param upgradeLinkOnFailure whether or not you want to have an "upgrade" link if the ad requests fail (i.e. the user has an ad blocker, or don't have internet)
	 * @param adsReplacementLayoutRes the resources linking to the layout you want to use when you can't show ads. Put null to use the default one.
	 */
	public void doAdsForNonPremium(int adsViewGroupRes, boolean upgradeLinkOnFailure, Integer adsReplacementLayoutRes) throws PremiumModeException {
        if(this.mActivity.findViewById(adsViewGroupRes)==null)
            throw new PremiumModeException.ViewNotFoundException();
        if(upgradeLinkOnFailure && this.mActivity.getResources().getLayout(adsReplacementLayoutRes)==null)
            throw new PremiumModeException.WrongLayoutException();

        Log.v(TAG + "_ads", "Editing ads info.");
        this.mDoAds = true;
		this.mAdsContainerRes = adsViewGroupRes;
		this.mUpgradeLinkOnFailure = upgradeLinkOnFailure;
		this.mAdsReplacementLayoutRes = adsReplacementLayoutRes;
        this.showAdsForNonPremium();
	}

	/**
	 * Show a "Upgrade to Premium" button for non premium users
	 * @param upgradeButtonViewGroupRes the Resource Layout for the button container
	 * @param upgradeButtonLayoutReference the Resource Layout for the button. Will be ignored if you want default look.
	 * @throws PremiumModeException.WrongLayoutException
	 */
	public void doUpgradeButtonForNonPremium(int upgradeButtonViewGroupRes, int upgradeButtonLayoutReference) throws PremiumModeException {
        if(this.mActivity.findViewById(upgradeButtonViewGroupRes)==null)
            throw new PremiumModeException.ViewNotFoundException();
        if(this.mActivity.getResources().getLayout(upgradeButtonLayoutReference)==null)
            throw new PremiumModeException.WrongLayoutException();
        Log.v(TAG + "_upgradeButton", "Editing info about the upgrade normal button.");
		this.mUpgradeButton = true;
		this.mUpgradeButtonContainerRes = upgradeButtonViewGroupRes;
		this.mUpgradeButtonLayoutReference = upgradeButtonLayoutReference;
        showUpgradeButtonForNonPremium();
	}

	/**
	 * Show an "Upgrade to premium" button in the menu.
	 * @param menu the menu instance to modify.
	 * @param menuButtonText the text to show in the menu. 0 for default value
	 */
	public void doMenuButtonForNonPremium(Menu menu, String menuButtonText) {
        Log.v(TAG+"_menuButton", "Editing upgrade menu button info.");
		this.mPremiumMenuButton = true;
		this.mMenu = menu;
        this.mMenuButtonText = menuButtonText;//TODO: the user should be able to customize more than just the text.
        this.showPremiumButtonInMenu();
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

    /**
     * Retrieve whether or not the user is premium from the preferences
     * @param c the current context
     * @return whether or not the user is premium
     */
    public static boolean getPremiumFromPrefs(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getBoolean(c.getString(R.string.SP_is_premium),false);
    }

    /**
     * Display the ads configured by {@code doAdsForNonPremium()} or hide the container if the user is premium
     * @see com.magicmicky.freemiumlibrary.PremiumManager#doAdsForNonPremium(int, boolean, Integer)
     */
    private void showAdsForNonPremium() {
        ViewGroup adsContainer = (ViewGroup) mActivity.findViewById(mAdsContainerRes);
        if(!isPremium()) {
            Log.d(TAG + "_ads", "Showing ads.");
            View adsReplacement=null;
            adsContainer.removeAllViews();
            adsContainer.invalidate();
            if (mUpgradeLinkOnFailure) {
                adsReplacement = mActivity.getLayoutInflater().inflate(mAdsReplacementLayoutRes, adsContainer, false);
                adsContainer.removeAllViews();
                adsContainer.addView(adsReplacement);
                adsReplacement.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                    onUpgrade();
                    }
                });
            }
            AdsInstantiator customAdsInstantiator = new AdsInstantiator(mActivity,mAdUnitId, adsReplacement, mTestDevices);
            adsContainer.setVisibility(View.VISIBLE);
            customAdsInstantiator.addAdsTo(adsContainer);
        } else {
            Log.d(TAG + "_ads", "Hiding ads container.");
            hideAdsContainer();
        }


    }

    /**
     * Display or not the configured Upgrade button of the user.
     * @see #doUpgradeButtonForNonPremium(int, int)
     */
    private void showUpgradeButtonForNonPremium() {
        if(!isPremium() && isInAppBillingSupported()) {
            Log.d(TAG + "_upgradeButton", "Showing upgrade normal button.");
            ViewGroup messageContainer = (ViewGroup) mActivity.findViewById(mUpgradeButtonContainerRes);
            messageContainer.removeAllViews();
            View upgradeMessage =null;
            upgradeMessage = mActivity.getLayoutInflater().inflate(mUpgradeButtonLayoutReference, messageContainer, false);
            messageContainer.addView(upgradeMessage);
            upgradeMessage.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    onUpgrade();
                }
            });
        } else {
            Log.d(TAG + "_upgradeButton", "Hiding upgrade normal button.");
            hideUpgradeButtonContainer();
        }

    }

    /**
     * Display a menu premium asking the user to upgrade to premium. The menu button should be set up with {@code doPremiumButtonInMenu}
     * @see #doMenuButtonForNonPremium
     */
    private void showPremiumButtonInMenu() {
        if(!isPremium() && isInAppBillingSupported()) {
            Log.d(TAG+"_menuButton","Showing upgrade menu button.");
            if(this.mMenu.findItem(MENU_PREMIUM)==null) {
                MenuItem updateMenuItem;
                if(mMenuButtonText!=null) {
                    updateMenuItem = this.mMenu.add(0, MENU_PREMIUM, 0, this.mMenuButtonText);
                } else {
                    updateMenuItem= this.mMenu.add(0, MENU_PREMIUM, 0, R.string.action_premium);
                }

                updateMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        onUpgrade();
                        return false;
                    }
                });
            }
        } else {
            Log.d(TAG+"_menuButton","Removing upgrade menu button");
            mMenu.removeItem(MENU_PREMIUM);
        }
    }

	/**
     * This is the method called after the user upgraded, or when we have a result from the
     * inventory query result of the user.
	 * It modifies the premium status of the user, save it, and display or not the ads/premium upgrade link/...
	 * @param isPremium whether or not the user is premium
	 */
	private void updatePremium(boolean isPremium) {
        //If the user's status changed, we need to hide/show the different stuff of the user.
        if(this.mIsPremium != isPremium) {
            Log.v(TAG+"_updatePrefs", "Updating prefs: user is " + (isPremium ? "" : "not") + " Premium");
            this.mIsPremiumInitialized=true;
            this.mIsPremium=isPremium;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
            prefs.edit().putBoolean(mActivity.getString(R.string.SP_is_premium),isPremium).commit();

            if(this.mPremiumMenuButton)
                showPremiumButtonInMenu();
            if(this.mUpgradeButton)
                showUpgradeButtonForNonPremium();
            if(this.mDoAds)
                showAdsForNonPremium();
        } else {
            this.mIsPremiumInitialized=true;
            this.mIsPremium=isPremium;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
            prefs.edit().putBoolean(mActivity.getString(R.string.SP_is_premium),isPremium).commit();
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
		mActivity.findViewById(this.mUpgradeButtonContainerRes).setVisibility(View.GONE);
	}

	/**
	 * Launch the Upgrade workflow
	 * @throws PremiumModeException.PremiumPackageIdError
	 */
    protected void onUpgrade() throws PremiumModeException.PremiumPackageIdError{
        if(this.isInAppBillingSupported()) {
            if(mSkuPremium ==null) {
                throw new PremiumModeException.PremiumPackageIdError();
            }

            Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
            try {
                mHelper.launchPurchaseFlow(mActivity, mSkuPremium, RC_REQUEST,
                        mPurchaseFinishedListener, "");
            } catch (IllegalStateException e) {
                if(e.getMessage().startsWith("Can't start async operation") && e.getMessage().endsWith("is in progress.")) {
                    Log.d(TAG, "the helper seemed to be doing some work in background. Stopping it and launching the purchase flow again");
                    mHelper.flagEndAsync();
                    mHelper.launchPurchaseFlow(mActivity, mSkuPremium, RC_REQUEST,
                            mPurchaseFinishedListener, "");

                } else {
                    throw e;
                }
            }
        }
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
	 * Listener that is called when we finish querying the items and subscriptions we own
	 */
	private final IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			Log.d(TAG, "Query inventory finished.");
			if (result.isFailure()) {
				Log.e(TAG, "Failed to query inventory: " + result);
				return;
			}
			Log.d(TAG, "Query inventory was successful.");
            if(isNetworkAvailable()) {
                // Do we have the premium upgrade?
                Purchase premiumPurchase = inventory.getPurchase(mSkuPremium);
                boolean isPremium = (premiumPurchase != null);
                Log.d(TAG, "User is " + (isPremium ? "PREMIUM" : "NOT PREMIUM") + ". Updating prefs.");
                updatePremium(isPremium);
                Log.d(TAG, "Initial inventory query finished;");
            } else {
                Log.d(TAG, "Network not available. No update of the user status");
            }
        }
	};


	/**
	 * Retrieve whether or not the user is premium from the preferences
	 * @return whether or not the user is premium
	 */
	private boolean getPremiumFromPrefs() {
		this.mIsPremiumInitialized=true;
		return getPremiumFromPrefs(mActivity);
	}


    /**
     * Checks if the network is available.
     * @return the network availability
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ninfo = cm.getActiveNetworkInfo();
        return ninfo != null && ninfo.isConnected();
    }
}