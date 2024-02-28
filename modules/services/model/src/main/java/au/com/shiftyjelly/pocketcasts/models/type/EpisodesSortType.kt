package au.com.shiftyjelly.pocketcasts.models.type

enum class EpisodesSortType(
    val serverId: Int,
) {
    EPISODES_SORT_BY_TITLE_ASC(
        serverId = 0,
    ),
    EPISODES_SORT_BY_TITLE_DESC(
        serverId = 1,
    ),
    EPISODES_SORT_BY_DATE_ASC(
        serverId = 2,
    ),
    EPISODES_SORT_BY_DATE_DESC(
        serverId = 3,
    ),
    EPISODES_SORT_BY_LENGTH_ASC(
        serverId = 4,
    ),
    EPISODES_SORT_BY_LENGTH_DESC(
        serverId = 5,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}
