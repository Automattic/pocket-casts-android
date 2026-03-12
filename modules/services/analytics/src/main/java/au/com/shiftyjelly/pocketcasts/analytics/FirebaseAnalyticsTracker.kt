package au.com.shiftyjelly.pocketcasts.analytics

import android.os.Bundle
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.automattic.eventhorizon.BannerAdImpressionEvent
import com.automattic.eventhorizon.BannerAdReportEvent
import com.automattic.eventhorizon.BannerAdTappedEvent
import com.automattic.eventhorizon.DiscoverFeaturedPodcastSubscribedEvent
import com.automattic.eventhorizon.DiscoverFeaturedPodcastTappedEvent
import com.automattic.eventhorizon.DiscoverListEpisodePlayEvent
import com.automattic.eventhorizon.DiscoverListEpisodeTappedEvent
import com.automattic.eventhorizon.DiscoverListImpressionEvent
import com.automattic.eventhorizon.DiscoverListPodcastSubscribedEvent
import com.automattic.eventhorizon.DiscoverListPodcastTappedEvent
import com.automattic.eventhorizon.DiscoverListShowAllTappedEvent
import com.automattic.eventhorizon.PlaybackPlayEvent
import com.automattic.eventhorizon.PlusPromotionDismissedEvent
import com.automattic.eventhorizon.PlusPromotionNotNowButtonTappedEvent
import com.automattic.eventhorizon.PlusPromotionShownEvent
import com.automattic.eventhorizon.PodcastSubscribedEvent
import com.automattic.eventhorizon.PurchaseSuccessfulEvent
import com.automattic.eventhorizon.Trackable
import javax.inject.Inject

class FirebaseAnalyticsTracker @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalyticsWrapper,
    private val settings: Settings,
) : AnalyticsTracker {
    override val id get() = ID

    override fun track(event: Trackable): TrackedEvent? {
        if (!settings.collectAnalytics.value || event::class.java !in EVENTS) {
            return null
        }

        val name = ANALYTIC_EVENT_TO_FIREBASE_NAME[event::class.java] ?: event.analyticsName
        val bundle = Bundle().apply {
            event.analyticsProperties.forEach { (key, value) ->
                putString(key, value.toString())
            }
        }
        firebaseAnalytics.logEvent(name, bundle)
        return TrackedEvent(
            key = name,
            properties = event.analyticsProperties,
        )
    }

    override fun refreshMetadata() = Unit

    override fun flush() = Unit

    override fun clearAllData() = Unit

    private companion object {
        private const val ID = "Firebase"

        private val EVENTS = listOf(
            DiscoverFeaturedPodcastSubscribedEvent::class.java,
            DiscoverFeaturedPodcastTappedEvent::class.java,
            DiscoverListEpisodePlayEvent::class.java,
            DiscoverListEpisodeTappedEvent::class.java,
            DiscoverListImpressionEvent::class.java,
            DiscoverListPodcastSubscribedEvent::class.java,
            DiscoverListPodcastTappedEvent::class.java,
            DiscoverListShowAllTappedEvent::class.java,
            DiscoverListShowAllTappedEvent::class.java,
            PurchaseSuccessfulEvent::class.java,
            PlusPromotionShownEvent::class.java,
            PlusPromotionDismissedEvent::class.java,
            PlusPromotionNotNowButtonTappedEvent::class.java,
            PlaybackPlayEvent::class.java,
            PodcastSubscribedEvent::class.java,
            BannerAdImpressionEvent::class.java,
            BannerAdTappedEvent::class.java,
            BannerAdReportEvent::class.java,
        )

        // Firebase event names that are different to Tracks
        private val ANALYTIC_EVENT_TO_FIREBASE_NAME = mapOf(
            DiscoverListEpisodeTappedEvent::class.java to "discover_list_podcast_episode_tap",
            DiscoverListPodcastSubscribedEvent::class.java to "discover_list_podcast_subscribe",
            DiscoverListPodcastTappedEvent::class.java to "discover_list_podcast_tap",
            DiscoverListShowAllTappedEvent::class.java to "discover_list_show_all",
        )
    }
}
