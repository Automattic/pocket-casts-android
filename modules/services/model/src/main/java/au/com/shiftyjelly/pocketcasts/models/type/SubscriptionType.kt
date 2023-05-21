package au.com.shiftyjelly.pocketcasts.models.type

enum class SubscriptionType(val label: String) {
    NONE("none"),
    PLUS("plus"),
    PATRON("patron");
    override fun toString() = label
}
