package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.extensions.colorIndex
import au.com.shiftyjelly.pocketcasts.repositories.extensions.drawableId
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR

class PlaylistUpdateAnalytics @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {

    fun update(
        playlist: Playlist,
        userPlaylistUpdate: UserPlaylistUpdate?,
        isCreatingFilter: Boolean
    ) {

        when {
            isCreatingFilter -> {
                sendPlaylistCreatedEvent(playlist)
            }

            // If the playlist is a draft, then we are in the middle of the filter creation flow and
            // we don't want to send update events
            !playlist.draft -> {
                sendPlaylistUpdateEvent(userPlaylistUpdate)
            }
        }
    }

    private fun sendPlaylistCreatedEvent(playlist: Playlist) {
        val properties = buildMap<String, Any> {

            val icon = when (playlist.drawableId) {

                IR.drawable.ic_filters_list,
                IR.drawable.auto_filter_list,
                IR.drawable.automotive_filter_list -> AnalyticsProp.Value.Icon.LIST

                IR.drawable.ic_filters_headphones,
                IR.drawable.auto_filter_headphones,
                IR.drawable.automotive_filter_headphones -> AnalyticsProp.Value.Icon.HEADPHONES

                IR.drawable.ic_filters_clock,
                IR.drawable.auto_filter_clock,
                IR.drawable.automotive_filter_clock -> AnalyticsProp.Value.Icon.CLOCK

                IR.drawable.ic_filters_download,
                IR.drawable.auto_filter_downloaded,
                IR.drawable.automotive_filter_downloaded -> AnalyticsProp.Value.Icon.DOWNLOADED

                IR.drawable.ic_filters_play,
                IR.drawable.auto_filter_play,
                IR.drawable.automotive_filter_play -> AnalyticsProp.Value.Icon.PLAY

                IR.drawable.ic_filters_volume,
                IR.drawable.auto_filter_volume,
                IR.drawable.automotive_filter_volume -> AnalyticsProp.Value.Icon.VOLUME

                IR.drawable.ic_filters_video,
                IR.drawable.auto_filter_video,
                IR.drawable.automotive_filter_video -> AnalyticsProp.Value.Icon.VIDEO

                IR.drawable.ic_filters_star,
                IR.drawable.auto_filter_star,
                IR.drawable.automotive_filter_star -> AnalyticsProp.Value.Icon.STARRED

                else -> {
                    Timber.e("No matching icon found")
                    null
                }
            }

            val mediaType = when (playlist.audioVideo) {
                0 -> AnalyticsProp.Value.MediaType.ALL
                1 -> AnalyticsProp.Value.MediaType.AUDIO
                2 -> AnalyticsProp.Value.MediaType.VIDEO
                else -> {
                    Timber.e("No match found for audioVideo Int")
                    null
                }
            }

            val releaseDate = when (playlist.filterHours) {
                Playlist.ANYTIME -> AnalyticsProp.Value.ReleaseDate.ANYTIME
                Playlist.LAST_24_HOURS -> AnalyticsProp.Value.ReleaseDate.TWENTY_FOUR_HOURS
                Playlist.LAST_3_DAYS -> AnalyticsProp.Value.ReleaseDate.THREE_DAYS
                Playlist.LAST_WEEK -> AnalyticsProp.Value.ReleaseDate.WEEK
                Playlist.LAST_2_WEEKS -> AnalyticsProp.Value.ReleaseDate.TWO_WEEKS
                Playlist.LAST_MONTH -> AnalyticsProp.Value.ReleaseDate.MONTH
                else -> {
                    Timber.e("Unexpected filter hours value")
                    null
                }
            }

            put(AnalyticsProp.Key.ALL_PODCASTS, playlist.allPodcasts)
            put(AnalyticsProp.Key.COLOR, playlist.colorIndex)
            put(AnalyticsProp.Key.DOWNLOADED, playlist.downloaded)
            put(AnalyticsProp.Key.DURATION, playlist.filterDuration)
            put(AnalyticsProp.Key.EPISODE_STATUS_IN_PROGRESS, playlist.partiallyPlayed)
            put(AnalyticsProp.Key.EPISODE_STATUS_PLAYED, playlist.finished)
            put(AnalyticsProp.Key.EPISODE_STATUS_UNPLAYED, playlist.unplayed)
            icon?.let { put(AnalyticsProp.Key.ICON_NAME, it) }
            mediaType?.let { put(AnalyticsProp.Key.MEDIA_TYPE, it) }
            put(AnalyticsProp.Key.STARRED, playlist.starred)
            releaseDate?.let { put(AnalyticsProp.Key.RELEASE_DATE, it) }
        }

        analyticsTracker.track(AnalyticsEvent.FILTER_CREATED, properties)
    }

