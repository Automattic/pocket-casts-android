package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.repositories.extensions.colorIndex
import au.com.shiftyjelly.pocketcasts.repositories.extensions.drawableId
import javax.inject.Inject
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR

class PlaylistUpdateAnalytics @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) {

    fun update(
        smartPlaylist: SmartPlaylist,
        userPlaylistUpdate: UserPlaylistUpdate?,
        isCreatingFilter: Boolean,
    ) {
        when {
            isCreatingFilter -> sendPlaylistCreatedEvent(smartPlaylist)

            // If the playlist is a draft, then we are in the middle of the filter creation flow and
            // we don't want to send update events
            !smartPlaylist.draft -> sendPlaylistUpdateEvent(userPlaylistUpdate)
        }
    }

    private fun sendPlaylistCreatedEvent(smartPlaylist: SmartPlaylist) {
        val properties = buildMap<String, Any> {
            put(Key.ALL_PODCASTS, smartPlaylist.allPodcasts)
            colorAnalyticsValue(smartPlaylist)?.let {
                put(Key.COLOR, it)
            }
            put(Key.DOWNLOADED, smartPlaylist.downloaded)
            put(Key.NOT_DOWNLOADED, smartPlaylist.notDownloaded)
            put(Key.DURATION, smartPlaylist.filterDuration)
            if (smartPlaylist.filterDuration) {
                put(Key.DURATION_LONGER_THAN, smartPlaylist.longerThan)
                put(Key.DURATION_SHORTER_THAN, smartPlaylist.shorterThan)
            }
            put(Key.EPISODE_STATUS_IN_PROGRESS, smartPlaylist.partiallyPlayed)
            put(Key.EPISODE_STATUS_PLAYED, smartPlaylist.finished)
            put(Key.EPISODE_STATUS_UNPLAYED, smartPlaylist.unplayed)
            iconAnalyticsValue(smartPlaylist)?.let {
                put(Key.ICON_NAME, it)
            }
            mediaTypeAnalyticsValue(smartPlaylist)?.let {
                put(Key.MEDIA_TYPE, it)
            }
            put(Key.STARRED, smartPlaylist.starred)
            releaseDateAnalyticsValue(smartPlaylist)?.let {
                put(Key.RELEASE_DATE, it)
            }
        }

        analyticsTracker.track(AnalyticsEvent.FILTER_CREATED, properties)
    }

    private fun iconAnalyticsValue(smartPlaylist: SmartPlaylist) = when (smartPlaylist.drawableId) {
        IR.drawable.ic_filters_list -> Value.IconName.LIST
        IR.drawable.ic_filters_headphones -> Value.IconName.HEADPHONES
        IR.drawable.ic_filters_clock -> Value.IconName.CLOCK
        IR.drawable.ic_filters_download -> Value.IconName.DOWNLOADED
        IR.drawable.ic_filters_play -> Value.IconName.PLAY
        IR.drawable.ic_filters_volume -> Value.IconName.VOLUME
        IR.drawable.ic_filters_video -> Value.IconName.VIDEO
        IR.drawable.ic_filters_star -> Value.IconName.STARRED
        else -> {
            Timber.e("No matching analytics icon found")
            null
        }
    }

    private fun mediaTypeAnalyticsValue(smartPlaylist: SmartPlaylist) = when (smartPlaylist.audioVideo) {
        SmartPlaylist.AUDIO_VIDEO_FILTER_ALL -> Value.MediaType.ALL
        SmartPlaylist.AUDIO_VIDEO_FILTER_AUDIO_ONLY -> Value.MediaType.AUDIO
        SmartPlaylist.AUDIO_VIDEO_FILTER_VIDEO_ONLY -> Value.MediaType.VIDEO
        else -> {
            Timber.e("No match found for audioVideo Int")
            null
        }
    }

    private fun releaseDateAnalyticsValue(smartPlaylist: SmartPlaylist) = when (smartPlaylist.filterHours) {
        SmartPlaylist.ANYTIME -> Value.ReleaseDate.ANYTIME
        SmartPlaylist.LAST_24_HOURS -> Value.ReleaseDate.TWENTY_FOUR_HOURS
        SmartPlaylist.LAST_3_DAYS -> Value.ReleaseDate.THREE_DAYS
        SmartPlaylist.LAST_WEEK -> Value.ReleaseDate.WEEK
        SmartPlaylist.LAST_2_WEEKS -> Value.ReleaseDate.TWO_WEEKS
        SmartPlaylist.LAST_MONTH -> Value.ReleaseDate.MONTH
        else -> {
            Timber.e("Unexpected filter hours value")
            null
        }
    }

