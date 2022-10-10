package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsPropValue
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist

sealed interface FilterUpdatedEvent {
    val groupValue: AnalyticsPropValue
}

sealed class PlaylistProperty {

    data class AutoDownload(private val enabled: Boolean) : PlaylistProperty() {
        val enabledValue = AnalyticsPropValue(enabled)
    }

    data class AutoDownloadLimit(private val limit: Int) : PlaylistProperty() {
        val limitValue = AnalyticsPropValue(limit)
    }

    object Color : PlaylistProperty()

    object Downloaded : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = AnalyticsPropValue("download_status")
    }

    object Duration : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = AnalyticsPropValue("episode_duration")
    }

    object EpisodeStatus : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = AnalyticsPropValue("episode_status")
    }

    object FilterName : PlaylistProperty()

    object Icon : PlaylistProperty()

    object MediaType : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = AnalyticsPropValue("media_type")
    }

    object Podcasts : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = AnalyticsPropValue("podcasts")
    }

    object ReleaseDate : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = AnalyticsPropValue("release_date")
    }

    data class Sort(private val sortOrder: Playlist.SortOrder) : PlaylistProperty() {
        val sortOrderValue = AnalyticsPropValue(
            when (sortOrder) {
                Playlist.SortOrder.NEWEST_TO_OLDEST -> "newest_to_oldest"
                Playlist.SortOrder.OLDEST_TO_NEWEST -> "oldest_to_newest"
                Playlist.SortOrder.SHORTEST_TO_LONGEST -> "shortest_to_longest"
                Playlist.SortOrder.LONGEST_TO_SHORTEST -> "longest_to_shortest"
                Playlist.SortOrder.LAST_DOWNLOAD_ATTEMPT_DATE -> "last_download_attempt_date"
            }
        )
    }

    object Starred : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = AnalyticsPropValue("starred")
    }
}
