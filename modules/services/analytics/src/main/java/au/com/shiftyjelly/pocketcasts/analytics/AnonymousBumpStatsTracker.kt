package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnonymousBumpStatsTracker @Inject constructor(
    appDatabase: AppDatabase,
    private val settings: Settings,
) : Tracker {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val bumpStatsDao = appDatabase.bumpStatsDao()

    override val id get() = ID

    override fun shouldTrack(event: AnalyticsEvent): Boolean {
        return event in PAID_SPONSOR_RELATED_EVENTS && settings.collectAnalytics.value
    }

    override fun track(event: AnalyticsEvent, properties: Map<String, Any>): TrackedEvent {
        scope.launch {
            val bumpStat = AnonymousBumpStat(
                name = event.name.lowercase(Locale.getDefault()),
                customEventProps = properties,
            ).withBumpName()
            bumpStatsDao.insert(bumpStat)
        }
        return TrackedEvent(event, properties)
    }

    override fun refreshMetadata() = Unit

    override fun flush() = Unit

    override fun clearAllData() = Unit

    private companion object {
        private const val ID = "BumpStats"

        private val PAID_SPONSOR_RELATED_EVENTS = listOf(
            AnalyticsEvent.DISCOVER_LIST_IMPRESSION,
            AnalyticsEvent.DISCOVER_LIST_SHOW_ALL_TAPPED,
            AnalyticsEvent.DISCOVER_LIST_EPISODE_PLAY,
            AnalyticsEvent.DISCOVER_LIST_EPISODE_TAPPED,
            AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED,
            AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED,
        )
    }
}
