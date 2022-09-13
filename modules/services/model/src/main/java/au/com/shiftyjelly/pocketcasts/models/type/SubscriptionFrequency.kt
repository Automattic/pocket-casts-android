package au.com.shiftyjelly.pocketcasts.models.type

enum class SubscriptionFrequency(val label: String) {
    NONE("none"),
    MONTHLY("monthly"),
    YEARLY("yearly");
    override fun toString() = label
}
