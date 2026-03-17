package au.com.shiftyjelly.pocketcasts.analytics

import com.automattic.eventhorizon.SourceViewType

enum class SourceView(
    val key: String,
    val analyticsValue: SourceViewType,
) {
    AUTO_PAUSE(
        key = "auto_pause",
        analyticsValue = SourceViewType.AutoPause,
    ),
    AUTO_PLAY(
        key = "auto_play",
        analyticsValue = SourceViewType.AutoPlay,
    ),
    AUTO_DOWNLOAD(
        key = "auto_download",
        analyticsValue = SourceViewType.AutoDownload,
    ),
    BOTTOM_SHELF(
        key = "bottom_shelf",
        analyticsValue = SourceViewType.BottomShelf,
    ),
    CLIP_SHARING(
        key = "clip_sharing",
        analyticsValue = SourceViewType.ClipSharing,
    ),
    CHROMECAST(
        key = "chromecast",
        analyticsValue = SourceViewType.Chromecast,
    ),
    DISCOVER(
        key = "discover",
        analyticsValue = SourceViewType.Discover,
    ),
    DISCOVER_PLAIN_LIST(
        key = "discover_plain_list",
        analyticsValue = SourceViewType.DiscoverPlainList,
    ),
    DISCOVER_PODCAST_LIST(
        key = "discover_podcast_list",
        analyticsValue = SourceViewType.DiscoverPodcastList,
    ),
    DISCOVER_RANKED_LIST(
        key = "discover_ranked_list",
        analyticsValue = SourceViewType.DiscoverRankedList,
    ),
    DOWNLOADS(
        key = "downloads",
        analyticsValue = SourceViewType.Downloads,
    ),
    ENGAGE_SDK_CONTINUATION(
        key = "engage_sdk_continuation",
        analyticsValue = SourceViewType.EngageSdkContinuation,
    ),
    ENGAGE_SDK_FEATURED(
        key = "engage_sdk_featured",
        analyticsValue = SourceViewType.EngageSdkFeatured,
    ),
    ENGAGE_SDK_RECOMMENDATIONS(
        key = "engage_sdk_recommendations",
        analyticsValue = SourceViewType.EngageSdkRecommendations,
    ),
    ENGAGE_SDK_SIGN_IN(
        key = "engage_sdk_sign_in",
        analyticsValue = SourceViewType.EngageSdkSignIn,
    ),
    EPISODE_DETAILS(
        key = "episode_details",
        analyticsValue = SourceViewType.EpisodeDetails,
    ),
    EPISODE_SWIPE_ACTION(
        key = "episode_swipe_action",
        analyticsValue = SourceViewType.EpisodeSwipeAction,
    ),
    EPISODE_TRANSCRIPT(
        key = "episode_transcript",
        analyticsValue = SourceViewType.EpisodeTranscript,
    ),
    FILES(
        key = "files",
        analyticsValue = SourceViewType.Files,
    ),
    FILES_SETTINGS(
        key = "files_settings",
        analyticsValue = SourceViewType.FilesSettings,
    ),
    FILTERS(
        key = "filters",
        analyticsValue = SourceViewType.Filters,
    ),
    FULL_SCREEN_VIDEO(
        key = "full_screen_video",
        analyticsValue = SourceViewType.FullScreenVideo,
    ),
    LISTENING_HISTORY(
        key = "listening_history",
        analyticsValue = SourceViewType.ListeningHistory,
    ),
    MEDIA_BUTTON_BROADCAST_ACTION(
        key = "media_button_broadcast_action",
        analyticsValue = SourceViewType.MediaButtonBroadcastAction,
    ),
    MEDIA_BUTTON_BROADCAST_SEARCH_ACTION(
        key = "media_button_broadcast_search_action",
        analyticsValue = SourceViewType.MediaButtonBroadcastSearchAction,
    ),
    METERED_NETWORK_CHANGE(
        key = "metered_network_change",
        analyticsValue = SourceViewType.MeteredNetworkChange,
    ),
    MINIPLAYER(
        key = "miniplayer",
        analyticsValue = SourceViewType.Miniplayer,
    ),
    MULTI_SELECT(
        key = "multi_select",
        analyticsValue = SourceViewType.MultiSelect,
    ),
    NOTIFICATION(
        key = "notification",
        analyticsValue = SourceViewType.Notification,
    ),
    NOTIFICATION_BOOKMARK(
        key = "notification_bookmark",
        analyticsValue = SourceViewType.NotificationBookmark,
    ),
    ONBOARDING_RECOMMENDATIONS(
        key = "onboarding_recommendations",
        analyticsValue = SourceViewType.OnboardingRecommendations,
    ),
    ONBOARDING_RECOMMENDATIONS_SEARCH(
        key = "onboarding_recommendations_search",
        analyticsValue = SourceViewType.OnboardingRecommendationsSearch,
    ),
    PLAYER(
        key = "player",
        analyticsValue = SourceViewType.Player,
    ),
    PLAYER_BROADCAST_ACTION(
        key = "player_broadcast_action",
        analyticsValue = SourceViewType.PlayerBroadcastAction,
    ),
    PLAYER_PLAYBACK_EFFECTS(
        key = "player_playback_effects",
        analyticsValue = SourceViewType.PlayerPlaybackEffects,
    ),
    PODCAST_LIST(
        key = "podcast_list",
        analyticsValue = SourceViewType.PodcastList,
    ),
    PODCAST_SCREEN(
        key = "podcast_screen",
        analyticsValue = SourceViewType.PodcastScreen,
    ),
    PODCAST_SETTINGS(
        key = "podcast_settings",
        analyticsValue = SourceViewType.PodcastSettings,
    ),
    PROFILE(
        key = "profile",
        analyticsValue = SourceViewType.Profile,
    ),
    REFERRALS(
        key = "referrals",
        analyticsValue = SourceViewType.Referrals,
    ),
    SEARCH(
        key = "search",
        analyticsValue = SourceViewType.Search,
    ),
    SEARCH_RESULTS(
        key = "search_results",
        analyticsValue = SourceViewType.SearchResults,
    ),
    SHARE_LIST(
        key = "share_list",
        analyticsValue = SourceViewType.ShareList,
    ),
    STARRED(
        key = "starred",
        analyticsValue = SourceViewType.Starred,
    ),
    STATS(
        key = "stats",
        analyticsValue = SourceViewType.Stats,
    ),
    TASKER(
        key = "tasker",
        analyticsValue = SourceViewType.Tasker,
    ),
    UNKNOWN(
        key = "unknown",
        analyticsValue = SourceViewType.Unknown,
    ),
    UP_NEXT(
        key = "up_next",
        analyticsValue = SourceViewType.UpNext,
    ),
    UP_NEXT_HISTORY(
        key = "up_next_history",
        analyticsValue = SourceViewType.UpNextHistory,
    ),
    WHATS_NEW(
        key = "whats_new",
        analyticsValue = SourceViewType.WhatsNew,
    ),
    ABOUT(
        key = "about",
        analyticsValue = SourceViewType.About,
    ),
    APPEARANCE(
        key = "appearance_settings",
        analyticsValue = SourceViewType.AppearanceSettings,
    ),
    STORAGE_AND_DATA_USAGE(
        key = "storage_and_data_usage",
        analyticsValue = SourceViewType.StorageAndDataUsage,
    ),
    WIDGET_PLAYER_LARGE(
        key = "widget_player_large",
        analyticsValue = SourceViewType.WidgetPlayerLarge,
    ),
    WIDGET_PLAYER_MEDIUM(
        key = "widget_player_medium",
        analyticsValue = SourceViewType.WidgetPlayerMedium,
    ),
    WIDGET_PLAYER_OLD(
        key = "widget_player_old",
        analyticsValue = SourceViewType.WidgetPlayerOld,
    ),
    WIDGET_PLAYER_SMALL(
        key = "widget_player_small",
        analyticsValue = SourceViewType.WidgetPlayerSmall,
    ),
    ;

    companion object {
        fun fromString(source: String?) = SourceView.entries.find { it.key == source } ?: UNKNOWN
    }
}
