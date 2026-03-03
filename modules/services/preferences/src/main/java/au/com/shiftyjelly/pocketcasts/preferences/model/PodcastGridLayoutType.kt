package au.com.shiftyjelly.pocketcasts.preferences.model

import com.automattic.eventhorizon.PodcastListLayoutType

enum class PodcastGridLayoutType(
    val id: Int,
    val serverId: Int,
    val eventHorizonValue: PodcastListLayoutType,
) {
    LARGE_ARTWORK(
        id = 0,
        serverId = 0,
        eventHorizonValue = PodcastListLayoutType.LargeArtwork,
    ),
    SMALL_ARTWORK(
        id = 1,
        serverId = 1,
        eventHorizonValue = PodcastListLayoutType.SmallArtwork,
    ),
    LIST_VIEW(
        id = 2,
        serverId = 2,
        eventHorizonValue = PodcastListLayoutType.List,
    ),
    ;

    val analyticsValue get() = eventHorizonValue.toString()

    companion object {
        val default = LARGE_ARTWORK

        fun fromLayoutId(id: Int) = entries.find { it.id == id } ?: default

        fun fromServerId(serverId: Int) = entries.find { it.serverId == serverId } ?: default
    }
}
