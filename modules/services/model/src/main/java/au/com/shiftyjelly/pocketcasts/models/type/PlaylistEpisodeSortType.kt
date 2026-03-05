package au.com.shiftyjelly.pocketcasts.models.type

import com.automattic.eventhorizon.PlaylistEpisodeSortType as EventHorizonSortType

enum class PlaylistEpisodeSortType(
    val serverId: Int,
    val eventHorizonValue: EventHorizonSortType,
) {
    NewestToOldest(
        serverId = 0,
        eventHorizonValue = EventHorizonSortType.NewestToOldest,
    ),
    OldestToNewest(
        serverId = 1,
        eventHorizonValue = EventHorizonSortType.OldestToNewest,
    ),
    ShortestToLongest(
        serverId = 2,
        eventHorizonValue = EventHorizonSortType.ShortestToLongest,
    ),
    LongestToShortest(
        serverId = 3,
        eventHorizonValue = EventHorizonSortType.LongestToShortest,
    ),
    DragAndDrop(
        serverId = 4,
        eventHorizonValue = EventHorizonSortType.DragAndDrop,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}
