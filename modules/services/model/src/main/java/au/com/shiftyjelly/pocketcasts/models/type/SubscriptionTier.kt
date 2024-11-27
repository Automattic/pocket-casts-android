package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PATRON_MONTHLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PATRON_YEARLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_MONTHLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_YEARLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class SubscriptionTier(
    val label: String,
    val isPaid: Boolean,
    val supportedProductIds: List<String>,
) {
    NONE(
        label = "none",
        isPaid = false,
        supportedProductIds = emptyList(),
    ),
    PLUS(
        label = "plus",
        isPaid = true,
        supportedProductIds = listOf(PLUS_MONTHLY_PRODUCT_ID, PLUS_YEARLY_PRODUCT_ID),
    ),
    PATRON(
        label = "patron",
        isPaid = true,
        supportedProductIds = listOf(PATRON_MONTHLY_PRODUCT_ID, PATRON_YEARLY_PRODUCT_ID),
    ),
    ;

    override fun toString() = label

    fun toUserTier() =
        when (this) {
            NONE -> UserTier.Free
            PLUS -> UserTier.Plus
            PATRON -> UserTier.Patron
        }

    companion object {
        private val productIdToTierMap: Map<String, SubscriptionTier> = SubscriptionTier.entries.flatMap { entry ->
            entry.supportedProductIds.map { productId -> productId to entry }
        }.toMap()

        fun fromString(string: String?) = SubscriptionTier.entries.find { it.label.equals(string, ignoreCase = true) } ?: NONE

        fun fromProductId(productId: String): SubscriptionTier = productIdToTierMap[productId] ?: NONE

        fun fromFeatureTier(feature: Feature) = when (feature.tier) {
            is FeatureTier.Free -> NONE
            is FeatureTier.Plus -> if (feature.isCurrentlyExclusiveToPatron()) PATRON else PLUS
            is FeatureTier.Patron -> PATRON
        }
    }
}