    private fun colorAnalyticsValue(smartPlaylist: SmartPlaylist) = when (smartPlaylist.colorIndex) {
        0 -> Value.Color.RED
        1 -> Value.Color.BLUE
        2 -> Value.Color.GREEN
        3 -> Value.Color.PURPLE
        4 -> Value.Color.YELLOW
        else -> {
            Timber.e("No matching analytics color found")
            null
        }
    }

    private fun sendPlaylistUpdateEvent(userPlaylistUpdate: UserPlaylistUpdate?) {
        userPlaylistUpdate?.properties?.map { playlistProperty ->
            when (playlistProperty) {
                is FilterUpdatedEvent -> {
                    val properties = mapOf(
                        Key.GROUP to playlistProperty.groupValue,
                        Key.SOURCE to userPlaylistUpdate.source.analyticsValue,
                    )
                    analyticsTracker.track(AnalyticsEvent.FILTER_UPDATED, properties)
                }

                is PlaylistProperty.AutoDownload -> {
                    val properties = mapOf(
                        Key.SOURCE to userPlaylistUpdate.source.analyticsValue,
                        Key.ENABLED to playlistProperty.enabled,
                    )
                    analyticsTracker.track(AnalyticsEvent.FILTER_AUTO_DOWNLOAD_UPDATED, properties)
                }

                is PlaylistProperty.AutoDownloadLimit -> {
                    val properties = mapOf(Key.LIMIT to playlistProperty.limit)
                    analyticsTracker.track(AnalyticsEvent.FILTER_AUTO_DOWNLOAD_LIMIT_UPDATED, properties)
                }

                is PlaylistProperty.Sort -> {
                    val sortOrderString = when (playlistProperty.sortOrder) {
                        PlaylistEpisodeSortType.NewestToOldest -> "newest_to_oldest"
                        PlaylistEpisodeSortType.OldestToNewest -> "oldest_to_newest"
                        PlaylistEpisodeSortType.ShortestToLongest -> "shortest_to_longest"
                        PlaylistEpisodeSortType.LongestToShortest -> "longest_to_shortest"
                        PlaylistEpisodeSortType.LastDownloadAttempt -> "last_download_attempt_date"
                    }
                    val properties = mapOf(Key.SORT_ORDER to sortOrderString)
                    analyticsTracker.track(AnalyticsEvent.FILTER_SORT_BY_CHANGED, properties)
                }

                PlaylistProperty.Color,
                PlaylistProperty.FilterName,
                PlaylistProperty.Icon,
                -> { /* Do nothing. These are handled by the filter_edit_dismissed event. */ }
            }
        }
    }

    companion object {
        private object Key {
            const val ALL_PODCASTS = "all_podcasts"
            const val COLOR = "color"
            const val DOWNLOADED = "downloaded"
            const val DURATION = "duration"
            const val DURATION_LONGER_THAN = "duration_longer_than"
            const val DURATION_SHORTER_THAN = "duration_shorter_than"
            const val ENABLED = "enabled"
            const val GROUP = "group"
            const val ICON_NAME = "icon_name"
            const val LIMIT = "limit"
            const val EPISODE_STATUS_IN_PROGRESS = "expisode_status_in_progress"
            const val EPISODE_STATUS_PLAYED = "episode_status_played"
            const val EPISODE_STATUS_UNPLAYED = "episode_status_unplayed"
            const val MEDIA_TYPE = "media_type"
            const val NOT_DOWNLOADED = "not_downloaded"
            const val RELEASE_DATE = "release_date"
            const val SORT_ORDER = "sort_order"
            const val SOURCE = "source"
            const val STARRED = "starred"
        }

        private object Value {
            object IconName {
                const val CLOCK = "filter_clock"
                const val DOWNLOADED = "filter_downloaded"
                const val HEADPHONES = "filter_headphones"
                const val LIST = "filter_list"
                const val PLAY = "filter_play"
                const val STARRED = "filter_starred"
                const val VOLUME = "filter_volume"
                const val VIDEO = "filter_video"
            }

            object MediaType {
                const val ALL = "all"
                const val AUDIO = "audio"
                const val VIDEO = "video"
            }

            object ReleaseDate {
                const val ANYTIME = "anytime"
                const val TWENTY_FOUR_HOURS = "24_hours"
                const val THREE_DAYS = "3_days"
                const val WEEK = "last_week"
                const val TWO_WEEKS = "last_2_Weeks"
                const val MONTH = "last_month"
            }

            object Color {
                const val RED = "red"
                const val BLUE = "blue"
                const val GREEN = "green"
                const val PURPLE = "purple"
                const val YELLOW = "yellow"
            }
        }
    }
}
