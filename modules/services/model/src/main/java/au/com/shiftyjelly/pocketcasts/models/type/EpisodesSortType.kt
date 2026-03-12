package au.com.shiftyjelly.pocketcasts.models.type

import com.automattic.eventhorizon.EpisodeSortType

enum class EpisodesSortType(
    val serverId: Int,
    val analyticsValue: EpisodeSortType,
) {
    EPISODES_SORT_BY_TITLE_ASC(
        serverId = 0,
        analyticsValue = EpisodeSortType.TitleAToZ,
    ),
    EPISODES_SORT_BY_TITLE_DESC(
        serverId = 1,
        analyticsValue = EpisodeSortType.TitleZToA,
    ),
    EPISODES_SORT_BY_DATE_ASC(
        serverId = 2,
        analyticsValue = EpisodeSortType.OldestToNewest,
    ),
    EPISODES_SORT_BY_DATE_DESC(
        serverId = 3,
        analyticsValue = EpisodeSortType.NewestToOldest,
    ),
    EPISODES_SORT_BY_LENGTH_ASC(
        serverId = 4,
        analyticsValue = EpisodeSortType.ShortestToLongest,
    ),
    EPISODES_SORT_BY_LENGTH_DESC(
        serverId = 5,
        analyticsValue = EpisodeSortType.LongestToShortest,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}
