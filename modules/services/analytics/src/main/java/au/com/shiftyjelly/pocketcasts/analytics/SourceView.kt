package au.com.shiftyjelly.pocketcasts.analytics

enum class SourceView(val analyticsValue: String) {
    AUTO_PAUSE("auto_pause"),
    AUTO_PLAY("auto_play"),
    BOTTOM_SHELF("bottom_shelf"),
    CLIP_SHARING("clip_sharing"),
    CHROMECAST("chromecast"),
    DISCOVER("discover"),
    DISCOVER_PLAIN_LIST("discover_plain_list"),
    DISCOVER_PODCAST_LIST("discover_podcast_list"),
    DISCOVER_RANKED_LIST("discover_ranked_list"),
    DOWNLOADS("downloads"),
    ENGAGE_SDK_CONTINUATION("engage_sdk_continuation"),
    ENGAGE_SDK_FEATURED("engage_sdk_featured"),
    ENGAGE_SDK_RECOMMENDATIONS("engage_sdk_recommendations"),
    EPISODE_DETAILS("episode_details"),
    EPISODE_SWIPE_ACTION("episode_swipe_action"),
    FILES("files"),
    FILTERS("filters"),
    FULL_SCREEN_VIDEO("full_screen_video"),
    LISTENING_HISTORY("listening_history"),
    MEDIA_BUTTON_BROADCAST_ACTION("media_button_broadcast_action"),
    MEDIA_BUTTON_BROADCAST_SEARCH_ACTION("media_button_broadcast_search_action"),
    METERED_NETWORK_CHANGE("metered_network_change"),
    MINIPLAYER("miniplayer"),
    MULTI_SELECT("multi_select"),
    NOTIFICATION("notification"),
    NOTIFICATION_BOOKMARK("notification_bookmark"),
    NOVA_LAUNCHER_RECENTLY_PLAYED("nova_launcher_recently_played"),
    NOVA_LAUNCHER_SUBSCRIBED_PODCASTS("nova_launcher_subscribed_podcasts"),
    NOVA_LAUNCHER_TRENDING_PODCASTS("nova_launcher_trending_podcasts"),
    ONBOARDING_RECOMMENDATIONS("onboarding_recommendations"),
    ONBOARDING_RECOMMENDATIONS_SEARCH("onboarding_recommendations_search"),
    PLAYER("player"),
    PLAYER_BROADCAST_ACTION("player_broadcast_action"),
    PLAYER_PLAYBACK_EFFECTS("player_playback_effects"),
    PODCAST_LIST("podcast_list"),
    PODCAST_SCREEN("podcast_screen"),
    PODCAST_SETTINGS("podcast_settings"),
    PROFILE("profile"),
    SEARCH("search"),
    SEARCH_RESULTS("search_results"),
    SHARE_LIST("share_list"),
    STARRED("starred"),
    STATS("stats"),
    TASKER("tasker"),
    UNKNOWN("unknown"),
    UP_NEXT("up_next"),
    WHATS_NEW("whats_new"),
    WIDGET_PLAYER_LARGE("widget_player_large"),
    WIDGET_PLAYER_MEDIUM("widget_player_medium"),
    WIDGET_PLAYER_OLD("widget_player_old"),
    WIDGET_PLAYER_SMALL("widget_player_small"),
    ;

    fun skipTracking() = this in listOf(AUTO_PLAY, AUTO_PAUSE)

    companion object {
        fun fromString(source: String?) =
            SourceView.entries.find { it.analyticsValue == source } ?: UNKNOWN
    }
}
