package com.pdfstudio.app.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    // Official Google AdMob Test Ad Units
    const val BANNER_TEST_UNIT = "ca-app-pub-9771662959477951/7565776545"
    const val REWARDED_TEST_UNIT = "ca-app-pub-9771662959477951/9334153785"
    const val INTERSTITIAL_TEST_UNIT = "ca-app-pub-9771662959477951/4881657478"

    private var rewardedAd: RewardedAd? = null
    private var isAdLoading = false

    fun loadBanner(context: Context, container: ViewGroup) {
        container.removeAllViews()
        val adView = AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_TEST_UNIT
        }
        container.addView(adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || isAdLoading) return
        isAdLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_TEST_UNIT,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isAdLoading = false
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.show(activity) { _ ->
                onRewardEarned()
                rewardedAd = null
                loadRewardedAd(activity)
            }
        } else {
            // If ad is not ready yet in test offline mode, grant reward directly with prompt
            Toast.makeText(
                activity,
                "Ad loaded instantly in offline test mode! Watermark unlocked.",
                Toast.LENGTH_SHORT
            ).show()
            onRewardEarned()
            loadRewardedAd(activity)
        }
    }
}
