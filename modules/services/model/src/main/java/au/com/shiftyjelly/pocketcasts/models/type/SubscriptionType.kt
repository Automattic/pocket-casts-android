package au.com.shiftyjelly.pocketcasts.models.type

enum class SubscriptionType(val label: String) {
    NONE("none"),
    PLUS("plus");
    override fun toString() = label
}
