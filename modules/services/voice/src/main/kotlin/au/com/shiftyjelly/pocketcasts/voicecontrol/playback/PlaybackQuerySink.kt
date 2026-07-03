package au.com.shiftyjelly.pocketcasts.voicecontrol.playback

import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceResponse
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackQuerySink @Inject constructor(
    private val playbackManager: PlaybackManager,
) : VoicePlaybackQuerySink {
    override fun whatsPlaying(): VoiceResponse.Spoken {
        val episode = playbackManager.getCurrentEpisode()
        val podcast = playbackManager.getCurrentPodcast()
        return when {
            episode != null && podcast != null ->
                VoiceResponse.Spoken("${episode.title} from ${podcast.title}")

            episode != null ->
                VoiceResponse.Spoken(episode.title)

            else ->
                VoiceResponse.Spoken("Nothing is playing")
        }
    }

    override fun position(): VoiceResponse.Spoken {
        val episode = playbackManager.getCurrentEpisode() ?: return VoiceResponse.Spoken("No episode")
        val playedUpTo = episode.playedUpTo.toLong()
        return VoiceResponse.Spoken("${formatSec(playedUpTo)} of ${formatSec(episode.duration.toLong())}")
    }

    override fun timeRemaining(): VoiceResponse.Spoken {
        val episode = playbackManager.getCurrentEpisode() ?: return VoiceResponse.Spoken("No episode")
        val remaining = (episode.duration - episode.playedUpTo).toLong().coerceAtLeast(0)
        return VoiceResponse.Spoken("${formatSec(remaining)} remaining")
    }

    override fun currentPodcast(): VoiceResponse.Spoken {
        val podcast = playbackManager.getCurrentPodcast()
        return if (podcast != null) {
            VoiceResponse.Spoken(podcast.title)
        } else {
            VoiceResponse.Spoken("No podcast playing")
        }
    }

    override fun episodeDuration(): VoiceResponse.Spoken {
        val episode = playbackManager.getCurrentEpisode()
        return if (episode != null) {
            VoiceResponse.Spoken(formatSec(episode.duration.toLong()))
        } else {
            VoiceResponse.Spoken("No episode")
        }
    }

    override fun publishDate(): VoiceResponse.Spoken {
        val episode = playbackManager.getCurrentEpisode()
        return if (episode != null) {
            val df = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            VoiceResponse.Spoken(df.format(episode.publishedDate))
        } else {
            VoiceResponse.Spoken("No episode")
        }
    }

    override fun episodeDescription(): VoiceResponse.Spoken {
        val episode = playbackManager.getCurrentEpisode()
        return if (episode != null) {
            VoiceResponse.Spoken(episode.episodeDescription)
        } else {
            VoiceResponse.Spoken("No episode")
        }
    }

    override fun downloadStatus(): VoiceResponse.Spoken {
        val episode = playbackManager.getCurrentEpisode()
        return if (episode != null) {
            val status = if (episode.isDownloaded) "downloaded" else "streaming"
            VoiceResponse.Spoken("Episode is $status")
        } else {
            VoiceResponse.Spoken("No episode")
        }
    }

    override fun episodeTitle(): VoiceResponse.Spoken {
        val episode = playbackManager.getCurrentEpisode()
        return if (episode != null) {
            VoiceResponse.Spoken(episode.title)
        } else {
            VoiceResponse.Spoken("No episode")
        }
    }

    private fun formatSec(totalSeconds: Long): String {
        val seconds = totalSeconds.coerceAtLeast(0)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val sec = seconds % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m ${sec}s"
        } else {
            "${minutes}m ${sec}s"
        }
    }
}
