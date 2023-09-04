package au.com.shiftyjelly.pocketcasts.preferences.model

enum class PodcastGridLayoutType(val id: Int, val analyticsValue: String) {
    LARGE_ARTWORK(id = 0, analyticsValue = "large_artwork"),
    SMALL_ARTWORK(id = 1, analyticsValue = "small_artwork"),
    LIST_VIEW(id = 2, analyticsValue = "list");

    companion object {
        val default = LARGE_ARTWORK
        fun fromLayoutId(id: Int) =
            PodcastGridLayoutType.values().find { it.id == id } ?: default
    }
}
