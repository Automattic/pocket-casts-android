package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Playlist

sealed interface FilterUpdatedEvent {
    val groupValue: String
}

sealed class PlaylistProperty {

    data class AutoDownload(val enabled: Boolean) : PlaylistProperty()

    data class AutoDownloadLimit(val limit: Int) : PlaylistProperty()

    object Color : PlaylistProperty()

    object Downloaded : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue: String = "download_status"
    }

    object Duration : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = "episode_duration"
    }

    object EpisodeStatus : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = "episode_status"
    }

    object FilterName : PlaylistProperty()

    object Icon : PlaylistProperty()

    object MediaType : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = "media_type"
    }

    object Podcasts : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = "podcasts"
    }

    object ReleaseDate : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = "release_date"
    }

    data class Sort(val sortOrder: Playlist.SortOrder) : PlaylistProperty()

    object Starred : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = "starred"
    }
}
