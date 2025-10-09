package au.com.shiftyjelly.pocketcasts.models.to

sealed interface ImprovedSearchResultItem {
    data class FolderItem(
        val uuid: String,
    ) : ImprovedSearchResultItem

    data class PodcastItem(
        val uuid: String,
        val isFollowed: Boolean,
    ) : ImprovedSearchResultItem

    data class EpisodeItem(
        val uuid: String
    ) : ImprovedSearchResultItem
}