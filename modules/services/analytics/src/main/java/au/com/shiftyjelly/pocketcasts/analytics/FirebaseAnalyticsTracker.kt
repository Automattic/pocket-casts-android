package au.com.shiftyjelly.pocketcasts.analytics

import android.os.Bundle
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import javax.inject.Inject

class FirebaseAnalyticsTracker @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalyticsWrapper,
    private val settings: Settings,
) : Tracker {
    override val id get() = ID

    override fun shouldTrack(event: AnalyticsEvent): Boolean {
        return event in EVENTS && settings.collectAnalytics.value
    }

    override fun track(event: AnalyticsEvent, properties: Map<String, Any>): TrackedEvent {
        val name = ANALYTIC_EVENT_TO_FIREBASE_NAME[event] ?: event.key
        val bundle = Bundle().apply {
            properties.forEach { (key, value) ->
                putString(key, value.toString())
            }
        }
        firebaseAnalytics.logEvent(name, bundle)
        return TrackedEvent(event, properties, usedKey = name)
    }

    override fun refreshMetadata() = Unit

    override fun flush() = Unit

    override fun clearAllData() = Unit

    private companion object {
        private const val ID = "Firebase"

        private val EVENTS = listOf(
            AnalyticsEvent.DISCOVER_FEATURED_PODCAST_SUBSCRIBED,
            AnalyticsEvent.DISCOVER_FEATURED_PODCAST_TAPPED,
            AnalyticsEvent.DISCOVER_LIST_EPISODE_PLAY,
            AnalyticsEvent.DISCOVER_LIST_EPISODE_TAPPED,
            AnalyticsEvent.DISCOVER_LIST_IMPRESSION,
            AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED,
            AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED,
            AnalyticsEvent.DISCOVER_LIST_SHOW_ALL_TAPPED,
            AnalyticsEvent.PURCHASE_SUCCESSFUL,
            AnalyticsEvent.PLUS_PROMOTION_SHOWN,
            AnalyticsEvent.PLUS_PROMOTION_DISMISSED,
            AnalyticsEvent.PLUS_PROMOTION_NOT_NOW_BUTTON_TAPPED,
            AnalyticsEvent.PLAYBACK_PLAY,
            AnalyticsEvent.PODCAST_SUBSCRIBED,
            AnalyticsEvent.BANNER_AD_IMPRESSION,
            AnalyticsEvent.BANNER_AD_TAPPED,
            AnalyticsEvent.BANNER_AD_REPORT,
        )

        // Firebase event names that are different to Tracks
        private val ANALYTIC_EVENT_TO_FIREBASE_NAME = mapOf(
            AnalyticsEvent.DISCOVER_LIST_EPISODE_TAPPED to "discover_list_podcast_episode_tap",
            AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED to "discover_list_podcast_subscribe",
            AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED to "discover_list_podcast_tap",
            AnalyticsEvent.DISCOVER_LIST_SHOW_ALL_TAPPED to "discover_list_show_all",
        )
    }
}
