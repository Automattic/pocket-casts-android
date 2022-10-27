package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class AnonymousBumpStatsTracker @Inject constructor(
    appDatabase: AppDatabase
) : Tracker, CoroutineScope {
    companion object {
        private val PAID_SPONSOR_RELATED_EVENTS = listOf(
            AnalyticsEvent.DISCOVER_LIST_IMPRESSION,
            AnalyticsEvent.DISCOVER_LIST_SHOW_ALL_TAPPED,
            AnalyticsEvent.DISCOVER_LIST_EPISODE_PLAY,
            AnalyticsEvent.DISCOVER_LIST_EPISODE_TAPPED,
            AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED,
            AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED
        )

        private fun shouldTrack(event: AnalyticsEvent) =
            PAID_SPONSOR_RELATED_EVENTS.contains(event)
    }

    override val coroutineContext: CoroutineContext = Dispatchers.IO
    private val bumpStatsDao = appDatabase.bumpStatsDao()

    override fun track(event: AnalyticsEvent, properties: Map<String, Any>) {
        if (shouldTrack(event)) {
            launch {
                val bumpStat = AnonymousBumpStat(
                    name = event.name.lowercase(Locale.getDefault()),
                    customEventProps = properties
                ).withBumpName()
                bumpStatsDao.insert(bumpStat)
            }
        }
    }

    override fun refreshMetadata() {}
    override fun flush() {}
    override fun clearAllData() {}
}
