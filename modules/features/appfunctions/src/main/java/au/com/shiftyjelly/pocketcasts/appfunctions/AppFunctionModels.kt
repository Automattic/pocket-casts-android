package au.com.shiftyjelly.pocketcasts.appfunctions

import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionStringValueConstraint
import java.time.Instant

/** A podcast show. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PodcastResult(
    /** Unique podcast ID. Pass to "subscribeToPodcast" or as "podcastUuid" to "findEpisodes". */
    val uuid: String,
    /** Podcast name. */
    val title: String,
    /** Podcast author or publisher. */
    val author: String,
    /** True when the user follows this podcast in Pocket Casts. */
    val isSubscribed: Boolean,
)

/** A podcast episode, or an audio file the user uploaded to Pocket Casts. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class EpisodeResult(
    /** Unique episode ID. Pass to "playEpisode", "addToUpNext", "downloadEpisode" or "starEpisode". */
    val uuid: String,
    /** Episode title. */
    val title: String,
    /** ID of the podcast the episode belongs to. Null for files the user uploaded. */
    val podcastUuid: String?,
    /** Name of the podcast the episode belongs to. Null for files the user uploaded. */
    val podcastTitle: String?,
    /** When the episode was published. */
    val publishedAt: Instant,
    /** Episode length in seconds. 0 when unknown. */
    val durationSeconds: Int,
    /** How far the user has listened, in seconds. */
    val playedUpToSeconds: Int,
    /** True when the user has finished listening to the episode. */
    val isPlayed: Boolean,
    /** True when the user has starred the episode. */
    val isStarred: Boolean,
    /** True when the episode is downloaded and can play offline. */
    val isDownloaded: Boolean,
)

/** The result of playing something found by a free-text search. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PlaySearchResult(
    /** What was matched: "UP_NEXT", "PODCAST_OR_EPISODE" or "PLAYLIST". */
    @property:AppFunctionStringValueConstraint(enumValues = ["UP_NEXT", "PODCAST_OR_EPISODE", "PLAYLIST"])
    val matchType: String,
    /** The episode that started playing. Null when the Up Next queue was resumed. */
    val episode: EpisodeResult?,
    /** Name of the playlist that started playing. Null unless "matchType" is "PLAYLIST". */
    val playlistTitle: String?,
)

/** The playing episode and the Up Next queue. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class UpNextResult(
    /** The episode loaded in the player, whether playing or paused. Null when nothing is loaded. */
    val currentEpisode: EpisodeResult?,
    /** True when audio is playing right now. */
    val isPlaying: Boolean,
    /** Playback speed multiplier, for example 1.5. */
    val playbackSpeed: Double,
    /** Episodes queued to play after the current one, in play order. */
    val queue: List<EpisodeResult>,
    /** Total number of queued episodes, which can exceed the size of "queue". */
    val queueSize: Int,
)

/** The state of the sleep timer. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SleepTimerResult(
    /** True when a sleep timer is set. */
    val isRunning: Boolean,
    /** Minutes until playback stops. 0 when the timer stops at the end of an episode or is off. */
    val minutesLeft: Int,
    /** True when playback stops at the end of the current episode. */
    val stopsAtEndOfEpisode: Boolean,
)
