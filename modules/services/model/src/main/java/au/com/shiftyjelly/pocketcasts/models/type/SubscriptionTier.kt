package au.com.shiftyjelly.pocketcasts.models.type

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
        // Till subscriptionTier is not supported, subscriptionType is used as a fallback to determine subscriptionTier
        fun fromString(string: String?, subscriptionType: SubscriptionType) =
            SubscriptionTier.values().find { it.label == string?.lowercase() } ?: if (subscriptionType == SubscriptionType.PLUS) PLUS else NONE
    }
}
