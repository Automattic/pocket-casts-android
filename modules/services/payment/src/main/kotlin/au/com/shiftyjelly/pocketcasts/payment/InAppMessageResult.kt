package au.com.shiftyjelly.pocketcasts.payment

sealed interface InAppMessageResult {
    data object NoActionNeeded : InAppMessageResult

    data object SubscriptionStatusUpdated : InAppMessageResult
}
