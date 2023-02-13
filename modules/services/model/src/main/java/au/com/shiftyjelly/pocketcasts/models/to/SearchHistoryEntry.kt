package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.entity.SearchHistoryItem
import java.util.Date
import au.com.shiftyjelly.pocketcasts.models.entity.Episode as EpisodeModel
import au.com.shiftyjelly.pocketcasts.models.entity.Folder as FolderModel
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast as PodcastModel

sealed class SearchHistoryEntry(
    val id: Long? = null,
    open val isSubscribed: Boolean = false,
) {
    class Episode(
        id: Long? = null,
        val uuid: String,
        val title: String,
        val publishedDate: Date,
        val duration: Double,
    ) : SearchHistoryEntry(id = id)

    class Folder(
        id: Long? = null,
        override val isSubscribed: Boolean,
        val uuid: String,
        val title: String,
        val color: Int,
        val podcastIds: List<String>,
    ) : SearchHistoryEntry(id = id, isSubscribed = isSubscribed)

    class Podcast(
        id: Long? = null,
        override val isSubscribed: Boolean,
        val uuid: String,
        val title: String,
        val author: String,
    ) : SearchHistoryEntry(id = id, isSubscribed = isSubscribed)

    class SearchTerm(
        id: Long? = null,
        val term: String,
    ) : SearchHistoryEntry(id = id)

    fun toSearchHistoryItem() = when (this) {
        is Episode -> SearchHistoryItem(
            id = id,
            episode = SearchHistoryItem.Episode(
                uuid = uuid,
                title = title,
                publishedDate = publishedDate,
                duration = duration
            )
        )

        is Folder -> SearchHistoryItem(
            id = id,
            isSubscribed = isSubscribed,
            folder = SearchHistoryItem.Folder(
                uuid = uuid,
                title = title,
                color = color,
                podcastIds = podcastIds.joinToString(separator = ",")
            )
        )

        is Podcast -> SearchHistoryItem(
            id = id,
            isSubscribed = isSubscribed,
            podcast = SearchHistoryItem.Podcast(
                uuid = uuid,
                title = title,
                author = author
            )
        )

        is SearchTerm -> SearchHistoryItem(
            id = id,
            term = term
        )
    }

    companion object {
        fun fromEpisode(episode: EpisodeModel) = Episode(
            uuid = episode.uuid,
            title = episode.title,
            publishedDate = episode.publishedDate,
            duration = episode.duration
        )

        fun fromFolder(folder: FolderModel, podcastIds: List<String>) = Folder(
            uuid = folder.uuid,
            title = folder.name,
            color = folder.color,
            podcastIds = podcastIds,
            isSubscribed = true,
        )

        fun fromPodcast(podcast: PodcastModel) = Podcast(
            uuid = podcast.uuid,
            title = podcast.title,
            author = podcast.author,
            isSubscribed = podcast.isSubscribed
        )

        fun fromSearchHistoryItem(item: SearchHistoryItem) = when {
            item.episode != null -> {
                val episode = item.episode as SearchHistoryItem.Episode
                Episode(
                    id = item.id,
                    uuid = episode.uuid,
                    title = episode.title,
                    publishedDate = episode.publishedDate,
                    duration = episode.duration
                )
            }

            item.folder != null -> {
                val folder = item.folder as SearchHistoryItem.Folder
                Folder(
                    id = item.id,
                    isSubscribed = item.isSubscribed,
                    uuid = folder.uuid,
                    title = folder.title,
                    color = folder.color,
                    podcastIds = folder.podcastIds.split(","),
                )
            }

            item.podcast != null -> {
                val podcast = item.podcast as SearchHistoryItem.Podcast
                Podcast(
                    id = item.id,
                    isSubscribed = item.isSubscribed,
                    uuid = podcast.uuid,
                    title = podcast.title,
                    author = podcast.author
                )
            }

            item.term != null -> SearchTerm(id = item.id, term = item.term as String)

            else -> throw IllegalStateException("Unknown search history item")
        }
    }
}
