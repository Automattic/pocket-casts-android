package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode

open class AutomaticUpNextSource(val uuid: String) {

    companion object {
        var mostRecentList: String? = null

        object Predefined {
            const val downloads = "downloads"
            const val files = "files"
            const val starred = "starred"
        }

        fun create(): AutomaticUpNextSource? = mostRecentList?.let { AutomaticUpNextSource(it) }
        fun create(episode: PodcastEpisode): AutomaticUpNextSource = AutomaticUpNextSource(episode.podcastUuid)
    }
}
