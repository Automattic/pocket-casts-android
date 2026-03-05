package au.com.shiftyjelly.pocketcasts.models.type

import com.automattic.eventhorizon.EpisodeSortType

enum class EpisodesSortType(
    val serverId: Int,
    val eventHorizonValue: EpisodeSortType,
) {
    EPISODES_SORT_BY_TITLE_ASC(
        serverId = 0,
        eventHorizonValue = EpisodeSortType.TitleAToZ,
    ),
    EPISODES_SORT_BY_TITLE_DESC(
        serverId = 1,
        eventHorizonValue = EpisodeSortType.TitleZToA,
    ),
    EPISODES_SORT_BY_DATE_ASC(
        serverId = 2,
        eventHorizonValue = EpisodeSortType.OldestToNewest,
    ),
    EPISODES_SORT_BY_DATE_DESC(
        serverId = 3,
        eventHorizonValue = EpisodeSortType.NewestToOldest,
    ),
    EPISODES_SORT_BY_LENGTH_ASC(
        serverId = 4,
        eventHorizonValue = EpisodeSortType.ShortestToLongest,
    ),
    EPISODES_SORT_BY_LENGTH_DESC(
        serverId = 5,
        eventHorizonValue = EpisodeSortType.LongestToShortest,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}
