package au.com.shiftyjelly.pocketcasts.servers.discover

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import java.io.Serializable

data class PodcastSearch(
    var searchResults: MutableList<Podcast> = mutableListOf(),
    var searchEpisodesResults: MutableList<EpisodeItem> = mutableListOf(),
    var searchTerm: String = "",
    var isUrl: Boolean = false,
    val error: Throwable? = null
) : Serializable
