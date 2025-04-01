package au.com.shiftyjelly.pocketcasts.analytics

sealed class TrackerType {
    object FirstParty : TrackerType()
    object ThirdParty : TrackerType()
}
