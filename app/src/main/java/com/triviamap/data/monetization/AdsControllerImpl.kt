package com.triviamap.data.monetization

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.triviamap.BuildConfig
import com.triviamap.domain.monetization.AdsController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

/** Google UMP consent first (GDPR), the AdMob SDK is only initialized once ads may be requested. */
@Singleton
class AdsControllerImpl @Inject constructor(
    @ApplicationContext private val appContext: Context
) : AdsController {

    private val _canRequestAds = MutableStateFlow(false)
    override val canRequestAds: StateFlow<Boolean> = _canRequestAds

    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired

    private val _rewardedReady = MutableStateFlow(false)
    override val rewardedReady: StateFlow<Boolean> = _rewardedReady

    private val sdkStarted = AtomicBoolean(false)
    // Touched on the main thread only (Compose / AdMob callbacks)
    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false

    override fun gatherConsent(activity: Activity) {
        if (!BuildConfig.ADS_ENABLED) return
        val info = UserMessagingPlatform.getConsentInformation(activity)
        // A choice stored by a previous session may already allow ads
        refresh(info)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { refresh(info) }
            },
            { error ->
                android.util.Log.w("AdsController", "consent update failed: ${error.message}")
                refresh(info)
            }
        )
    }

    override fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            refresh(UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    override fun loadRewarded() {
        if (!BuildConfig.ADS_ENABLED || !_canRequestAds.value || rewardedAd != null || rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(
            appContext,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedLoading = false
                    rewardedAd = ad
                    _rewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedLoading = false
                    rewardedAd = null
                    _rewardedReady.value = false
                    android.util.Log.w("AdsController", "rewarded load failed: ${error.code} ${error.message}")
                }
            }
        )
    }

    override fun showRewarded(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedAd ?: return
        rewardedAd = null
        _rewardedReady.value = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = loadRewarded()
            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) = loadRewarded()
        }
        ad.show(activity) { onRewarded() }
    }

    private fun refresh(info: ConsentInformation) {
        android.util.Log.d("AdsController", "consent status=${info.consentStatus} canRequestAds=${info.canRequestAds()}")
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (info.canRequestAds()) {
            startSdk()
            _canRequestAds.value = true
        } else {
            _canRequestAds.value = false
        }
    }

    private fun startSdk() {
        if (!sdkStarted.compareAndSet(false, true)) return
        thread(name = "ads-init") {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                    .build()
            )
            MobileAds.initialize(appContext)
        }
    }
}
