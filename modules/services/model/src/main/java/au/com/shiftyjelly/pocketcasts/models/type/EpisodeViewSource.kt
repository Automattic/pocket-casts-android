package au.com.shiftyjelly.pocketcasts.models.type

enum class EpisodeViewSource(val value: String) {
    DISCOVER("discover"),
    FILTERS("filters"),
    PODCAST_SCREEN("podcast_screen"),
    STARRED("starred"),
    DOWNLOADS("downloads"),
    LISTENING_HISTORY("listening_history"),
    UP_NEXT("up_next"),
    SHARE("share"),
    NOTIFICATION("notification"),
    NOTIFICATION_BOOKMARK("notification_bookmark"),
    NOVA_LAUNCHER_NEW_RELEASES("nova_launcher_new_releases"),
    NOVA_LAUNCHER_IN_PROGRESS("nova_launcher_in_progress"),
    NOVA_LAUNCHER_QUEUE("nova_launcher_queue"),
    SEARCH("search"),
    SEARCH_HISTORY("search_history"),
    NOW_PLAYING("now_playing"),
    UNKNOWN("unknown"),
    ;

    companion object {
        fun fromString(source: String?) =
            EpisodeViewSource.values().find { it.value == source } ?: UNKNOWN
    }
}
