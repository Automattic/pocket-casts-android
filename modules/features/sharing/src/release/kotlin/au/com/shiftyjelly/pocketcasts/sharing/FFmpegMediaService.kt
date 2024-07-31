package au.com.shiftyjelly.pocketcasts.sharing

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.clip.Clip

internal class FFmpegMediaService(
    private val context: Context,
) : MediaExtractor {
    override suspend fun clipAudio(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range): Result<File> {
        throw AssertionError("Not supported in release")
    }
}
