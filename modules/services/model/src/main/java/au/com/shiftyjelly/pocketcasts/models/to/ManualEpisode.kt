package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.Embedded
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode

sealed interface ManualEpisode {
    val uuid: String

    data class Available(val episode: PodcastEpisode) : ManualEpisode {
        override val uuid get() = episode.uuid
    }

    data class Unavailable(val episode: ManualPlaylistEpisode) : ManualEpisode {
        override val uuid get() = episode.episodeUuid
    }
}

internal class RawManualEpisode(
    @Embedded(prefix = "m_") val manualEpisode: ManualPlaylistEpisode,
    @Embedded(prefix = "p_") val podcastEpisode: PodcastEpisode?,
) {
    fun toEpisode() = if (podcastEpisode != null) {
        ManualEpisode.Available(podcastEpisode)
    } else {
        ManualEpisode.Unavailable(manualEpisode)
    }
}
