package au.com.shiftyjelly.pocketcasts.repositories.podcast

sealed interface FilterUpdatedEvent {
    val groupValue: String
}

sealed class PlaylistProperty {

    object AutoDownload : PlaylistProperty()

    object AutoDownloadLimit : PlaylistProperty()

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

    object Sort : PlaylistProperty()

    object Starred : PlaylistProperty(), FilterUpdatedEvent {
        override val groupValue = "starred"
    }
}
