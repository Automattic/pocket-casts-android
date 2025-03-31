package au.com.shiftyjelly.pocketcasts.analytics

import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.preferences.di.PublicSharedPreferences
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class AppsFlyerAnalyticsTracker @Inject constructor(
    private val appsFlyerAnalytics: AppsFlyerAnalyticsWrapper,
    @PublicSharedPreferences preferences: SharedPreferences,
) : IdentifyingTracker(preferences), CoroutineScope {
    companion object {
        private const val ANON_ID = "anon_id_apps_flyer_anon_id"
        private val EVENTS = listOf(
            AnalyticsEvent.USER_SIGNED_IN,
            AnalyticsEvent.USER_ACCOUNT_CREATED,
            AnalyticsEvent.SSO_STARTED,
            AnalyticsEvent.PURCHASE_SUCCESSFUL,
            AnalyticsEvent.PURCHASE_CANCELLED,
            AnalyticsEvent.SETUP_ACCOUNT_SHOWN,
            AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
            AnalyticsEvent.SETUP_ACCOUNT_DISMISSED,
            AnalyticsEvent.SIGNIN_SHOWN,
            AnalyticsEvent.SIGNIN_DISMISSED,
            AnalyticsEvent.CREATE_ACCOUNT_SHOWN,
            AnalyticsEvent.CREATE_ACCOUNT_DISMISSED,
            AnalyticsEvent.CREATE_ACCOUNT_NEXT_BUTTON_TAPPED,
            AnalyticsEvent.SELECT_PAYMENT_FREQUENCY_SHOWN,
            AnalyticsEvent.SELECT_PAYMENT_FREQUENCY_DISMISSED,
            AnalyticsEvent.SELECT_PAYMENT_FREQUENCY_NEXT_BUTTON_TAPPED,
            AnalyticsEvent.PODCASTS_LIST_SHOWN,
            AnalyticsEvent.PODCASTS_TAB_OPENED,
            AnalyticsEvent.FILTERS_TAB_OPENED,
            AnalyticsEvent.DISCOVER_TAB_OPENED,
            AnalyticsEvent.PROFILE_TAB_OPENED,
            AnalyticsEvent.UP_NEXT_TAB_OPENED,
            AnalyticsEvent.PROFILE_SHOWN,
            AnalyticsEvent.PODCAST_SCREEN_SHOWN,
            AnalyticsEvent.PLAYBACK_PLAY,
            AnalyticsEvent.FILTER_LIST_SHOWN,
            AnalyticsEvent.DISCOVER_SHOWN,
            AnalyticsEvent.PODCAST_SUBSCRIBED,
        )

        private fun shouldTrack(event: AnalyticsEvent) =
            EVENTS.contains(event)
    }

    override val anonIdPrefKey = ANON_ID
    override val coroutineContext: CoroutineContext = Dispatchers.IO

    override fun track(event: AnalyticsEvent, properties: Map<String, Any>) {
        if (!shouldTrack(event)) {
            return
        }

        launch {
            val userId = anonID ?: generateNewAnonID()
            appsFlyerAnalytics.logEvent(event.key, properties, userId)
            Timber.i("AppsFlyer analytic event: ${event.key} properties: $properties")
        }
    }

    override fun getTrackerType() = TrackerType.ThirdParty
    override fun refreshMetadata() {}
    override fun flush() {}
    override fun clearAllData() {}
}
