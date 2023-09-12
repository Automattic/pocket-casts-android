package au.com.shiftyjelly.pocketcasts.repositories.extensions

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage

fun Podcast.getArtworkUrl(size: Int): String {
    return if (uuid == Podcast.userPodcast.uuid) thumbnailUrl ?: "" else PodcastImage.getArtworkUrl(size, uuid)
}
