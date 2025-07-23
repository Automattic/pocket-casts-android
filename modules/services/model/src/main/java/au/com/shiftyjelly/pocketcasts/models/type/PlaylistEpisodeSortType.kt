package au.com.shiftyjelly.pocketcasts.models.type

enum class PlaylistEpisodeSortType(
    val serverId: Int,
) {
    NewestToOldest(
        serverId = 0,
    ),
    OldestToNewest(
        serverId = 1,
    ),
    ShortestToLongest(
        serverId = 2,
    ),
    LongestToShortest(
        serverId = 3,
    ),
    LastDownloadAttempt(
        serverId = 100,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}
