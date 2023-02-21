package au.com.shiftyjelly.pocketcasts.servers.discover

import java.io.Serializable

data class GlobalServerSearch(
    val searchTerm: String = "",
    val error: Throwable? = null,
    val podcastSearch: PodcastSearch = PodcastSearch(),
    val episodeSearch: EpisodeSearch = EpisodeSearch(),
) : Serializable
