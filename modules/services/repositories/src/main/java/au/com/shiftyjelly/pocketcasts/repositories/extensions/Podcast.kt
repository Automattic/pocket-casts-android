package au.com.shiftyjelly.pocketcasts.repositories.extensions

import au.com.shiftyjelly.pocketcasts.models.db.helper.UserEpisodePodcastSubstitute
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage

val Podcast.largeArtworkUrl: String
    get() = getArtworkUrl(960)

fun Podcast.getArtworkUrl(size: Int): String {
    return if (uuid == UserEpisodePodcastSubstitute.substituteUuid) thumbnailUrl ?: "" else PodcastImage.getArtworkUrl(size, uuid)
}

fun Podcast.getArtworkJpgUrl(size: Int): String {
    return if (uuid == UserEpisodePodcastSubstitute.substituteUuid) thumbnailUrl ?: "" else PodcastImage.getArtworkJpgUrl(size, uuid)
}
