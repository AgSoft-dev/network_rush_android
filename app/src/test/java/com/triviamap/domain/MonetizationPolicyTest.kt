package com.triviamap.domain

import com.triviamap.domain.monetization.MonetizationPolicy
import com.triviamap.domain.monetization.SupportTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonetizationPolicyTest {
    @Test fun bannerNeedsConsent() = assertFalse(MonetizationPolicy.shouldShowBanner(isSupporter = false, canRequestAds = false))
    @Test fun bannerShownWhenConsentedAndNotSupporter() = assertTrue(MonetizationPolicy.shouldShowBanner(false, true))
    @Test fun supportersNeverSeeBanner() = assertFalse(MonetizationPolicy.shouldShowBanner(true, true))

    @Test fun productIdsAreUniqueAndResolvable() {
        assertEquals(SupportTier.entries.size, SupportTier.entries.map { it.productId }.toSet().size)
        SupportTier.entries.forEach { assertEquals(it, SupportTier.fromProductId(it.productId)) }
        assertNull(SupportTier.fromProductId("unknown"))
    }
}
