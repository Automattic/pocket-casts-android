package au.com.shiftyjelly.pocketcasts.models.type

enum class SubscriptionTier(val label: String) {
    NONE("none"),
    PLUS("plus"),
    PATRON("patron");
    override fun toString() = label
    companion object {
        fun fromString(string: String) = SubscriptionTier.values().find { it.label == string.lowercase() } ?: NONE
    }
}
