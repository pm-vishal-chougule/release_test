/*
 * PubMatic Inc. ("PubMatic") CONFIDENTIAL
 * Unpublished Copyright (c) 2006-2019 PubMatic, All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of PubMatic. The intellectual and technical concepts contained
 * herein are proprietary to PubMatic and may be covered by U.S. and Foreign Patents, patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is obtained
 * from PubMatic.  Access to the source code contained herein is hereby forbidden to anyone except current PubMatic employees, managers or contractors who have executed
 * Confidentiality and Non-disclosure agreements explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  PubMatic.   ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT  THE EXPRESS WRITTEN CONSENT OF PubMatic IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE
 * LAWS AND INTERNATIONAL TREATIES.  THE RECEIPT OR POSSESSION OF  THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT CONVEY OR IMPLY ANY RIGHTS
 * TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */

package com.pubmatic.openbid.app.mopubevent;

import android.app.Activity;
import android.util.Log;

import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.pubmatic.sdk.common.POBError;
import com.pubmatic.sdk.openbid.core.POBBid;
import com.pubmatic.sdk.openbid.interstitial.POBInterstitialEvent;
import com.pubmatic.sdk.openbid.interstitial.POBInterstitialEventListener;
import com.pubmatic.sdk.webrendering.ui.POBInterstitialRendering;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class implements the communication between the OpenBid SDK and the MoPub SDK for a given ad
 * unit. It implements the PubMatic's Wrapper interface. PM SDK notifies (using wrapper interface)
 * to make a request to MoPub SDK and pass the targeting parameters. This class also creates the MoPub's
 * MoPubInterstitial, initialize it and listen for the callback methods. And pass the MoPub ad event to
 * OpenBid SDK via POBInterstitialEventListener.
 */
public class MoPubInterstitialEventHandler implements POBInterstitialEvent, MoPubInterstitial.InterstitialAdListener {

    private static final String TAG = "MoPubInterstitialEvent";
    /**
     * Key to pass the PubMatic bid instance to CustomEventInterstitial
     */
    public static String PUBMATIC_BID_KEY = "POBBid";
    /**
     * Config listener to check if publisher want to config properties in MoPub ad
     */
    private MoPubConfigListener mopubConfigListener;
    /**
     * Interface to pass the MoPub ad event to OpenBid SDK
     */
    private POBInterstitialEventListener eventListener;
    /**
     * MoPub Interstitial Ad instance
     */
    private MoPubInterstitial moPubInterstitial;
    /**
     * MoPub Interstitial Ad unit id.
     */
    private String mopubAdUnitId;
    /**
     * Activity context on which interstitial Ad will get displayed.
     */
    private Activity context;

    public MoPubInterstitialEventHandler(Activity context, String adUnitId) {
        this.context = context;
        this.mopubAdUnitId = adUnitId;

    }

    /**
     * Sets the Data listener object. Publisher should implement the MoPubConfigListener and
     * override its method only when publisher needs to set the targeting parameters over MoPub
     * interstitial ad view.
     *
     * @param listener MoPub config listener
     */
    public void setConfigListener(MoPubConfigListener listener) {
        mopubConfigListener = listener;
    }

    private void initializeMoPubAd() {
        destroyMoPubAd();
        moPubInterstitial = new MoPubInterstitial(context, mopubAdUnitId);

        // DO NOT REMOVE/OVERRIDE BELOW LISTENER
        moPubInterstitial.setInterstitialAdListener(this);
    }

    private void destroyMoPubAd() {
        if (moPubInterstitial != null) {
            moPubInterstitial.destroy();
            moPubInterstitial = null;
        }
    }

