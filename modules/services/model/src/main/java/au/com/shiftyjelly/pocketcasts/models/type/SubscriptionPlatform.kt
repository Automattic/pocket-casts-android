package au.com.shiftyjelly.pocketcasts.models.type

enum class SubscriptionPlatform(val label: String) {
    NONE("none"),
    IOS("ios"),
    ANDROID("android"),
    WEB("web"),
    GIFT("gift");
    override fun toString() = label
}
