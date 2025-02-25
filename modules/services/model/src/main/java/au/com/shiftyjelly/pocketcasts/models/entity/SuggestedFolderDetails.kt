package au.com.shiftyjelly.pocketcasts.models.entity

import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType

data class SuggestedFolderDetails(
    val uuid: String,
    val name: String,
    val podcasts: List<String>,
    val color: Int,
    var podcastsSortType: PodcastsSortType,
)
