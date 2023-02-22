package au.com.shiftyjelly.pocketcasts.servers.discover

import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import java.io.Serializable

data class EpisodeSearch(
    val episodes: List<EpisodeItem> = emptyList(),
) : Serializable
