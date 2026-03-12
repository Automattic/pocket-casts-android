package au.com.shiftyjelly.pocketcasts.views.swipe

import com.automattic.eventhorizon.SwipeSourceType

enum class SwipeSource(
    val eventHorizonValue: SwipeSourceType,
) {
    PodcastDetails(
        eventHorizonValue = SwipeSourceType.PodcastDetails,
    ),
    Filters(
        eventHorizonValue = SwipeSourceType.Filters,
    ),
    Downloads(
        eventHorizonValue = SwipeSourceType.Downloads,
    ),
    ListeningHistory(
        eventHorizonValue = SwipeSourceType.ListeningHistory,
    ),
    Starred(
        eventHorizonValue = SwipeSourceType.Starred,
    ),
    Files(
        eventHorizonValue = SwipeSourceType.Files,
    ),
    UpNext(
        eventHorizonValue = SwipeSourceType.UpNext,
    ),
}
