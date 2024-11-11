package au.com.shiftyjelly.pocketcasts.repositories.sync

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager

interface PodcastRefresher {
    suspend fun refreshPodcast(existingPodcast: Podcast, playbackManager: PlaybackManager)
}
