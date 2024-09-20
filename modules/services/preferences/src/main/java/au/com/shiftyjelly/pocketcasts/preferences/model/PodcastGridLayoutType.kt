package au.com.shiftyjelly.pocketcasts.preferences.model

enum class PodcastGridLayoutType(
    val id: Int,
    val serverId: Int,
    val analyticsValue: String,
) {
    LARGE_ARTWORK(id = 0, serverId = 0, analyticsValue = "large_artwork"),
    SMALL_ARTWORK(id = 1, serverId = 1, analyticsValue = "small_artwork"),
    LIST_VIEW(id = 2, serverId = 2, analyticsValue = "list"),
    ;

    companion object {
        val default = LARGE_ARTWORK

        fun fromLayoutId(id: Int) = entries.find { it.id == id } ?: default

        fun fromServerId(serverId: Int) = entries.find { it.serverId == serverId } ?: default
    }
}
