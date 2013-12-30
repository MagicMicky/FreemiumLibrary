package com.magicmicky.freemiumlibrary.sample;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import com.magicmicky.freemiumlibrary.PremiumManager;

import java.util.HashSet;

public class MainActivity extends ActionBarActivity {
    private static PremiumManager mPremiumManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //We instantiate our PremiumManager with our premium package, app's public key adId and testDevices
        mPremiumManager = new PremiumManager(this,"premium","pubKey","adId",new HashSet<String>());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        //We can set up ads.
        mPremiumManager.doAdsForNonPremium(R.id.adView, true, R.layout.ads_replacement_default);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /*
    We make sure that onActivityResult delegate some actions to the PremiumManager
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(!(mPremiumManager != null && mPremiumManager.handleResult(requestCode, resultCode, intent))) {
            //Handle your own results...
        }
    }
    /*
    We can set up an upgrade menu button
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(this.mPremiumManager!=null)
            this.mPremiumManager.doMenuButtonForNonPremium(menu, getString(R.string.action_premium));
        return super.onPrepareOptionsMenu(menu);
    }

    /*
    And me clean our PremiumManager when we destroy the activity.
     */
    protected void onDestroy() {
        super.onDestroy();
        if(mPremiumManager != null)
            mPremiumManager.clean();
    }
    /**
     * A placeholder fragment containing a simple view.
     */
    static public class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onStart() {
            super.onStart();
            //And in the onStart of our Fragment we can set up an upgrade button inside our framgent.
            mPremiumManager.doUpgradeButtonForNonPremium(R.id.upgrade_button, R.layout.upgrade_to_premium_default);

        }
    }

}
