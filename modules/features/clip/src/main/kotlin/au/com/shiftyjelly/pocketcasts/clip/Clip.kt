package au.com.shiftyjelly.pocketcasts.clip

import android.os.Parcelable
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.utils.parceler.DurationParceler
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

data class Clip(
    val episode: PodcastEpisode,
    val range: Range,
) {
    @Parcelize
    data class Range(
        @TypeParceler<Duration, DurationParceler>() val start: Duration,
        @TypeParceler<Duration, DurationParceler>() val end: Duration,
    ) : Parcelable {
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
