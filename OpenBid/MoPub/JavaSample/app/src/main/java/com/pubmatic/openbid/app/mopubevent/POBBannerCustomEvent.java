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

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.MoPubErrorCode;
import com.pubmatic.sdk.common.POBError;
import com.pubmatic.sdk.common.base.POBAdDescriptor;
import com.pubmatic.sdk.common.base.POBAdRendererListener;
import com.pubmatic.sdk.common.utility.PMUtils;
import com.pubmatic.sdk.openbid.core.POBBid;
import com.pubmatic.sdk.openbid.core.POBRenderer;
import com.pubmatic.sdk.webrendering.mraid.POBWebRenderer;
import com.pubmatic.sdk.webrendering.ui.POBBannerRendering;

import java.util.Map;

public class POBBannerCustomEvent extends CustomEventBanner {

    private final String TAG = "POBBannerCustomEvent";
    static final String BID_KEY = "pubmatic_bid";
    private CustomEventBannerListener customEventBannerListener;
    private POBBid bid;

    @Override
    protected void loadBanner(Context context, CustomEventBannerListener customEventBannerListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        this.customEventBannerListener = customEventBannerListener;
        Log.d(TAG, "loadBanner");
        bid = (POBBid) localExtras.get(BID_KEY);
        if(null != bid){
            POBBannerRendering renderer = POBRenderer.getBannerRenderer(context);
            ((POBWebRenderer)renderer).setRefreshTimeoutInSec(bid.getRefreshInterval());
            renderer.setAdRendererListener(new AdRendererListenerImp());
            renderer.renderAd(bid);
        }else {
            handlerFailure(new POBError(POBError.NO_ADS_AVAILABLE, "Pubmatic Ads not available!"));
        }
    }



    @Override
    protected void onInvalidate() {
        if(null != customEventBannerListener){
            customEventBannerListener = null;
        }
        if(null != bid){
            bid = null;
        }
    }

    private void handlerFailure(POBError error){
        if(null != customEventBannerListener){
            MoPubErrorCode moPubErrorCode;
            switch (error.getErrorCode()) {
                case POBError.NO_ADS_AVAILABLE:
                    moPubErrorCode = MoPubErrorCode.NETWORK_NO_FILL;
                    break;
                case POBError.NETWORK_ERROR:
                    moPubErrorCode = MoPubErrorCode.NO_CONNECTION;
                    break;
                case POBError.SERVER_ERROR:
                    moPubErrorCode = MoPubErrorCode.SERVER_ERROR;
                    break;
                case POBError.TIMEOUT_ERROR:
                    moPubErrorCode = MoPubErrorCode.NETWORK_TIMEOUT;
                    break;
                case POBError.INTERNAL_ERROR:
                    moPubErrorCode = MoPubErrorCode.INTERNAL_ERROR;
                    break;
                case POBError.REQUEST_CANCELLED:
                    moPubErrorCode = MoPubErrorCode.CANCELLED;
                    break;
                case POBError.INVALID_REQUEST:
                    moPubErrorCode = MoPubErrorCode.NETWORK_INVALID_STATE;
                    break;
                default:
                    moPubErrorCode = MoPubErrorCode.UNSPECIFIED;
                    break;
            }
            customEventBannerListener.onBannerFailed(moPubErrorCode);
        }else {
            Log.e(TAG, "Can not call onAdRenderingFailed, CustomEventBannerListener reference null.");
        }
    }


    class AdRendererListenerImp implements POBAdRendererListener {

        @Override
        public void onAdRender(View view, POBAdDescriptor descriptor) {
            if(null != customEventBannerListener){
                FrameLayout parent = new FrameLayout(view.getContext());
                ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(PMUtils.convertDpToPixel(bid.getWidth()),
                        PMUtils.convertDpToPixel(bid.getHeight()));
                parent.addView(view, params);
                customEventBannerListener.onBannerLoaded(parent);
            }else {
                Log.e(TAG, "Can not call onBannerLoaded, CustomEventBannerListener reference null.");
            }
        }

        @Override
        public void onAdRenderingFailed(POBError error) {
            handlerFailure(error);
        }

        @Override
        public void onRenderAdClick() {
            if(null != customEventBannerListener){
                customEventBannerListener.onBannerClicked();
            }else {
                Log.e(TAG, "Can not call onBannerClicked, CustomEventBannerListener reference null.");
            }
        }

        @Override
        public void onAdInteractionStarted() {
            if(null != customEventBannerListener){
                customEventBannerListener.onBannerExpanded();
            }else {
                Log.e(TAG, "Can not call onBannerExpanded, CustomEventBannerListener reference null.");
            }

        }

        @Override
        public void onAdInteractionStopped() {
            if(null != customEventBannerListener){
                customEventBannerListener.onBannerCollapsed();
            }else {
                Log.e(TAG, "Can not call onBannerCollapsed, CustomEventBannerListener reference null.");
            }

        }

        @Override
        public void onMRAIDAdClick() {
            if(null != customEventBannerListener){
                customEventBannerListener.onBannerClicked();
            }else {
                Log.e(TAG, "Can not call onBannerClicked, CustomEventBannerListener reference null.");
            }

        }

        @Override
        public void onAdUnload() {
            //No action required
        }

        @Override
        public void onLeavingApplication() {
            if(null != customEventBannerListener){
                customEventBannerListener.onLeaveApplication();
            }else {
                Log.e(TAG, "Can not call onLeaveApplication, CustomEventBannerListener reference null.");
            }
        }



    }



}

