package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.automattic.eventhorizon.DiscoverListEpisodePlayEvent
import com.automattic.eventhorizon.DiscoverListEpisodeTappedEvent
import com.automattic.eventhorizon.DiscoverListImpressionEvent
import com.automattic.eventhorizon.DiscoverListPodcastSubscribedEvent
import com.automattic.eventhorizon.DiscoverListPodcastTappedEvent
import com.automattic.eventhorizon.DiscoverListShowAllTappedEvent
import com.automattic.eventhorizon.Trackable
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnonymousBumpStatsTracker @Inject constructor(
    appDatabase: AppDatabase,
    private val settings: Settings,
) : AnalyticsTracker,
    CoroutineScope {
    private val bumpStatsDao = appDatabase.bumpStatsDao()

    override val coroutineContext: CoroutineContext = Dispatchers.IO

    override val id get() = ID

    override fun track(event: Trackable): TrackedEvent? {
        if (!settings.collectAnalytics.value || event::class.java !in PAID_SPONSOR_RELATED_EVENTS) {
            return null
        }

        val bumpStat = AnonymousBumpStat(
            name = event.analyticsName.lowercase(Locale.getDefault()),
            customEventProps = event.analyticsProperties,
        ).withBumpName()

        launch {
            bumpStatsDao.insert(bumpStat)
        }

        return TrackedEvent(
            key = bumpStat.name,
            properties = bumpStat.customEventProps,
        )
    }

    private companion object {
        private const val ID = "BumpStats"

        private val PAID_SPONSOR_RELATED_EVENTS = listOf(
            DiscoverListImpressionEvent::class.java,
            DiscoverListShowAllTappedEvent::class.java,
            DiscoverListEpisodePlayEvent::class.java,
            DiscoverListEpisodeTappedEvent::class.java,
            DiscoverListPodcastSubscribedEvent::class.java,
            DiscoverListPodcastTappedEvent::class.java,
        )
    }
}
