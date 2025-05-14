package au.com.shiftyjelly.pocketcasts.models.type

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

    companion object {
        fun fromString(string: String?) = SubscriptionTier.entries.find { it.label.equals(string, ignoreCase = true) } ?: NONE
    }
}
