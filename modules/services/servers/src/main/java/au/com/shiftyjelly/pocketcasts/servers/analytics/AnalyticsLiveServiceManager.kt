package au.com.shiftyjelly.pocketcasts.servers.analytics

interface AnalyticsLiveServiceManager {
    suspend fun sendEvents(url: String, events: List<InputEvent>)
}
