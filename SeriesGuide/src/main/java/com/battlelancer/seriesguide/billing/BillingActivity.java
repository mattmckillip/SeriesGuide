
/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.billing;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.Utils;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

public class BillingActivity extends BaseActivity {

    public static final String TAG = "BillingActivity";

    // The SKU product ids as set in the Developer Console
    public static final String SKU_X = "x_upgrade";

    public static final String SKU_X_SUB = "x_sub_2014_02";

    public static final String SKU_X_SUB_LEGACY = "x_subscription";

    // (arbitrary) request code for the purchase flow
    private static final int RC_REQUEST = 749758;

    private static final String SOME_STRING = "SURPTk9UQ0FSRUlGWU9VUElSQVRFVEhJUw==";

    private IabHelper billingHelper;

    private View mProgressScreen;

    private View mContentContainer;

    private Button mButtonSub;

    private Button mButtonPass;

    private TextView mTextViewPriceSub;

    private String mSubPrice;

    private View mTextHasUpgrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);
        setupActionBar();

        setupViews();
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupViews() {
        mButtonSub = (Button) findViewById(R.id.buttonBillingGetSubscription);
        mButtonSub.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubscribeToXButtonClicked();
            }
        });
        mTextViewPriceSub = (TextView) findViewById(R.id.textViewBillingPriceSubscription);

        mButtonPass = (Button) findViewById(R.id.buttonBillingGetPass);
        mButtonPass.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.launchWebsite(BillingActivity.this, getString(R.string.url_x_pass), TAG,
                        "X Pass");
            }
        });

        mTextHasUpgrade = findViewById(R.id.textViewBillingExisting);

        mProgressScreen = findViewById(R.id.progressBarBilling);
        mContentContainer = findViewById(R.id.containerBilling);

        findViewById(R.id.textViewBillingMoreInfo).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.launchWebsite(BillingActivity.this, getString(R.string.url_whypay), TAG,
                        "WhyPayWebsite");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.d("onActivityResult(" + requestCode + "," + resultCode + "," + data);
        // Have we been disposed of in the meantime? If so, quit.
        if (billingHelper == null) {
            return;
        }

        // Pass on the activity result to the helper for handling
        if (!billingHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Timber.d("onActivityResult handled by IABUtil.");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // do not query IAB if user has key
        boolean hasUpgrade = Utils.hasXpass(this);
        updateViewStates(hasUpgrade);
        if (hasUpgrade) {
            setWaitMode(false);
        } else {
            setWaitMode(true);

            billingHelper = new IabHelper(this);
            billingHelper.startSetup(billingSetupListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (billingHelper != null) {
            billingHelper.dispose();
            billingHelper = null;
        }
    }

    private IabHelper.OnIabSetupFinishedListener billingSetupListener
            = new IabHelper.OnIabSetupFinishedListener() {
        public void onIabSetupFinished(IabResult result) {
            if (billingHelper == null) {
                // disposed
                return;
            }

            if (!result.isSuccess()) {
                logAndShowAlertDialog(R.string.subscription_unavailable,
                        "Problem setting up In-app Billing: " + result.getMessage());
                enableFallBackMode();
                setWaitMode(false);
                return;
            }

            Timber.d("onIabSetupFinished: Successful. Querying inventory.");
            List<String> detailSkus = new ArrayList<>();
            detailSkus.add(SKU_X_SUB);
            billingHelper.queryInventoryAsync(true, detailSkus, queryInventoryFinishedListener);
        }
    };

    // Listener that's called when we finish querying the items and
    // subscriptions we own
    IabHelper.QueryInventoryFinishedListener queryInventoryFinishedListener
            = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (billingHelper == null) {
                // disposed
                return;
            }

            if (result.isFailure()) {
                logAndShowAlertDialog(R.string.subscription_unavailable,
                        "Could not query inventory: " + result.getMessage());
                return;
            }

            // get sub state
            boolean hasUpgrade = checkForSubscription(BillingActivity.this, inventory);
            // get local sub price
            SkuDetails skuDetails = inventory.getSkuDetails(SKU_X_SUB);
            if (skuDetails != null) {
                mSubPrice = skuDetails.getPrice();
            }

            updateViewStates(hasUpgrade);
            setWaitMode(false);
        }
    };

    /**
     * Checks if the user is subscribed to X features or has the deprecated X upgrade (so he gets
     * the subscription for life). Also sets the current state through {@link
     * AdvancedSettings#setSupporterState(Context, boolean)}.
     */
    public static boolean checkForSubscription(@NonNull Context context,
            @NonNull Inventory inventory) {
        /*
         * Check for items we own. Notice that for each purchase, we check the
         * developer payload to see if it's correct! See
         * verifyDeveloperPayload().
         */

        // Does the user have the deprecated X Upgrade in-app purchase? If so unlock all features.
        Purchase premiumPurchase = inventory.getPurchase(SKU_X);
        boolean hasXUpgrade = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));

        // Does the user have an active unlock all subscription?
        Purchase xSubLegacy = inventory.getPurchase(SKU_X_SUB_LEGACY);
        Purchase xSub = inventory.getPurchase(SKU_X_SUB);
        boolean isSubscribedToX = (xSubLegacy != null && verifyDeveloperPayload(xSubLegacy))
                || (xSub != null && verifyDeveloperPayload(xSub));

        if (hasXUpgrade) {
            Timber.d("User has X SUBSCRIPTION for life.");
        } else {
            Timber.d("User has " + (isSubscribedToX ? "X SUBSCRIPTION" : "NO X SUBSCRIPTION"));
        }

        // notify the user about a change in subscription state
        boolean isSubscribedOld = AdvancedSettings.getLastSupporterState(context);
        boolean isSubscribed = hasXUpgrade || isSubscribedToX;
        if (!isSubscribedOld && isSubscribed) {
            Toast.makeText(context, R.string.upgrade_success, Toast.LENGTH_SHORT).show();
        } else if (isSubscribedOld && !isSubscribed) {
            onExpiredNotification(context);
        }

        // Save current state until we query again
        AdvancedSettings.setSupporterState(context, isSubscribed);

        return isSubscribed;
    }

    /**
     * Displays a notification that the subscription has expired. Its action opens {@link
     * BillingActivity}.
     */
    public static void onExpiredNotification(Context context) {
        NotificationCompat.Builder nb = new NotificationCompat.Builder(context);

        // set required attributes
        nb.setSmallIcon(R.drawable.ic_notification);
        nb.setContentTitle(context.getString(R.string.subscription_expired));
        nb.setContentText(context.getString(R.string.subscription_expired_details));

        // set additional attributes
        nb.setDefaults(Notification.DEFAULT_LIGHTS);
        nb.setAutoCancel(true);
        nb.setTicker(context.getString(R.string.subscription_expired_details));
        nb.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // build task stack
        Intent notificationIntent = new Intent(context, BillingActivity.class);
        PendingIntent contentIntent = TaskStackBuilder
                .create(context)
                .addNextIntent(new Intent(context, ShowsActivity.class))
                .addNextIntent(new Intent(context, SeriesGuidePreferences.class))
                .addNextIntent(notificationIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        nb.setContentIntent(contentIntent);

        // build the notification
        Notification notification = nb.build();

        // show the notification
        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(SeriesGuideApplication.NOTIFICATION_SUBSCRIPTION_ID, notification);
    }

    /**
     * Verifies the developer payload of a purchase.
     */
    public static boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * Not doing anything sophisticated here,
         * this is open source anyhow.
         */

        return SOME_STRING.equals(payload);
    }

    // User clicked the "Subscribe" button.
    private void onSubscribeToXButtonClicked() {
        Timber.d("Subscribe button clicked; launching purchase flow for X subscription.");

        String payload = SOME_STRING;

        setWaitMode(true);

        billingHelper.launchSubscriptionPurchaseFlow(this, SKU_X_SUB, RC_REQUEST,
                purchaseFinishedListener, payload);
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener
            = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Timber.d("Purchase finished: " + result + ", purchase: " + purchase);

            // Have we been disposed of in the meantime? If so, quit.
            if (billingHelper == null) {
                return;
            }

            if (result.isFailure()) {
                if (result.getResponse() != IabHelper.IABHELPER_USER_CANCELLED) {
                    logAndShowAlertDialog(R.string.subscription_failed, result.getMessage());
                }
                setWaitMode(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                logAndShowAlertDialog(R.string.subscription_failed,
                        "Authenticity verification failed.");
                setWaitMode(false);
                return;
            }

            Timber.d("Purchase successful.");

            if (purchase.getSku().equals(SKU_X_SUB)) {
                Timber.d("Purchased X subscription. Congratulating user.");
                // Save current state until we query again
                AdvancedSettings.setSupporterState(BillingActivity.this, true);
                updateViewStates(true);
                setWaitMode(false);
            }
        }
    };

    private void updateViewStates(boolean hasUpgrade) {
        // Only enable purchase button if the user does not have the upgrade yet
        mButtonSub.setEnabled(!hasUpgrade);
        mTextViewPriceSub.setText(
                getString(R.string.billing_price_subscribe, mSubPrice != null ? mSubPrice : "--"));
        mButtonPass.setEnabled(!hasUpgrade);
        mTextHasUpgrade.setVisibility(hasUpgrade ? View.VISIBLE : View.GONE);
    }

    /**
     * Disables the purchase button and hides the subscribed message.
     */
    private void enableFallBackMode() {
        mButtonSub.setEnabled(false);
        mButtonPass.setEnabled(true);
        mTextHasUpgrade.setVisibility(View.GONE);
    }

    private void setWaitMode(boolean isActive) {
        mProgressScreen.setVisibility(isActive ? View.VISIBLE : View.GONE);
        mContentContainer.setVisibility(isActive ? View.GONE : View.VISIBLE);
    }

    private void logAndShowAlertDialog(int errorResId, String message) {
        Timber.e(message);

        new AlertDialog.Builder(this)
                .setMessage(getString(errorResId) + "\n\n" + message)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }
}
