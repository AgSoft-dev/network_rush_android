package com.triviamap.domain.monetization

import android.app.Activity
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Optional tips. Consumable one-time products, so they can be bought again. Ids must match the Play Console. */
enum class SupportTier(val productId: String, val emoji: String) {
    COFFEE("support_coffee", "\u2615"),
    TRAM_TICKET("support_tram_ticket", "\uD83C\uDF9F\uFE0F");

    companion object {
        fun fromProductId(id: String): SupportTier? = entries.firstOrNull { it.productId == id }
    }
}

/** A tip the store can currently sell, with its localized price. */
data class SupportOffer(val tier: SupportTier, val price: String)

sealed interface SupportEvent {
    data class Thanks(val tier: SupportTier?) : SupportEvent
    data object Failed : SupportEvent
}

/** Google Play Billing behind an interface (tips only, nothing gates gameplay). */
interface SupportRepository {
    /** Tips the store returned; empty while offline, not connected, or products not configured. */
    val offers: StateFlow<List<SupportOffer>>
    val events: SharedFlow<SupportEvent>
    /** Idempotent: connects to Play, loads prices and acknowledges purchases interrupted earlier. */
    fun connect()
    fun purchase(activity: Activity, tier: SupportTier)
}

/** Consent-aware AdMob behind an interface (the banner is shown on the home screen only). */
interface AdsController {
    /** True once the user's consent choice allows requesting ads and the SDK is initialized. */
    val canRequestAds: StateFlow<Boolean>
    /** True when the user must be able to reopen the consent form (GDPR "privacy choices"). */
    val privacyOptionsRequired: StateFlow<Boolean>
    fun gatherConsent(activity: Activity)
    fun showPrivacyOptions(activity: Activity)
}

object MonetizationPolicy {
    /** Supporters (anyone who tipped) never see the banner. */
    fun shouldShowBanner(isSupporter: Boolean, canRequestAds: Boolean): Boolean = canRequestAds && !isSupporter
}
