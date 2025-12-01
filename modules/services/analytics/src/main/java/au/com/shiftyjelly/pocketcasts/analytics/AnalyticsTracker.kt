package au.com.shiftyjelly.pocketcasts.analytics

class AnalyticsTracker(
    private val trackers: Set<Tracker>,
    private val listeners: Set<Listener>,
) {
    fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) {
        val trackedEvents = trackers.associate { tracker ->
            val trackedEvent = if (tracker.shouldTrack(event)) {
                tracker.track(event, properties)
            } else {
                null
            }
            tracker.id to trackedEvent
        }
        listeners.forEach { listener ->
            listener.onEvent(event, properties, trackedEvents)
        }
    }

    private fun trackWithFlow(event: AnalyticsEvent, flow: String, properties: Map<String, Any> = emptyMap()) {
        track(
            event = event,
            properties = buildMap {
                put(AnalyticsParameter.FLOW, flow)
                if (properties.isNotEmpty()) {
                    putAll(properties)
                }
            },
        )
    }

    fun trackBannerAdImpression(id: String, location: String) {
        track(
            AnalyticsEvent.BANNER_AD_IMPRESSION,
            mapOf(
                "id" to id,
                "location" to location,
            ),
        )
    }

    fun trackBannerAdTapped(id: String, location: String) {
        track(
            AnalyticsEvent.BANNER_AD_TAPPED,
            mapOf(
                "id" to id,
                "location" to location,
            ),
        )
    }

    fun trackBannerAdReport(id: String, reason: String, location: String) {
        track(
            AnalyticsEvent.BANNER_AD_REPORT,
            mapOf(
                "id" to id,
                "reason" to reason,
                "location" to location,
            ),
        )
    }

    fun trackOnboardingIntroCarouselShown(flow: String) {
        trackWithFlow(event = AnalyticsEvent.ONBOARDING_INTRO_CAROUSEL_SHOWN, flow = flow)
    }

    fun trackOnboardingGetStarted(flow: String) {
        trackWithFlow(
            event = AnalyticsEvent.ONBOARDING_GET_STARTED,
            flow = flow,
            properties = mapOf(AnalyticsParameter.BUTTON to "get_started"),
        )
    }

    fun trackInterestsShown(flow: String) {
        trackWithFlow(event = AnalyticsEvent.INTERESTS_SHOWN, flow = flow)
    }

    fun trackInterestsNotNowTapped(flow: String) {
        trackWithFlow(event = AnalyticsEvent.INTERESTS_NOT_NOW_TAPPED, flow = flow)
    }

    fun trackInterestsShowMoreTapped(flow: String) {
        trackWithFlow(event = AnalyticsEvent.INTERESTS_SHOW_MORE_TAPPED, flow = flow)
    }

    fun trackInterestsCategorySelected(flow: String, categoryId: Int, categoryName: String, isSelected: Boolean) {
        trackWithFlow(
            event = AnalyticsEvent.INTERESTS_CATEGORY_SELECTED,
            flow = flow,
            properties = mapOf(
                AnalyticsParameter.CATEGORY_ID to categoryId,
                AnalyticsParameter.NAME to categoryName,
                AnalyticsParameter.IS_SELECTED to isSelected,
            ),
        )
    }

    fun trackInterestsContinueTapped(flow: String, categories: List<String>) {
        trackWithFlow(
            event = AnalyticsEvent.INTERESTS_CONTINUE_TAPPED,
            flow = flow,
            properties = mapOf(
                AnalyticsParameter.CATEGORIES to categories.joinToString(", "),
            ),
        )
    }

    fun trackRecommendationsShown(flow: String) {
        trackWithFlow(event = AnalyticsEvent.RECOMMENDATIONS_SHOWN, flow = flow)
    }

    fun trackRecommendationsSearchTapped(flow: String) {
        trackWithFlow(event = AnalyticsEvent.RECOMMENDATIONS_SEARCH_TAPPED, flow = flow)
    }

    fun trackRecommendationsImportTapped(flow: String) {
        trackWithFlow(event = AnalyticsEvent.RECOMMENDATIONS_IMPORT_TAPPED, flow = flow)
    }

    fun trackRecommendationsDismissed(flow: String, subscriptions: Int) {
        trackWithFlow(
            event = AnalyticsEvent.RECOMMENDATIONS_DISMISSED,
            flow = flow,
            properties = mapOf(
                AnalyticsParameter.SUBSCRIPTIONS to subscriptions,
            ),
        )
    }

    fun trackRecommendationsContinueTapped(flow: String, subscriptions: Int) {
        trackWithFlow(
            event = AnalyticsEvent.RECOMMENDATIONS_CONTINUE_TAPPED,
            flow = flow,
            properties = mapOf(
                AnalyticsParameter.SUBSCRIPTIONS to subscriptions,
            ),
        )
    }

    fun trackPodcastSubscribed(flow: String, podcastUuid: String, source: String) {
        trackWithFlow(
            event = AnalyticsEvent.PODCAST_SUBSCRIBED,
            flow = flow,
            properties = mapOf(
                AnalyticsParameter.UUID to podcastUuid,
                AnalyticsParameter.SOURCE to source,
            ),
        )
    }

    fun trackPodcastUnsubscribed(flow: String, podcastUuid: String, source: String) {
        trackWithFlow(
            event = AnalyticsEvent.PODCAST_UNSUBSCRIBED,
            flow = flow,
            properties = mapOf(
                AnalyticsParameter.UUID to podcastUuid,
                AnalyticsParameter.SOURCE to source,
            ),
        )
    }

    fun trackSetupAccountButtonTapped(flow: String, button: AnalyticsParameter.SetupAccountButton) {
        trackWithFlow(
            event = AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
            flow = flow,
            properties = mapOf(
                AnalyticsParameter.BUTTON to button.value,
            ),
        )
    }

    fun trackSetupAccountDismissed(flow: String) {
        trackWithFlow(event = AnalyticsEvent.SETUP_ACCOUNT_DISMISSED, flow = flow)
    }

    fun trackCreateAccountDismissed(flow: String) {
        trackWithFlow(event = AnalyticsEvent.CREATE_ACCOUNT_DISMISSED, flow = flow)
    }

    fun trackCreateAccountShown(flow: String) {
        trackWithFlow(event = AnalyticsEvent.CREATE_ACCOUNT_SHOWN, flow = flow)
    }

    fun trackSsoStartedGoogle() {
        track(
            AnalyticsEvent.SSO_STARTED,
            mapOf(
                AnalyticsParameter.SOURCE to "google",
            ),
        )
    }

    fun trackSetupAccountShown(flow: String) {
        trackWithFlow(event = AnalyticsEvent.SETUP_ACCOUNT_SHOWN, flow = flow)
    }

    fun trackSignInButtonTapped(flow: String) {
        trackWithFlow(
            event = AnalyticsEvent.SIGNIN_BUTTON_TAPPED,
            flow = flow,
            properties = mapOf(
                AnalyticsParameter.BUTTON to "sign_in",
            ),
        )
    }

    fun trackSignInForgotPasswordTapped(flow: String) {
        trackWithFlow(event = AnalyticsEvent.SIGNIN_FORGOT_PASSWORD_TAPPED, flow = flow)
    }

    fun trackEndOfYearModalShown(year: Int) {
        track(
            event = AnalyticsEvent.END_OF_YEAR_MODAL_SHOWN,
            properties = mapOf(
                AnalyticsParameter.YEAR to year,
            ),
        )
    }

    fun trackEndOfYearModalTapped(year: Int) {
        track(
            event = AnalyticsEvent.END_OF_YEAR_MODAL_TAPPED,
            properties = mapOf(
                AnalyticsParameter.YEAR to year,
            ),
        )
    }

    fun trackEndOfYearModalDismissed(year: Int) {
        track(
            event = AnalyticsEvent.END_OF_YEAR_MODAL_DISMISSED,
            properties = mapOf(
                AnalyticsParameter.YEAR to year,
            ),
        )
    }

    fun refreshMetadata() {
        trackers.forEach(Tracker::refreshMetadata)
    }

    fun flush() {
        trackers.forEach(Tracker::flush)
    }

    fun clearAllData() {
        trackers.forEach(Tracker::clearAllData)
    }

    interface Listener {
        fun onEvent(
            event: AnalyticsEvent,
            properties: Map<String, Any>,
            trackedEvents: Map<String, TrackedEvent?>,
        )
    }

    companion object {
        fun test(vararg trackers: Tracker) = AnalyticsTracker(
            trackers = trackers.toSet(),
            listeners = emptySet(),
        )
    }
}
