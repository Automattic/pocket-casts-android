package au.com.shiftyjelly.pocketcasts.models.type

enum class PlaylistEpisodeSortType(
    val serverId: Int,
    val analyticsValue: String,
) {
    NewestToOldest(
        serverId = 0,
        analyticsValue = "newest_to_oldest",
    ),
    OldestToNewest(
        serverId = 1,
        analyticsValue = "oldest_to_newest",
    ),
    ShortestToLongest(
        serverId = 2,
        analyticsValue = "shortest_to_longest",
    ),
    LongestToShortest(
        serverId = 3,
        analyticsValue = "longest_to_shortest",
    ),
    DragAndDrop(
        serverId = 4,
        analyticsValue = "drag_and_drop",
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}
