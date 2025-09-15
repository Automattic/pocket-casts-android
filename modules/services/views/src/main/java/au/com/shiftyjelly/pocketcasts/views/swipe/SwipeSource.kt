package au.com.shiftyjelly.pocketcasts.views.swipe

enum class SwipeSource(
    val analyticsValue: String,
) {
    PodcastDetails("podcast_details"),
    Filters("filters"),
    Downloads("downloads"),
    ListeningHistory("listening_history"),
    Starred("starred"),
    Files("files"),
    UpNext("up_next"),
}
