package au.com.shiftyjelly.pocketcasts.analytics

import android.os.Bundle
import androidx.core.os.bundleOf
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Event
import com.google.firebase.analytics.FirebaseAnalytics.Param
import timber.log.Timber

object FirebaseAnalyticsTracker {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var settings: Settings
    fun setup(analytics: FirebaseAnalytics, settings: Settings) {
        firebaseAnalytics = analytics
        this.settings = settings
    }

    fun openedFeaturedPodcast() {
        logEvent("featured_podcast_clicked")
    }

    fun subscribedToFeaturedPodcast() {
        logEvent("featured_podcast_subscribed")
    }

    fun podcastEpisodePlayedFromList(listId: String, podcastUuid: String) {
        val params = Bundle()
        params.putString("list_id", listId)
        params.putString("podcast_uuid", podcastUuid)

        logEvent("discover_list_episode_play", params)
    }

    fun podcastEpisodeTappedFromList(listId: String, podcastUuid: String, episodeUuid: String) {
        val params = Bundle().apply {
            putString("list_id", listId)
            putString("podcast_uuid", podcastUuid)
            putString("episode_uuid", episodeUuid)
        }
        logEvent("discover_list_podcast_episode_tap", params)
    }

    fun podcastSubscribedFromList(listId: String, podcastUuid: String) {
        val params = Bundle()
        params.putString("list_id", listId)
        params.putString("podcast_uuid", podcastUuid)

        logEvent("discover_list_podcast_subscribe", params)
    }

    fun podcastTappedFromList(listId: String, podcastUuid: String) {
        val params = Bundle()
        params.putString("list_id", listId)
        params.putString("podcast_uuid", podcastUuid)

        logEvent("discover_list_podcast_tap", params)
    }

    fun listShowAllTapped(listId: String) {
        val params = Bundle()
        params.putString("list_id", listId)

        logEvent("discover_list_show_all", params)
    }

    fun listImpression(listId: String) {
        val params = Bundle()
        params.putString("list_id", listId)

        logEvent("discover_list_impression", params)
    }

    fun listShared(listId: String) {
        val params = Bundle()
        params.putString("list_id", listId)

        logEvent("discover_list_shared", params)
    }

    fun navigatedToPodcasts() {
        logEvent("podcast_tab_open")
    }

    fun navigatedToFilters() {
        logEvent("filter_tab_open")
    }

    fun navigatedToDiscover() {
        logEvent("discover_open")
    }

    fun navigatedToProfile() {
        logEvent("profile_tab_open")
    }

    fun userGuideOpened() {
        logEvent("user_guide_opened")
    }

    fun userGuideEmailSupport() {
        logEvent("user_guide_email")
    }

    fun userGuideEmailFeedback() {
        logEvent("user_guide_feedback")
    }

    fun statusReportSent() {
        logEvent("status_report")
    }

    fun longPressedEpisodeButton() {
        logEvent("long_pressed_episode_btn")
    }

    fun enteredMultiSelect() {
        logEvent("entered_multi_select")
    }

    fun playedEpisode() {
        logEvent("played_episode")
    }

    fun subscribedToPodcast() {
        logEvent("subscribed_to_podcast")
    }

    fun tourStarted(tourName: String) {
        logEvent("${tourName}_tour_started")
    }

    fun tourCompleted(tourName: String) {
        logEvent("${tourName}_tour_completed")
    }

    fun tourCancelled(tourName: String, atStep: Int) {
        logEvent("${tourName}_tour_cancelled_$atStep")
    }

    fun openedCategory(categoryId: Int, region: String) {
        logEvent(
            "category_open",
            bundle = bundleOf(
                "id" to categoryId,
                "region" to region
            )
        )

        logEvent("category_page_open_$categoryId")
    }

    fun nowPlayingOpen() {
        logEvent("now_playing_open")
    }

    fun openedPlayerNotes() {
        logEvent("now_playing_notes_open")
    }

    fun openedPlayerChapters() {
        logEvent("now_playing_chapters_open")
    }

    fun openedUpNext() {
        logEvent("up_next_open")
    }

    fun openedPodcast(podcastUuid: String) {
        logEvent("podcast_open", bundleOf("podcastUuid" to podcastUuid))
    }

    fun openedEpisode(podcastUuid: String, episodeUuid: String) {
        logEvent("episode_open", bundleOf("podcastUuid" to podcastUuid, "episodeUuid" to episodeUuid))
    }

    fun openedFilter() {
        logEvent("filter_opened")
    }

    fun accountDeleted() {
        logEvent("account_deleted")
    }

    fun podcastFeedRefreshed() {
        logEvent("podcast_feed_refreshed")
    }

    fun foregroundServiceStartNotAllowedException() {
        logEvent("foreground_service_exception")
    }

    fun plusUpgradeViewed(promotionId: String, promotionName: String) {
        logEvent(
            Event.VIEW_PROMOTION,
            bundleOf(
                Param.PROMOTION_ID to promotionId,
                Param.PROMOTION_NAME to promotionName
            )
        )
    }

    fun plusUpgradeConfirmed(promotionId: String, promotionName: String) {
        logEvent(
            Event.SELECT_PROMOTION,
            bundleOf(
                Param.PROMOTION_ID to promotionId,
                Param.PROMOTION_NAME to promotionName
            )
        )
    }

    fun plusUpgradeClosed(promotionId: String, promotionName: String) {
        logEvent(
            "close_promotion",
            bundleOf(
                Param.PROMOTION_ID to promotionId,
                Param.PROMOTION_NAME to promotionName
            )
        )
    }

    fun closeAccountMissingClicked() {
        logEvent("close_account_missing")
    }

    fun createAccountClicked() {
        logEvent("select_create_account")
    }

    fun signInAccountClicked() {
        logEvent("select_sign_in_account")
    }

    fun plusPlanChosen(sku: String, title: String, price: Double?, currency: String?, isFreeTrial: Boolean) {
        val plan = bundleOf(
            Param.ITEM_ID to sku,
            Param.ITEM_NAME to title,
            Param.PRICE to price,
            Param.QUANTITY to 1
        )
        if (isFreeTrial) {
            plan.putString(Param.COUPON, "FREE TRIAL")
        }
        logEvent(
            Event.ADD_TO_CART,
            bundleOf(
                Param.CURRENCY to currency,
                Param.VALUE to price,
                Param.ITEMS to arrayOf(plan)
            )
        )
    }

    fun plusPurchased() {
        logEvent(Event.PURCHASE)
    }

    fun folderCreated() {
        logEvent("folder_created")
    }

    private fun logEvent(name: String, bundle: Bundle? = Bundle()) {
        if (settings.collectAnalytics.value) {
            firebaseAnalytics.logEvent(name, bundle)

            Timber.d("Analytic event $name $bundle")
        }
    }
}
