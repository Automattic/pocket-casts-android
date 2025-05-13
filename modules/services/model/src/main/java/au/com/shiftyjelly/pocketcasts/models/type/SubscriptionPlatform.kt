package au.com.shiftyjelly.pocketcasts.models.type

import com.squareup.moshi.JsonClass

@Suppress("ktlint:standard:enum-entry-name-case")
@JsonClass(generateAdapter = false)
enum class SubscriptionPlatform(
    val isPaid: Boolean,
    val analyticsValue: String,
) {
    Android(
        isPaid = true,
        analyticsValue = "android",
    ),
    iOS(
        isPaid = true,
        analyticsValue = "ios",
    ),
    Web(
        isPaid = true,
        analyticsValue = "web",
    ),
    Gift(
        isPaid = false,
        analyticsValue = "gift",
    ),
    Unknown(
        isPaid = false,
        analyticsValue = "unknown",
    ),
}
