package au.com.shiftyjelly.pocketcasts.models.type

import com.automattic.eventhorizon.EpisodeViewSourceType

enum class EpisodeViewSource(
    val key: String,
    val analyticsValue: EpisodeViewSourceType,
) {
    DISCOVER(
        key = "discover",
        analyticsValue = EpisodeViewSourceType.Discover,
    ),
    ENGAGE_SDK_CONTINUATION(
        key = "engage_sdk_continuation",
        analyticsValue = EpisodeViewSourceType.EngageSdkContinuation,
    ),
    ENGAGE_SDK_FEATURED(
        key = "engage_sdk_featured",
        analyticsValue = EpisodeViewSourceType.EngageSdkFeatured,
    ),
    ENGAGE_SDK_RECOMMENDATIONS(
        key = "engage_sdk_recommendations",
        analyticsValue = EpisodeViewSourceType.EngageSdkRecommendations,
    ),
    FILES(
        key = "files",
        analyticsValue = EpisodeViewSourceType.Files,
    ),
    FILTERS(
        key = "filters",
        analyticsValue = EpisodeViewSourceType.Filters,
    ),
    PODCAST_SCREEN(
        key = "podcast_screen",
        analyticsValue = EpisodeViewSourceType.PodcastScreen,
    ),
    STARRED(
        key = "starred",
        analyticsValue = EpisodeViewSourceType.Starred,
    ),
    DOWNLOADS(
        key = "downloads",
        analyticsValue = EpisodeViewSourceType.Downloads,
    ),
    LISTENING_HISTORY(
        key = "listening_history",
        analyticsValue = EpisodeViewSourceType.ListeningHistory,
    ),
    UP_NEXT(
        key = "up_next",
        analyticsValue = EpisodeViewSourceType.UpNext,
    ),
    SHARE(
        key = "share",
        analyticsValue = EpisodeViewSourceType.Share,
    ),
    NOTIFICATION(
        key = "notification",
        analyticsValue = EpisodeViewSourceType.Notification,
    ),
    NOTIFICATION_BOOKMARK(
        key = "notification_bookmark",
        analyticsValue = EpisodeViewSourceType.NotificationBookmark,
    ),
    SEARCH(
        key = "search",
        analyticsValue = EpisodeViewSourceType.Search,
    ),
    SEARCH_HISTORY(
        key = "search_history",
        analyticsValue = EpisodeViewSourceType.SearchHistory,
    ),
    NOW_PLAYING(
        key = "now_playing",
        analyticsValue = EpisodeViewSourceType.NowPlaying,
    ),
    UNKNOWN(
        key = "unknown",
        analyticsValue = EpisodeViewSourceType.Unknown,
    ),
    ;

    companion object {
        fun fromString(source: String?) = EpisodeViewSource.entries.find { it.key == source } ?: UNKNOWN
    }
}
