package au.com.shiftyjelly.pocketcasts.repositories.appreview

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppReviewAnalyticsListener @Inject constructor(
    private val settings: Settings,
    private val clock: Clock,
) : AnalyticsTracker.Listener {
    private val episodesCompletedSetting = settings.appReviewEpisodeCompletedTimestamps

    override fun onEvent(event: AnalyticsEvent, properties: Map<String, Any>) {
        when (event) {
            AnalyticsEvent.PLAYER_EPISODE_COMPLETED -> {
                val timestamps = episodesCompletedSetting.value
                if (timestamps.size <= 3) {
                    episodesCompletedSetting.set(timestamps + clock.instant(), updateModifiedAt = false)
                }
            }

            else -> Unit
        }
    }
}
