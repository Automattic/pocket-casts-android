package au.com.shiftyjelly.pocketcasts.discover

import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory

sealed interface TvDiscoverRow {
    val id: String
    val title: String

    data class FeaturedPodcasts(
        override val id: String,
        override val title: String,
        val podcasts: List<TvDiscoverPodcast>,
        val listId: String? = null,
        val listDatetime: String? = null,
    ) : TvDiscoverRow

    data class Categories(
        override val id: String,
        override val title: String,
        val categories: List<DiscoverCategory>,
    ) : TvDiscoverRow

    data class Banner(
        override val id: String,
        override val title: String,
        val banner: TvDiscoverBanner,
    ) : TvDiscoverRow

    data class SinglePodcast(
        override val id: String,
        override val title: String,
        val podcasts: List<TvDiscoverPodcast>,
        val listId: String? = null,
        val listDatetime: String? = null,
    ) : TvDiscoverRow

    data class Podcasts(
        override val id: String,
        override val title: String,
        val podcasts: List<TvDiscoverPodcast>,
        val listId: String? = null,
        val listDatetime: String? = null,
    ) : TvDiscoverRow

    data class Episodes(
        override val id: String,
        override val title: String,
        val episodes: List<TvDiscoverEpisode>,
        val listId: String? = null,
        val listDatetime: String? = null,
    ) : TvDiscoverRow
}

enum class TvDiscoverBanner(val id: String) {
    CreateAccount("create_account"),
    DiscoverMore("discover_more"),
    ;

    companion object {
        fun fromId(id: String?): TvDiscoverBanner? = entries.firstOrNull { it.id == id }
    }
}

data class TvCategoryPodcasts(
    val listId: String?,
    val podcasts: List<TvDiscoverPodcast>,
)

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
    val videoPreviewUrl: String? = null,
) {
    val thumbnailUrl: String = PodcastImage.getArtworkUrl(size = 960, uuid = podcastUuid, isWearOS = false)
    val podcastArtworkUrl: String = PodcastImage.getArtworkUrl(size = 200, uuid = podcastUuid, isWearOS = false)
}
