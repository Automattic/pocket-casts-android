package au.com.shiftyjelly.pocketcasts.sharing

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import java.io.File

interface MediaService {
    suspend fun clipAudio(
        podcast: Podcast,
        episode: PodcastEpisode,
        clipRange: Clip.Range,
    ): Result<File>

    suspend fun clipVideo(
        podcast: Podcast,
        episode: PodcastEpisode,
        clipRange: Clip.Range,
        cardType: VisualCardType,
        backgroundFile: File,
    ): Result<File>
}
