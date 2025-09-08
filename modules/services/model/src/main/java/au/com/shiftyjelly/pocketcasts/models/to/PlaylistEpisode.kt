package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.Embedded
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode

sealed interface PlaylistEpisode {
    val uuid: String

    fun toPodcastEpisode(): PodcastEpisode?

    data class Available(val episode: PodcastEpisode) : PlaylistEpisode {
        override val uuid get() = episode.uuid

        override fun toPodcastEpisode() = episode
    }

    data class Unavailable(val episode: ManualPlaylistEpisode) : PlaylistEpisode {
        override val uuid get() = episode.episodeUuid

        override fun toPodcastEpisode() = null
    }
}

fun List<PlaylistEpisode>.toPodcastEpisodes() = mapNotNull(PlaylistEpisode::toPodcastEpisode)

internal data class RawManualEpisode(
    @Embedded(prefix = "m_") val manualEpisode: ManualPlaylistEpisode,
    @Embedded(prefix = "p_") val podcastEpisode: PodcastEpisode?,
) {
    fun toEpisode() = if (podcastEpisode != null) {
        PlaylistEpisode.Available(podcastEpisode)
    } else {
        PlaylistEpisode.Unavailable(manualEpisode)
    }
}
