package au.com.shiftyjelly.pocketcasts.discover

import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage

sealed interface TvDiscoverRow {
    val id: String
    val title: String

    data class FeaturedPodcasts(
        override val id: String,
        override val title: String,
        val podcasts: List<TvDiscoverPodcast>,
    ) : TvDiscoverRow

    data class SinglePodcast(
        override val id: String,
        override val title: String,
        val podcasts: List<TvDiscoverPodcast>,
    ) : TvDiscoverRow

    data class Podcasts(
        override val id: String,
        override val title: String,
        val podcasts: List<TvDiscoverPodcast>,
    ) : TvDiscoverRow

    data class Episodes(
        override val id: String,
        override val title: String,
        val episodes: List<TvDiscoverEpisode>,
    ) : TvDiscoverRow
}

data class TvDiscoverPodcast(
    val uuid: String,
    val title: String,
    val author: String,
    val description: String,
    val isSponsored: Boolean = false,
) {
    val artworkUrl: String = PodcastImage.getMediumArtworkUrl(uuid)
}

data class TvDiscoverEpisode(
    val episodeUuid: String,
    val episodeTitle: String,
    val podcastUuid: String,
    val podcastTitle: String,
) {
    val thumbnailUrl: String = PodcastImage.getArtworkUrl(size = 960, uuid = podcastUuid, isWearOS = false)
    val podcastArtworkUrl: String = PodcastImage.getArtworkUrl(size = 200, uuid = podcastUuid, isWearOS = false)
}