    private fun sendPlaylistUpdateEvent(userPlaylistUpdate: UserPlaylistUpdate?) {
        userPlaylistUpdate?.properties?.map { playlistProperty ->
            when (playlistProperty) {

                is FilterUpdatedEvent -> {
                    val properties = mapOf(
                        AnalyticsProp.Key.GROUP to playlistProperty.groupValue,
                        AnalyticsProp.Key.SOURCE to userPlaylistUpdate.source.analyticsValue
                    )
                    analyticsTracker.track(AnalyticsEvent.FILTER_UPDATED, properties)
                }

                is PlaylistProperty.AutoDownload -> {
                    val properties = mapOf(
                        AnalyticsProp.Key.SOURCE to userPlaylistUpdate.source.analyticsValue,
                        AnalyticsProp.Key.ENABLED to playlistProperty.enabled
                    )
                    analyticsTracker.track(AnalyticsEvent.FILTER_AUTO_DOWNLOAD_UPDATED, properties)
                }

                is PlaylistProperty.AutoDownloadLimit -> {
                    val properties = mapOf(AnalyticsProp.Key.LIMIT to playlistProperty.limit)
                    analyticsTracker.track(AnalyticsEvent.FILTER_AUTO_DOWNLOAD_LIMIT_UPDATED, properties)
                }

                is PlaylistProperty.Sort -> {
                    val sortOrderString = when (playlistProperty.sortOrder) {
                        Playlist.SortOrder.NEWEST_TO_OLDEST -> "newest_to_oldest"
                        Playlist.SortOrder.OLDEST_TO_NEWEST -> "oldest_to_newest"
                        Playlist.SortOrder.SHORTEST_TO_LONGEST -> "shortest_to_longest"
                        Playlist.SortOrder.LONGEST_TO_SHORTEST -> "longest_to_shortest"
                        Playlist.SortOrder.LAST_DOWNLOAD_ATTEMPT_DATE -> "last_download_attempt_date"
                    }
                    val properties = mapOf(AnalyticsProp.Key.SORT_ORDER to sortOrderString)
                    analyticsTracker.track(AnalyticsEvent.FILTER_SORT_BY_CHANGED, properties)
                }

                PlaylistProperty.Color,
                PlaylistProperty.FilterName,
                PlaylistProperty.Icon -> { /* Do nothing. These are handled by the filter_edit_dismissed event. */ }
            }
        }
    }

    companion object {
        private object AnalyticsProp {
            object Key {
                const val ALL_PODCASTS = "all_podcasts"
                const val COLOR = "color"
                const val DOWNLOADED = "downloaded"
                const val DURATION = "duration"
                const val ENABLED = "enabled"
                const val GROUP = "group"
                const val ICON_NAME = "icon_name"
                const val LIMIT = "limit"
                const val EPISODE_STATUS_IN_PROGRESS = "expisode_status_in_progress"
                const val EPISODE_STATUS_PLAYED = "episode_status_played"
                const val EPISODE_STATUS_UNPLAYED = "episode_status_unplayed"
                const val MEDIA_TYPE = "media_type"
                const val RELEASE_DATE = "release_date"
                const val SORT_ORDER = "sort_order"
                const val SOURCE = "source"
                const val STARRED = "starred"
            }

            object Value {
                object Icon {
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
                    const val TWENTY_FOUR_HOURS = "24 hours"
                    const val THREE_DAYS = "3 days"
                    const val WEEK = "Last Week"
                    const val TWO_WEEKS = "Last 2 Weeks"
                    const val MONTH = "Last Month"
                }
            }
        }
    }
}
