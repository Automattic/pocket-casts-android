package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode

class AutomaticUpNextSource private constructor(val uuid: String?) {

    constructor() : this(mostRecentList)
    constructor(episode: PodcastEpisode) : this(episode.podcastUuid)

    companion object {
        var mostRecentList: String? = null

        object Predefined {
            const val downloads = "downloads"
            const val files = "files"
            const val starred = "starred"
        }
    }
}
