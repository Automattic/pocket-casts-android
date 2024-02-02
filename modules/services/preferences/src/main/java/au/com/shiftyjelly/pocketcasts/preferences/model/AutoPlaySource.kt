package au.com.shiftyjelly.pocketcasts.preferences.model

import java.util.UUID

sealed interface AutoPlaySource {
    val id: String

    data class PodcastOrFilter(
        val uuid: String,
    ) : AutoPlaySource {
        override val id get() = uuid
    }

    data object Downloads : AutoPlaySource {
        override val id = "downloads"
    }

    data object Files : AutoPlaySource {
        override val id = "files"
    }

    data object Starred : AutoPlaySource {
        override val id = "starred"
    }

    data object None : AutoPlaySource {
        override val id = ""
    }

    companion object {
        private val Constants = listOf(None, Downloads, Files, Starred)

        fun fromId(id: String) = when {
            runCatching { UUID.fromString(id) }.isSuccess -> PodcastOrFilter(id)
            else -> Constants.find { it.id == id } ?: None
        }
    }
}
