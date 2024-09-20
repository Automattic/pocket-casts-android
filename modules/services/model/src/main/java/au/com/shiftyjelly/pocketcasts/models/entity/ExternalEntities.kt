package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import au.com.shiftyjelly.pocketcasts.model.BuildConfig

data class ExternalPodcast(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "episode_count") val episodeCount: Int,
    @ColumnInfo(name = "initial_release_timestamp") val initialReleaseTimestampMs: Long?,
    @ColumnInfo(name = "latest_release_timestamp") val latestReleaseTimestampMs: Long?,
    @ColumnInfo(name = "last_used_timestamp") val lastUsedTimestampMs: Long?,
    @ColumnInfo(name = "podcast_category") private val _categories: String,
) {
    val coverSize get() = 960
    val coverUrl get() = podcastCover(id)
    val categories get() = _categories.split('\n')
}

data class ExternalPodcastOrUserEpisode(
    @ColumnInfo(name = "is_podcast_episode") val isPodcastEpisode: Boolean,
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "duration") val durationMs: Long,
    @ColumnInfo(name = "current_position") val playbackPositionMs: Long,
    @ColumnInfo(name = "release_timestamp") val releaseTimestampMs: Long,
    @ColumnInfo(name = "is_downloaded") val isDownloaded: Boolean,
    @ColumnInfo(name = "is_video") val isVideo: Boolean,
    @ColumnInfo(name = "podcast_id") val podcastId: String?,
    @ColumnInfo(name = "podcast_title") val podcastTitle: String?,
    @ColumnInfo(name = "season_number") val seasonNumber: Int?,
    @ColumnInfo(name = "episode_number") val episodeNumber: Int?,
    @ColumnInfo(name = "last_used_timestamp") val lastUsedTimestampMs: Long?,
    @ColumnInfo(name = "artwork_url") val artworkUrl: String?,
    @ColumnInfo(name = "tint_color_index") val tintColorIndex: Int?,
) {
    fun toExternalEpisode() = if (isPodcastEpisode) {
        podcastId?.let { podcastId ->
            podcastTitle?.let { podcastTitle ->
                ExternalEpisode.Podcast(
                    id = id,
                    title = title,
                    releaseTimestampMs = releaseTimestampMs,
                    lastUsedTimestampMs = lastUsedTimestampMs,
                    durationMs = durationMs,
                    playbackPositionMs = playbackPositionMs,
                    isDownloaded = isDownloaded,
                    isVideo = isVideo,
                    podcastId = podcastId,
                    podcastTitle = podcastTitle,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                )
            }
        }
    } else {
        tintColorIndex?.let { tintColorIndex ->
            ExternalEpisode.User(
                id = id,
                title = title,
                releaseTimestampMs = releaseTimestampMs,
                durationMs = durationMs,
                playbackPositionMs = playbackPositionMs,
                isDownloaded = isDownloaded,
                isVideo = isVideo,
                artworkUrl = artworkUrl,
                tintColorIndex = tintColorIndex,
            )
        }
    }
}

sealed interface ExternalEpisode {
    val id: String
    val title: String
    val releaseTimestampMs: Long
    val durationMs: Long
    val playbackPositionMs: Long
    val isDownloaded: Boolean
    val isVideo: Boolean

    val coverSize get() = 960
    val coverUrl: String

    data class Podcast(
        @ColumnInfo(name = "id") override val id: String,
        @ColumnInfo(name = "title") override val title: String,
        @ColumnInfo(name = "release_timestamp") override val releaseTimestampMs: Long,
        @ColumnInfo(name = "duration") override val durationMs: Long,
        @ColumnInfo(name = "current_position") override val playbackPositionMs: Long,
        @ColumnInfo(name = "is_downloaded") override val isDownloaded: Boolean,
        @ColumnInfo(name = "is_video") override val isVideo: Boolean,
        @ColumnInfo(name = "last_used_timestamp") val lastUsedTimestampMs: Long?,
        @ColumnInfo(name = "podcast_id") val podcastId: String,
        @ColumnInfo(name = "podcast_title") val podcastTitle: String,
        @ColumnInfo(name = "season_number") val seasonNumber: Int?,
        @ColumnInfo(name = "episode_number") val episodeNumber: Int?,
    ) : ExternalEpisode {
        override val coverUrl get() = podcastCover(podcastId)
        val percentComplete get() = ((playbackPositionMs.toDouble() / durationMs.coerceAtLeast(1)) * 100).coerceIn(0.0..100.0)
    }

    data class User(
        override val id: String,
        override val title: String,
        override val releaseTimestampMs: Long,
        override val durationMs: Long,
        override val playbackPositionMs: Long,
        override val isDownloaded: Boolean,
        override val isVideo: Boolean,
        val artworkUrl: String?,
        val tintColorIndex: Int,
    ) : ExternalEpisode {
        override val coverUrl get() = artworkUrl?.takeIf { tintColorIndex == 0 } ?: "${BuildConfig.SERVER_STATIC_URL}/discover/images/artwork/dark/960/$tintColorIndex.png"
    }
}

data class ExternalPodcastMap(
    private val map: Map<String, ExternalPodcastList>,
    private val limitPerGroup: Int,
) {
    fun trendingGroup(limit: Int = limitPerGroup) = map[CuratedPodcast.TRENDING_LIST_ID]?.let {
        if (limit != limitPerGroup) {
            it.copy(podcasts = it.podcasts.take(limit))
        } else {
            it
        }
    }

    fun featuruedGroup(limit: Int = limitPerGroup) = map[CuratedPodcast.FEATURED_LIST_ID]?.let {
        if (limit != limitPerGroup) {
            it.copy(podcasts = it.podcasts.take(limit))
        } else {
            it
        }
    }

    fun genericGroups(limit: Int = limitPerGroup) = (map - CuratedPodcast.specialListIds).let {
        if (limit != limitPerGroup) {
            it.mapValues { (_, group) -> group.copy(podcasts = group.podcasts.take(limit)) }
        } else {
            it
        }
    }
}

data class ExternalPodcastList(
    val id: String,
    val title: String,
    val podcasts: List<ExternalPodcastView>,
)

data class ExternalPodcastView(
    val id: String,
    val title: String,
    val description: String?,
) {
    val coverSize get() = 960
    val coverUrl get() = podcastCover(id)
}

private fun podcastCover(podcastId: String) = "${BuildConfig.SERVER_STATIC_URL}/discover/images/webp/960/$podcastId.webp"