    //<editor-fold desc="POBInterstitialEvent overridden methods">
    @Override
    public void requestAd(POBBid bid) {

        initializeMoPubAd();
        StringBuilder targetingParams = null;

        // Check if publisher want to set any targeting data
        if (mopubConfigListener != null) {
            mopubConfigListener.configure(moPubInterstitial);
        }

        if (moPubInterstitial.getInterstitialAdListener() != this) {
            Log.w(TAG, "Do not set MoPub listener. This is used by MoPubInterstitialEventHandler internally.");
        }

        if (null != bid) {
            // Logging details of bid objects for debug purpose.
            Log.d(TAG, bid.toString());
            targetingParams = new StringBuilder();
            Map<String, String> targeting = bid.getTargetingInfo();
            if (targeting != null && !targeting.isEmpty()) {
                // using iterator for iteration over Map.entrySet()
                Iterator<Map.Entry<String, String>> iterator = targeting.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    targetingParams.append(entry.getKey() + ":" + entry.getValue());
                    if (iterator.hasNext()) {
                        targetingParams.append(",");
                    }
                    Log.d(TAG, "Targeting param [" + entry.getKey() + "] = " + entry.getValue());
                }
            }

            // Pass bid object to MoPub custom event for rendering PubMatic Ad
            Map<String, Object> localExtra = new HashMap<>();
            localExtra.put(PUBMATIC_BID_KEY, bid);

            // Check if any local extra is configured by publisher, append it
            Map<String, Object> publisherLocalExtra = moPubInterstitial.getLocalExtras();
            if (publisherLocalExtra != null && !publisherLocalExtra.isEmpty()) {
                localExtra.putAll(publisherLocalExtra);
            }
            moPubInterstitial.setLocalExtras(localExtra);
        }
        //Add custom targeting parameters to MoPub Ad request
        if (targetingParams != null) {

            // Check if keywords is configured by publisher, append it
            String publisherKeywords = moPubInterstitial.getKeywords();
            if (publisherKeywords != null && !"".equalsIgnoreCase(publisherKeywords)) {
                targetingParams.append(",");
                targetingParams.append(publisherKeywords);
            }
            moPubInterstitial.setKeywords(targetingParams.toString());
        }
        // Load MoPub ad request
        moPubInterstitial.load();
    }

    @Override
    public void setEventListener(POBInterstitialEventListener listener) {
        this.eventListener = listener;
    }

    @Override
    public POBInterstitialRendering getRenderer(String partnerName) {
        return null;
    }

    @Override
    public void show() {
        if (moPubInterstitial != null && moPubInterstitial.isReady()) {
            moPubInterstitial.show();
        }
    }

    @Override
    public void destroy() {
        destroyMoPubAd();
    }

    //<editor-fold desc="InterstitialAdListener overridden methods">
    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        if (null != eventListener) {
            eventListener.onAdServerWin();
        }
    }
    //</editor-fold>

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {

        if (null != eventListener) {
            switch (errorCode) {
                case NO_FILL:
                case NETWORK_NO_FILL:
                    eventListener.onFailed(new POBError(POBError.NO_ADS_AVAILABLE, errorCode.toString()));
                    break;
                case NO_CONNECTION:
                case NETWORK_TIMEOUT:
                    eventListener.onFailed(new POBError(POBError.NETWORK_ERROR, errorCode.toString()));
                    break;
                case SERVER_ERROR:
                    eventListener.onFailed(new POBError(POBError.SERVER_ERROR, errorCode.toString()));
                    break;
                case CANCELLED:
                    eventListener.onFailed(new POBError(POBError.REQUEST_CANCELLED, errorCode.toString()));
                    break;
                default:
                    eventListener.onFailed(new POBError(POBError.INTERNAL_ERROR, errorCode.toString()));
                    break;
            }
        } else {
            Log.e(TAG, "Can not call failure callback, POBInterstitialEventListener reference null. MoPub error:"+errorCode.toString());
        }
    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {
        if (null != eventListener) {
            eventListener.onAdOpened();
        }
    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        if (null != eventListener) {
            eventListener.onAdLeftApplication();
        }
    }

    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        if (null != eventListener) {
            eventListener.onAdClosed();
        }
        destroy();
    }

    /**
     * Interface to get the MoPub Interstitial ad object, to configure the properties.
     */
    public interface MoPubConfigListener {
        /**
         * This method is called before event handler makes an ad request call to MoPub SDK. It passes
         * MoPub ad object which will be used to make an ad request. Publisher can configure the ad
         * request properties on the provided object.
         *
         * @param ad MoPub Interstitial ad
         */
        void configure(MoPubInterstitial ad);
    }
    //</editor-fold>

}
