package au.com.shiftyjelly.pocketcasts.preferences.model

import java.util.UUID

sealed interface AutoPlaySource {
    val id: String
    val analyticsValue: String

    data class PodcastOrFilter(
        val uuid: String,
    ) : AutoPlaySource {
        override val id get() = uuid

        override val analyticsValue: String
            get() = "podcast_or_filter_$uuid"
    }

    enum class Predefined(
        override val id: String,
        override val analyticsValue: String,
    ) : AutoPlaySource {
        Downloads(
            id = "downloads",
            analyticsValue = "downloads",
        ),
        Files(
            id = "files",
            analyticsValue = "files",
        ),
        Starred(
            id = "starred",
            analyticsValue = "starred",
        ),
        None(
            id = "",
            analyticsValue = "none",
        ),
    }

    companion object {
        fun fromId(id: String) = runCatching { UUID.fromString(id) }
            .map { PodcastOrFilter(id) }
            .recover { Predefined.entries.find { it.id == id } }
            .getOrNull()
            ?: Predefined.None
    }
}
