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

package com.pubmatic.openbid.kotlinsampleapp.mopubevent

import android.app.Activity
import android.util.Log
import com.mopub.mobileads.MoPubErrorCode
import com.mopub.mobileads.MoPubInterstitial
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openbid.core.POBBid
import com.pubmatic.sdk.openbid.interstitial.POBInterstitialEvent
import com.pubmatic.sdk.openbid.interstitial.POBInterstitialEventListener
import com.pubmatic.sdk.webrendering.ui.POBInterstitialRendering
import java.util.*

/**
 * This class implements the communication between the OpenBid SDK and the MoPub SDK for a given ad
 * unit. It implements the PubMatic's Wrapper interface. PM SDK notifies (using wrapper interface)
 * to make a request to MoPub SDK and pass the targeting parameters. This class also creates the MoPub's
 * MoPubInterstitial, initialize it and listen for the callback methods. And pass the MoPub ad event to
 * OpenBid SDK via POBInterstitialEventListener.
 */
open class MoPubInterstitialEventHandler(
        /**
         * Activity context on which interstitial Ad will get displayed.
         */
        private val context: Activity,
        /**
         * MoPub Interstitial Ad unit id.
         */
        private val mopubAdUnitId: String) : POBInterstitialEvent, MoPubInterstitial.InterstitialAdListener {

    /**
     * Interface to get the MoPub Interstitial ad object, to configure the properties.
     */
    interface MoPubConfigListener {
        /**
         * This method is called before event handler makes ad request call to MoPub SDK. It passes
         * MoPub ad object which will be used to make an ad request. Publisher can configure the ad
         * request properties on the provided objects.
         * @param ad MoPub Interstitial ad
         */
        fun configure(ad: MoPubInterstitial)
    }

    /**
     * Interface to pass the MoPub ad event to OpenBid SDK
     */
    private var eventListener: POBInterstitialEventListener? = null

    /**
     * Config listener to check if publisher want to config properties in MoPub ad
     */
    private var mopubConfigListener: MoPubConfigListener? = null

    /**
     * MoPub Interstitial Ad instance
     */
    private var moPubInterstitial: MoPubInterstitial? = null

    private fun initializeMoPubAd() {
        destroyMoPubAd()
        moPubInterstitial = MoPubInterstitial(context, mopubAdUnitId)

        // DO NOT REMOVE/OVERRIDE BELOW LISTENER
        moPubInterstitial?.setInterstitialAdListener(this)
    }

    /**
     * Sets the Data listener object. Publisher should implement the MoPubConfigListener and
     * override its method only when publisher needs to set the targeting parameters over MoPub
     * interstitial ad view.
     *
     * @param listener MoPub config listener
     */
    fun setConfigListener(listener: MoPubConfigListener) {
        mopubConfigListener = listener
    }

    private fun destroyMoPubAd() {
        if (moPubInterstitial != null) {
            moPubInterstitial?.destroy()
            moPubInterstitial = null
        }
    }

    //<editor-fold desc="POBInterstitialEvent overridden methods">
    override fun requestAd(bid: POBBid?) {

        initializeMoPubAd()
        var targetingParams: StringBuilder? = null

        // Check if publisher want to set any targeting data
        moPubInterstitial?.let { mopubConfigListener?.configure(it) }

        if (moPubInterstitial?.interstitialAdListener != this) {
            Log.w(TAG, "Do not set MoPub listener. This is used by MoPubInterstitialEventHandler internally.")
        }

        if (null != bid) {
            // Logging details of bid objects for debug purpose.
            Log.d(TAG, bid.toString())
            targetingParams = StringBuilder()
            val targeting = bid.targetingInfo
            if (targeting != null && !targeting.isEmpty()) {
                // using iterator for iteration over Map.entrySet()
                val iterator = targeting.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    targetingParams.append(entry.key + ":" + entry.value)
                    if (iterator.hasNext()) {
                        targetingParams.append(",")
                    }
                }
            }

            //Pass bid object to MoPub custom event for rendering PubMatic Ad
            val localExtra = HashMap<String, Any>()
            localExtra[PUBMATIC_BID_KEY] = bid

            // Check if any local extra is configured by publisher, append it
            val publisherLocalExtra = moPubInterstitial?.getLocalExtras()
            if (publisherLocalExtra != null && !publisherLocalExtra.isEmpty()) {
                localExtra.putAll(publisherLocalExtra)
            }
            moPubInterstitial!!.setLocalExtras(localExtra)
        }
        //Add custom targeting parameters to MoPub Ad request
        if (targetingParams != null) {

            // Check if keywords is configured by publisher, append it
            val publisherKeywords = moPubInterstitial?.getKeywords()
            if (publisherKeywords != null && !"".equals(publisherKeywords, ignoreCase = true)) {
                targetingParams.append(",")
                targetingParams.append(publisherKeywords)
            }
            moPubInterstitial?.setKeywords(targetingParams.toString())
        }
        // Load MoPub ad request
        moPubInterstitial?.load()
    }

    override fun setEventListener(listener: POBInterstitialEventListener) {
        this.eventListener = listener
    }

    override fun getRenderer(partnerName: String): POBInterstitialRendering? {
        return null
    }

    override fun show() {
        if (moPubInterstitial != null && moPubInterstitial?.isReady()!!) {
            moPubInterstitial?.show()
        }
    }

    override fun destroy() {
        destroyMoPubAd()
    }
    //</editor-fold>

    //<editor-fold desc="InterstitialAdListener overridden methods">
    override fun onInterstitialLoaded(interstitial: MoPubInterstitial) {
        eventListener?.onAdServerWin()
    }

    override fun onInterstitialFailed(interstitial: MoPubInterstitial, errorCode: MoPubErrorCode) {
        if (null != eventListener) {
            when (errorCode) {
                MoPubErrorCode.NO_FILL, MoPubErrorCode.NETWORK_NO_FILL -> eventListener?.onFailed(POBError(POBError.NO_ADS_AVAILABLE, errorCode.toString()))
                MoPubErrorCode.NO_CONNECTION, MoPubErrorCode.NETWORK_TIMEOUT -> eventListener?.onFailed(POBError(POBError.NETWORK_ERROR, errorCode.toString()))
                MoPubErrorCode.SERVER_ERROR -> eventListener?.onFailed(POBError(POBError.SERVER_ERROR, errorCode.toString()))
                MoPubErrorCode.CANCELLED -> eventListener?.onFailed(POBError(POBError.REQUEST_CANCELLED, errorCode.toString()))
                else -> eventListener?.onFailed(POBError(POBError.INTERNAL_ERROR, errorCode.toString()))
            }
        }else{
            Log.e(TAG, "Can not call failure callback, POBInterstitialEventListener reference null. MoPub error:$errorCode")
        }
    }

    override fun onInterstitialShown(interstitial: MoPubInterstitial) {
        eventListener?.onAdOpened()
    }

    override fun onInterstitialClicked(interstitial: MoPubInterstitial) {
        eventListener?.onAdLeftApplication()
    }

    override fun onInterstitialDismissed(interstitial: MoPubInterstitial) {
        eventListener?.onAdClosed()
        destroy()
    }

    companion object {

        private val TAG = "MoPubInterstitialEvent"

        /**
         * Key to pass the PubMatic bid instance to CustomEventInterstitial
         */
        var PUBMATIC_BID_KEY = "POBBid"
    }
    //</editor-fold>

}
