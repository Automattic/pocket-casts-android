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
    SEARCH("search"),
    SEARCH_HISTORY("search_history"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(source: String?) =
            EpisodeViewSource.values().find { it.value == source } ?: UNKNOWN
    }
}
