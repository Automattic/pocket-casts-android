package au.com.shiftyjelly.pocketcasts.models.type

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class SubscriptionPlatform(val label: String) {
    NONE("none"),
    IOS("ios"),
    ANDROID("android"),
    WEB("web"),
    GIFT("gift"),
    ;
    override fun toString() = label
}
