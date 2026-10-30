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

/** A playlist of episodes the user created. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PlaylistResult(
    /** Unique playlist ID. Pass as "playlistUuid" to "getPlaylistEpisodes". */
    val uuid: String,
    /** Playlist name. */
    val title: String,
)

/** A podcast episode, or an audio/video file the user uploaded to Pocket Casts. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class EpisodeResult(
    /** Unique episode ID. Pass to any function that takes an "episodeUuid", such as "playEpisode" or "getTranscript". */
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
    /** True when the user archived the episode, hiding it from their episode lists. */
    val isArchived: Boolean,
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
    /**
     * The episode loaded in the player, whether playing or paused. Its "playedUpToSeconds" is the live playback
     * position. Null when nothing is loaded.
     */
    val currentEpisode: EpisodeResult?,
    /** True when audio is playing right now. */
    val isPlaying: Boolean,
    /** Playback speed multiplier, for example 1.5. */
    val playbackSpeed: Double,
    /** Episodes queued to play after the current one, in play order. */
    val queue: List<EpisodeResult>,
    /** Total number of queued episodes, which can exceed the size of "queue". */
    val queueSize: Int,
    /** Title of the chapter playing now. Null when the episode has no chapters or nothing is loaded. */
    val currentChapterTitle: String?,
    /**
     * Listening time left, in seconds at normal speed, across the rest of the current episode and every queued
     * episode. Episodes with an unknown length count as 0.
     */
    val upNextRemainingSeconds: Int,
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

/** A chapter of an episode. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ChapterResult(
    /** Chapter title. */
    val title: String,
    /** Where the chapter starts, in seconds. Pass to "seekTo" to jump to it. */
    val startSeconds: Int,
    /** Where the chapter ends, in seconds. */
    val endSeconds: Int,
)

/** An episode with its show notes and chapters. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class EpisodeDetailsResult(
    /** The episode. */
    val episode: EpisodeResult,
    /** The episode description and show notes as plain text, including any links. Null when there are none. */
    val showNotes: String?,
    /** The episode's chapters in order. Empty when it has none or they have not loaded yet. */
    val chapters: List<ChapterResult>,
)

/** An episode transcript, or part of one. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class TranscriptResult(
    /**
     * The transcript, one line per passage. Each line starts with its time as "[h:mm:ss]" when known, and with the
     * speaker's name when the speaker changes.
     */
    val text: String,
    /** True when Pocket Casts generated the transcript from the audio, so names and words may be inaccurate. */
    val isGenerated: Boolean,
    /** True when the text was cut short. Call again with "fromSeconds" set to the last time shown to read on. */
    val isTruncated: Boolean,
)

/** A saved moment in an episode. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class BookmarkResult(
    /** Unique bookmark ID. */
    val uuid: String,
    /** Bookmark title. */
    val title: String,
    /** ID of the bookmarked episode. */
    val episodeUuid: String,
    /** Position of the bookmark in the episode, in seconds. */
    val timeSeconds: Int,
)
