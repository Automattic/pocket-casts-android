package au.com.shiftyjelly.pocketcasts.sharing

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.clip.Clip
import au.com.shiftyjelly.pocketcasts.sharing.ui.VisualCardType
import java.io.File

internal class FFmpegMediaService(
    private val context: Context,
) : MediaService {
    override suspend fun clipAudio(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range): Result<File> {
        throw AssertionError("Not supported in release")
    }

    override suspend fun clipVideo(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range, cardType: VisualCardType, backgroundFile: File): Result<File> {
        throw AssertionError("Not supported in release")
    }
}
