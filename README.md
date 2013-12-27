FreemiumLibary
==============

The Android Freemium Library is a library that aims to help you put up a freemium model within your android application

##What is a Freemium Business Model?
What I mean by Freemium Business Model is to propose users to use features from your apps for free,
but will have to pay to use advanced features. They can also be shown ads when they are not premium
- it's up to you.

##How does this library helps?
This library implements and simplifies multiple functionalities that would be useful to you.
It implements the **in-app billing v3** used to charge user via the Google Play Store.
It also implements the **AdMob** library that can be used to show ads to the user when he is not premium.


##How to use the library?
To use the library you need to create a class instance of a PremiumManager. It's advised to do so in the `onCreate` or `onResume()` call of your activity.

        this.mPremiumManager = new PremiumManager(this,getString(R.string.premium_sku),getString(R.string.appPublicKey), getString(R.string.adKey), testDevices);

The PremiumManager constructor will take a few arguments: the premium "sku" id of your Google Play upgrade package.
Your app Public Key that you can find on the Google Play store [1].
Your AdMob key, so that it can show ads to the user, and a list of test devices for AdMob.

To catch the return intent of the in-app payment, you will also need to implement the onActivityResult of your application and call the handleResult of your PremiumManager.

    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent intent) {
        if(!(mPremiumManager != null && mPremiumManager.handleResult(requestCode, resultCode, intent))) {
            Log.v(TAG, "Activity result not handled by PremiumManager");
            //Handle your own results...
        }
    }

Finally, you'll have to clean the PremiumManager in your activity's onDestroy call.

	protected void onDestroy() {
		Crouton.cancelAllCroutons();
		super.onDestroy();
        if(mPremiumManager != null)
            mPremiumManager.clean();
	}


Once your premium manager and your activity are set up, you will be able to select the features you want to use.

### Showing Ads when the user is not premium
To show ads for a non-premium user, you just need to call the PremiumManager's `doAdsForNonPremium()` method.

	    this.mPremiumManager.doAdsForNonPremium(R.id.adView, true, R.layout.ads_replacement_default);

The method doAdsForNonPremium will take as arguments:

* The ViewGroup that should contain the ad. Be sure that the ad will fit in this viewgroup and check the LogCat if you don't see it.
* Whether or not you want an upgrade link when the ads can not be selected (i.e.: the user has no internet, or is using AdBlock)
* And finally, if the previous argument was set to "true" the replacement layout that should be inflated.


### Showing an upgrade button in your layout (i.e. in the drawer)
To show an upgrade button on your application, you just need to call the method `doUpgradeButtonForNonPremium`.

    this.mPremiumManager.doUpgradeButtonForNonPremium(R.id.update_to_premium, R.layout.drawer_update_to_premium_default);

This method takes the following arguments

* The ViewGroup that will contain the premium button.
* The layout to inflate in this viewgroup container.

### Showing an upgrade button in the Menu
You can also show an upgrade button in the menu. It will be available on pre and post honeycomb menu style.
You just need to call `doPremiumButtonInMenu` in your Activity's onPrepareOptionsMenu

```java
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(this.mPremiumManager!=null)
            this.mPremiumManager.doPremiumButtonInMenu(menu, R.string.action_premium);
        return super.onPrepareOptionsMenu(menu);
    }
```

This method will require the following arguments:
* The Menu where the new item should be created
* The String you want to show for the user to upgrade. (be sure to check the String's length!)


## More advanced use.
You also have access to other methods that could be useful in other cases.

`public boolean isPremium()` will tell you if the user is premium or not. Thanks to it, you will be able to select features you want to activate only for premium users.

`public static boolean getPremiumFromPrefs(Context c)` will let you get the premium information for the preferences.
It could be useful to use it in a widget or somewere you don't want to instancite a PremiumManager.

Finally, `public boolean isInAppBillingSupported()` will tell you whether or not the user's device support InAppBilling.

##Use case.
The library is currently used in my HabitRPG application. You can find it on the [Google Play Store](https://play.google.com/store/apps/details?id=com.magicmicky.habitrpgmobileapp)

##Developped by
* Mickael Goubin - [@MagicMicky](http://twitter.com/MagicMicky)


##License
>Copyright 2014 Mickael Goubin
>
>Licensed under the Apache License, Version 2.0 (the "License");
>you may not use this file except in compliance with the License.
>You may obtain a copy of the License at
>
>   http://www.apache.org/licenses/LICENSE-2.0
>
>Unless required by applicable law or agreed to in writing, software
>distributed under the License is distributed on an "AS IS" BASIS,
>WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
>See the License for the specific language governing permissions and
>limitations under the License.