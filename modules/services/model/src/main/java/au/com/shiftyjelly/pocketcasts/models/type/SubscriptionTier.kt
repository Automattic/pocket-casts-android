package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class SubscriptionTier(
    val label: String,
    val isPaid: Boolean,
) {
    NONE(
        label = "none",
        isPaid = false,
    ),
    PLUS(
        label = "plus",
        isPaid = true,
    ),
    PATRON(
        label = "patron",
        isPaid = true,
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
        fun fromString(string: String?) = SubscriptionTier.entries.find { it.label.equals(string, ignoreCase = true) } ?: NONE

        fun fromFeatureTier(feature: Feature) = when (feature.tier) {
            is FeatureTier.Free -> NONE
            is FeatureTier.Plus -> if (feature.isCurrentlyExclusiveToPatron()) PATRON else PLUS
            is FeatureTier.Patron -> PATRON
        }
    }
}
