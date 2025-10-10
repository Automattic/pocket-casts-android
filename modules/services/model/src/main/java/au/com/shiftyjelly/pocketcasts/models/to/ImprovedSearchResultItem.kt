package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import java.util.Date
import kotlin.time.Duration

sealed interface ImprovedSearchResultItem {
    val uuid: String
    val title: String

    data class FolderItem(
        val folder: Folder,
        val podcasts: List<Podcast>,
    ) : ImprovedSearchResultItem {
        override val uuid: String get() = folder.uuid
        override val title: String get() = folder.name
    }

    data class PodcastItem(
        override val uuid: String,
        override val title: String,
        val author: String,
        val isFollowed: Boolean,
    ) : ImprovedSearchResultItem

    data class EpisodeItem(
        override val uuid: String,
        override val title: String,
        val podcastUuid: String,
        val publishedDate: Date,
        val duration: Duration,
    ) : ImprovedSearchResultItem
}
