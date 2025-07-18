package au.com.shiftyjelly.pocketcasts.preferences.model

import java.util.UUID

sealed interface AutoPlaySource {
    val id: String

    // We can safely use the ID as server ID. Keeping it if need to make changes in the future.
    val serverId: String get() = id

    data class PodcastOrFilter(
        val uuid: String,
    ) : AutoPlaySource {
        override val id get() = uuid
    }

    enum class Predefined(
        override val id: String,
    ) : AutoPlaySource {
        Downloads(
            id = "downloads",
        ),
        Files(
            id = "files",
        ),
        Starred(
            id = "starred",
        ),
        None(
            id = "",
        ),
    }

    companion object {
        fun fromId(id: String) = runCatching { UUID.fromString(id) }
            .map { PodcastOrFilter(id) }
            .recover { Predefined.entries.find { it.id == id } }
            .getOrNull()
            ?: Predefined.None

        fun fromServerId(id: String) = fromId(id)
    }
}
