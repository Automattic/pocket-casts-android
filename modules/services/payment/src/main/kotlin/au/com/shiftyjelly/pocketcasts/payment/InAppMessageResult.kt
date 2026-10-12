package au.com.shiftyjelly.pocketcasts.payment

sealed interface InAppMessageResult {
    data object NoActionNeeded : InAppMessageResult

    data class SubscriptionStatusUpdated(
        val purchaseToken: String?,
    ) : InAppMessageResult
}
