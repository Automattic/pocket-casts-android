package au.com.shiftyjelly.pocketcasts.models.type

import com.automattic.eventhorizon.PlaylistEpisodeSortType as EventHorizonSortType

enum class PlaylistEpisodeSortType(
    val serverId: Int,
    val analyticsValue: EventHorizonSortType,
) {
    NewestToOldest(
        serverId = 0,
        analyticsValue = EventHorizonSortType.NewestToOldest,
    ),
    OldestToNewest(
        serverId = 1,
        analyticsValue = EventHorizonSortType.OldestToNewest,
    ),
    ShortestToLongest(
        serverId = 2,
        analyticsValue = EventHorizonSortType.ShortestToLongest,
    ),
    LongestToShortest(
        serverId = 3,
        analyticsValue = EventHorizonSortType.LongestToShortest,
    ),
    DragAndDrop(
        serverId = 4,
        analyticsValue = EventHorizonSortType.DragAndDrop,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}
