package au.com.shiftyjelly.pocketcasts.servers.analytics

import javax.inject.Inject
import timber.log.Timber

class AnalyticsLiveServiceManagerImpl @Inject constructor(
    private val service: AnalyticsLiveService,
) : AnalyticsLiveServiceManager {
    override suspend fun sendEvents(url: String, events: List<InputEvent>) {
        runCatching {
            service.sendEvents(url, events)
        }.onFailure { error ->
            Timber.e(error, "Failed to send analytics events")
        }
    }
}
