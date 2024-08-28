package au.com.shiftyjelly.pocketcasts.sharing

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class Clip(
    val sourceUri: String,
    val range: Range,
) {
    companion object {
        fun fromEpisode(
            episode: PodcastEpisode,
            range: Range = Range.fromPosition(episode.playedUpTo.seconds, episode.duration.seconds),
        ) = Clip(
            sourceUri = episode.let { if (it.isDownloaded) it.downloadedFilePath else it.downloadUrl }.orEmpty(),
            range = range,
        )
    }

    data class Range(
        val start: Duration,
        val end: Duration,
    ) {
        val startInSeconds = start.inWholeSeconds.toInt()
        val endInSeconds = end.inWholeSeconds.toInt()
        val duration = end - start
        val durationInSeconds = duration.inWholeSeconds.toInt()

        operator fun contains(duration: Duration) = duration in start..end

        companion object {
            fun fromPosition(
                playbackPosition: Duration,
                episodeDuration: Duration,
                clipDuration: Duration = 15.seconds,
            ): Clip.Range {
                val initialClipStart = (playbackPosition - clipDuration / 2).coerceAtLeast(0.seconds)
                val clipEnd = (initialClipStart + clipDuration).coerceAtMost(episodeDuration)
                val clipStart = (clipEnd - clipDuration).coerceAtLeast(0.seconds)
                return Clip.Range(clipStart, clipEnd)
            }
        }
    }
}
