package au.com.shiftyjelly.pocketcasts.media

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.Clip
import au.com.shiftyjelly.pocketcasts.sharing.MediaService
import au.com.shiftyjelly.pocketcasts.sharing.VisualCardType
import java.io.File

internal class NoOpMediaService(
    private val context: Context,
) : MediaService {
    override suspend fun clipAudio(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) = runCatching {
        error("Operation not supported")
    }

    override suspend fun clipVideo(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range, cardType: VisualCardType, backgroundFile: File) = runCatching {
        error("Operation not supported")
    }
}
