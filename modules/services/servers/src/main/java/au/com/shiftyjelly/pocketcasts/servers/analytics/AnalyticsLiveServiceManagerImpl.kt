package au.com.shiftyjelly.pocketcasts.servers.analytics

import javax.inject.Inject

class AnalyticsLiveServiceManagerImpl @Inject constructor(
    private val service: AnalyticsLiveService,
) : AnalyticsLiveServiceManager {
    override suspend fun sendEvents(url: String, events: List<InputEvent>) {
        service.sendEvents(url, events)
    }
}
