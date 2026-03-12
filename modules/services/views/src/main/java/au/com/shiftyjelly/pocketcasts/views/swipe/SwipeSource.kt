package au.com.shiftyjelly.pocketcasts.views.swipe

import com.automattic.eventhorizon.SwipeSourceType

enum class SwipeSource(
    val analyticsValue: SwipeSourceType,
) {
    PodcastDetails(
        analyticsValue = SwipeSourceType.PodcastDetails,
    ),
    Filters(
        analyticsValue = SwipeSourceType.Filters,
    ),
    Downloads(
        analyticsValue = SwipeSourceType.Downloads,
    ),
    ListeningHistory(
        analyticsValue = SwipeSourceType.ListeningHistory,
    ),
    Starred(
        analyticsValue = SwipeSourceType.Starred,
    ),
    Files(
        analyticsValue = SwipeSourceType.Files,
    ),
    UpNext(
        analyticsValue = SwipeSourceType.UpNext,
    ),
}
