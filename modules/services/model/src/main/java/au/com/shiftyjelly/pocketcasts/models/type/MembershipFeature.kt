package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class Membership(
    val subscription: Subscription?,
    val createdAt: Instant?,
    val features: List<MembershipFeature>,
) {
    fun hasFeature(feature: MembershipFeature): Boolean {
        val isSubscriptionFeature = subscription?.tier?.hasFeature(feature) == true
        return isSubscriptionFeature || feature in features
    }

    companion object {
        val Empty = Membership(
            subscription = null,
            createdAt = null,
            features = emptyList(),
        )
    }
}

@JsonClass(generateAdapter = false)
enum class MembershipFeature {
    NoBannerAds,
    NoDiscoverAds,
}

private fun SubscriptionTier.hasFeature(feature: MembershipFeature) = when (this) {
    SubscriptionTier.Plus -> when (feature) {
        MembershipFeature.NoBannerAds -> true
        MembershipFeature.NoDiscoverAds -> true
    }

    SubscriptionTier.Patron -> when (feature) {
        MembershipFeature.NoBannerAds -> true
        MembershipFeature.NoDiscoverAds -> true
    }
}
