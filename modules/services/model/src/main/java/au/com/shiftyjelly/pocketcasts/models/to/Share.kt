package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import java.io.Serializable

data class Share(
    var podcast: Podcast,
    var episode: Episode? = null,
    var timeInSeconds: Int? = null,
    var message: String? = null
) : Serializable
