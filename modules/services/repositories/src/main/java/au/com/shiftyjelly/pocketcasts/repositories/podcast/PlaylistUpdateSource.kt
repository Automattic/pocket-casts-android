package au.com.shiftyjelly.pocketcasts.repositories.podcast

enum class PlaylistUpdateSource(val analyticsValue: String) {
    AUTO_DOWNLOAD_SETTINGS("auto_download_settings"),

    // These both use the same analytics key
    FILTER_EPISODE_LIST("filters"),
    FILTER_OPTIONS("filters"),

    PODCAST_SETTINGS("podcast_settings")
}
