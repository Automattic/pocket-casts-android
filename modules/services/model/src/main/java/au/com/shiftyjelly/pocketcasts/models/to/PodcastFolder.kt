package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

data class PodcastFolder(
    val podcast: Podcast,
    val folder: Folder? = null
)
