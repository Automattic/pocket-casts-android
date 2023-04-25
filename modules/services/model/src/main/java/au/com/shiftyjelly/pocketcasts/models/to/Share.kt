package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import java.io.Serializable

data class Share(
    var podcast: Podcast,
    var episode: PodcastEpisode? = null,
    var timeInSeconds: Int? = null,
    var message: String? = null
) : Serializable
