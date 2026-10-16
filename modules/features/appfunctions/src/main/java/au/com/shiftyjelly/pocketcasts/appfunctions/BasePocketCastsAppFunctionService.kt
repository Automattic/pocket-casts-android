package au.com.shiftyjelly.pocketcasts.appfunctions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadType
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayFromSearchResolver
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayFromSearchResult
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackEffectsUpdater
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.SleepTimer
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistDraft
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.ServiceManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "PocketCastsAppFunctionService",
    appFunctionXmlFileName = "pocket_casts_app_functions",
)
abstract class BasePocketCastsAppFunctionService : AppFunctionService() {
    @Inject internal lateinit var playbackManager: PlaybackManager

    @Inject internal lateinit var playFromSearchResolver: PlayFromSearchResolver

    @Inject internal lateinit var playbackEffectsUpdater: PlaybackEffectsUpdater

    @Inject internal lateinit var sleepTimer: SleepTimer

    @Inject internal lateinit var podcastManager: PodcastManager

    @Inject internal lateinit var episodeManager: EpisodeManager

    @Inject internal lateinit var playlistManager: PlaylistManager

    @Inject internal lateinit var downloadQueue: DownloadQueue

    @Inject internal lateinit var serviceManager: ServiceManager

    @Inject internal lateinit var settings: Settings

    /**
     * Search podcasts by name and return their IDs.
     * Required workflow: Call this first to obtain the "podcastUuid" needed by "subscribeToPodcast" and "findEpisodes".
     *
     * @param query Podcast name or keywords, for example "daily news". Must not be blank.
     * @param searchCatalog False (default) searches only podcasts the user follows. True searches the full Pocket
     * Casts catalog, which needs an internet connection; use it to find podcasts the user does not follow yet.
     * @return Up to 20 matching podcasts, best match first. Empty when nothing matches.
     * @throws AppFunctionAppUnknownException If the catalog search fails; suggest the user check their internet
     * connection.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun searchPodcasts(query: String, searchCatalog: Boolean = false): List<PodcastResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) throw AppFunctionInvalidArgumentException("query must not be blank, pass a podcast or episode name")
        val podcasts = if (searchCatalog) {
            serviceManager.searchForPodcasts(query.trim())
                .getOrElse { throw AppFunctionAppUnknownException("Podcast catalog search failed, the device may be offline") }
                .searchResults
        } else {
            podcastManager.findSubscribedFlow(query.trim()).first()
        }
        val subscribedUuids = podcastManager.findSubscribedUuids().toSet()
        podcasts.take(RESULT_LIMIT).map { podcast ->
            PodcastResult(
                uuid = podcast.uuid,
                title = podcast.title,
                author = podcast.author,
                isSubscribed = podcast.uuid in subscribedUuids,
            )
        }
    }

    /**
     * Find episodes in one of the user's lists, from a podcast, or by title, and return their IDs.
     * Required workflow: Call this first to obtain the "episodeUuid" needed by "playEpisode", "addToUpNext",
     * "downloadEpisode" and "starEpisode". Set at least one of "list", "podcastUuid" or "query"; they combine as
     * filters.
     *
     * @param list Optional list to read from. "IN_PROGRESS": episodes the user started but has not finished.
     * "NEW_RELEASES": recent episodes from followed podcasts. "STARRED": episodes the user starred. "DOWNLOADED":
     * episodes stored on the device for offline listening.
     * @param podcastUuid Optional podcast ID from "searchPodcasts". Limits results to that podcast, including
     * podcasts the user does not follow.
     * @param query Optional words matched against episode titles, case insensitive.
     * @return Up to 20 matching episodes, newest first. Empty when nothing matches.
     * @throws AppFunctionInvalidArgumentException If no filter is set; retry with at least one.
     * @throws AppFunctionElementNotFoundException If "podcastUuid" is unknown; call "searchPodcasts" for a valid ID.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun findEpisodes(
        @AppFunctionStringValueConstraint(enumValues = [LIST_IN_PROGRESS, LIST_NEW_RELEASES, LIST_STARRED, LIST_DOWNLOADED])
        list: String? = null,
        podcastUuid: String? = null,
        query: String? = null,
    ): List<EpisodeResult> = withContext(Dispatchers.IO) {
        val searchTerm = query?.trim()?.takeIf { it.isNotEmpty() }
        if (list == null && podcastUuid == null && searchTerm == null) {
            throw AppFunctionInvalidArgumentException("Set at least one of list, podcastUuid or query")
        }
        val episodes: List<PodcastEpisode> = when (list) {
            LIST_IN_PROGRESS -> smartEpisodes(SmartPlaylistDraft.InProgress.rules, searchTerm)

            LIST_NEW_RELEASES -> smartEpisodes(SmartPlaylistDraft.NewReleases.rules, searchTerm)

            LIST_STARRED -> episodeManager.findStarredEpisodes()

            LIST_DOWNLOADED -> episodeManager.findDownloadedEpisodesFlow().first()

            null -> if (podcastUuid != null) {
                episodeManager.findEpisodesByPodcastOrderedByPublishDate(findOrDownloadPodcast(podcastUuid))
            } else {
                smartEpisodes(SmartRules.Default, searchTerm)
            }

            else -> throw AppFunctionInvalidArgumentException("Unknown list: $list")
        }
        val matches = episodes
            .filter { podcastUuid == null || it.podcastUuid == podcastUuid }
            .filter { searchTerm == null || it.title.contains(searchTerm, ignoreCase = true) }
            .sortedByDescending(PodcastEpisode::publishedDate)
            .take(RESULT_LIMIT)
        matches.toEpisodeResults()
    }

    /**
     * Get what is loaded in the player, whether it is playing, the playback speed, and the Up Next queue.
     * Answers questions such as "what am I listening to?" or "what plays next?".
     *
     * @return The player state and up to 20 queued episodes in play order.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getUpNext(): UpNextResult = withContext(Dispatchers.IO) {
        currentUpNext()
    }

    /**
     * Play a podcast, episode or playlist by name in a single step, the same way as a voice assistant request.
     * A podcast match plays its latest unplayed episode. "up next" resumes the Up Next queue. Prefer "playEpisode"
     * when an episode ID is already known.
     *
     * @param query What to play: a podcast name such as "The Daily", an episode title, a playlist name, or "up next".
     * Only podcasts the user follows are matched by name.
     * @return What matched and the episode that started playing.
     * @throws AppFunctionElementNotFoundException If nothing matches or the podcast has no unplayed episodes; call
     * "searchPodcasts" with "searchCatalog" true to find the podcast, then "findEpisodes" and "playEpisode".
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun playPodcast(query: String): PlaySearchResult = withContext(Dispatchers.IO) {
        if (query.isBlank()) throw AppFunctionInvalidArgumentException("query must not be blank, pass a podcast or episode name")
        when (val result = playFromSearchResolver.playFromSearch(query, SOURCE)) {
            PlayFromSearchResult.UpNext -> PlaySearchResult(
                matchType = MATCH_UP_NEXT,
                episode = playbackManager.upNextQueue.currentEpisode?.toEpisodeResult(),
                playlistTitle = null,
            )

            is PlayFromSearchResult.Episode -> PlaySearchResult(
                matchType = MATCH_PODCAST_OR_EPISODE,
                episode = result.episode.toEpisodeResult(),
                playlistTitle = null,
            )

            is PlayFromSearchResult.Playlist -> PlaySearchResult(
                matchType = MATCH_PLAYLIST,
                episode = result.firstEpisode.toEpisodeResult(),
                playlistTitle = result.title,
            )

            is PlayFromSearchResult.PodcastWithNothingToPlay -> throw AppFunctionElementNotFoundException(
                "${result.podcast.title} has no unplayed episodes",
            )

            PlayFromSearchResult.NotFound -> throw AppFunctionElementNotFoundException("Nothing matched \"$query\", call searchPodcasts with searchCatalog true to look further")
        }
    }

    /**
     * Play an episode immediately, replacing what is playing.
     * Required workflow: Call "findEpisodes" first to obtain "episodeUuid".
     *
     * @param episodeUuid Episode ID from "findEpisodes" or "getUpNext".
     * @return The episode that started playing.
     * @throws AppFunctionElementNotFoundException If "episodeUuid" is unknown; call "findEpisodes" for a valid ID.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun playEpisode(episodeUuid: String): EpisodeResult = withContext(Dispatchers.IO) {
        val episode = findEpisode(episodeUuid)
        playbackManager.playNowSuspend(episode = episode, sourceView = SOURCE)
        episode.toEpisodeResult()
    }

    /**
     * Queue an episode in Up Next without interrupting what is playing.
     * Required workflow: Call "findEpisodes" first to obtain "episodeUuid".
     *
     * @param episodeUuid Episode ID from "findEpisodes".
     * @param position "TOP" plays the episode next, after the current one. "BOTTOM" or null (default) adds it to the
     * end of the queue.
     * @return The episode that was queued.
     * @throws AppFunctionElementNotFoundException If "episodeUuid" is unknown; call "findEpisodes" for a valid ID.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addToUpNext(
        episodeUuid: String,
        @AppFunctionStringValueConstraint(enumValues = [POSITION_TOP, POSITION_BOTTOM])
        position: String? = null,
    ): EpisodeResult = withContext(Dispatchers.IO) {
        val episode = findEpisode(episodeUuid)
        when (position) {
            POSITION_TOP -> playbackManager.playNext(episode = episode, source = SOURCE)
            POSITION_BOTTOM, null -> playbackManager.playLast(episode = episode, source = SOURCE)
            else -> throw AppFunctionInvalidArgumentException("Unknown position: $position")
        }
        episode.toEpisodeResult()
    }

    /**
     * Play, pause, skip within, or move past the episode loaded in the player.
     *
     * @param action "PLAY" resumes the loaded episode. "PAUSE" pauses it. "SKIP_FORWARD" and "SKIP_BACK" jump
     * within it. "NEXT_EPISODE" finishes with it and starts the next episode in Up Next.
     * @param seconds How far "SKIP_FORWARD" or "SKIP_BACK" jumps. 0 (default) uses the user's skip setting. Ignored
     * by other actions.
     * @return The player state right after the request. Changes can take a moment to show; call "getUpNext" to
     * check again.
     * @throws AppFunctionElementNotFoundException If nothing is loaded in the player, or "NEXT_EPISODE" is used with
     * an empty queue; use "playPodcast" or "playEpisode" to start something.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun controlPlayback(
        @AppFunctionStringValueConstraint(
            enumValues = [ACTION_PLAY, ACTION_PAUSE, ACTION_SKIP_FORWARD, ACTION_SKIP_BACK, ACTION_NEXT_EPISODE],
        )
        action: String,
        seconds: Int = 0,
    ): UpNextResult = withContext(Dispatchers.IO) {
        if (seconds < 0) throw AppFunctionInvalidArgumentException("seconds must not be negative")
        if (playbackManager.upNextQueue.currentEpisode == null) {
            throw AppFunctionElementNotFoundException("Nothing is loaded in the player, start something with playPodcast or playEpisode")
        }
        val jumpAmountSeconds = seconds.takeIf { it > 0 }
        when (action) {
            ACTION_PLAY -> playbackManager.playQueueSuspend(sourceView = SOURCE)

            ACTION_PAUSE -> playbackManager.pauseSuspend(sourceView = SOURCE)

            ACTION_SKIP_FORWARD -> playbackManager.skipForwardSuspend(SOURCE, jumpAmountSeconds)

            ACTION_SKIP_BACK -> playbackManager.skipBackwardSuspend(SOURCE, jumpAmountSeconds)

            ACTION_NEXT_EPISODE -> {
                if (playbackManager.upNextQueue.queueEpisodes.isEmpty()) {
                    throw AppFunctionElementNotFoundException("Up Next is empty, queue an episode with addToUpNext first")
                }
                playbackManager.playNextInQueue(SOURCE)
            }

            else -> throw AppFunctionInvalidArgumentException("Unknown action: $action")
        }
        currentUpNext()
    }

    /**
     * Set how fast the current podcast plays. The speed is saved for that podcast if it has its own effects,
     * otherwise for all podcasts.
     *
     * @param speed Speed multiplier from 0.5 to 5.0, for example 1.5 for one and a half times normal speed. Rounded
     * to one decimal place.
     * @return The speed that was applied.
     * @throws AppFunctionElementNotFoundException If no podcast episode is loaded; start one with "playPodcast" or
     * "playEpisode" first.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun setPlaybackSpeed(speed: Double): Double = withContext(Dispatchers.IO) {
        if (speed !in MIN_SPEED..MAX_SPEED) {
            throw AppFunctionInvalidArgumentException("speed must be between $MIN_SPEED and $MAX_SPEED")
        }
        val effects = playbackEffectsUpdater.update { playbackSpeed = speed.roundedSpeed() }
            ?: throw AppFunctionElementNotFoundException("No podcast episode is loaded, start one with playPodcast or playEpisode")
        effects.playbackSpeed
    }

    /**
     * Stop playback automatically after a number of minutes, or when the current episode ends. Replaces any
     * existing sleep timer. Set exactly one of "minutes" or "endOfEpisode".
     *
     * @param minutes Minutes until playback stops, from 1 to 1440. Leave 0 when "endOfEpisode" is true.
     * @param endOfEpisode True stops playback when the current episode ends.
     * @return The sleep timer state.
     * @throws AppFunctionInvalidArgumentException If both or neither of "minutes" and "endOfEpisode" are set.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun setSleepTimer(minutes: Int = 0, endOfEpisode: Boolean = false): SleepTimerResult = withContext(Dispatchers.IO) {
        if (endOfEpisode && minutes != 0) {
            throw AppFunctionInvalidArgumentException("Set either minutes or endOfEpisode, not both")
        }
        if (endOfEpisode) {
            settings.setlastSleepEndOfEpisodes(1)
            sleepTimer.cancelTimer()
            sleepTimer.updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterEpisodes = 1)
        } else {
            if (minutes !in 1..MAX_SLEEP_MINUTES) {
                throw AppFunctionInvalidArgumentException("minutes must be between 1 and $MAX_SLEEP_MINUTES")
            }
            sleepTimer.sleepAfter(minutes.minutes)
        }
        currentSleepTimer()
    }

    /**
     * Turn off the sleep timer so playback continues. Does nothing when no timer is set.
     *
     * @return The sleep timer state.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun cancelSleepTimer(): SleepTimerResult = withContext(Dispatchers.IO) {
        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = false)
        sleepTimer.cancelTimer()
        currentSleepTimer()
    }

    /**
     * Follow a podcast so its new episodes appear in the user's library and "NEW_RELEASES".
     * Required workflow: Call "searchPodcasts" with "searchCatalog" true first to obtain "podcastUuid".
     *
     * @param podcastUuid Podcast ID from "searchPodcasts".
     * @return The followed podcast.
     * @throws AppFunctionElementNotFoundException If the podcast cannot be found or loaded; check the ID with
     * "searchPodcasts", or suggest the user check their internet connection.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun subscribeToPodcast(podcastUuid: String): PodcastResult = withContext(Dispatchers.IO) {
        val podcast = try {
            podcastManager.subscribeToPodcastOrThrow(podcastUuid, sync = true)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            throw AppFunctionElementNotFoundException("Could not follow podcast $podcastUuid: ${exception.message}")
        }
        PodcastResult(uuid = podcast.uuid, title = podcast.title, author = podcast.author, isSubscribed = true)
    }

    /**
     * Download an episode in the background so it can play offline. Downloads even on mobile data.
     * Required workflow: Call "findEpisodes" first to obtain "episodeUuid".
     *
     * @param episodeUuid Episode ID from "findEpisodes".
     * @return The episode. "isDownloaded" stays false until the download finishes; check later with "findEpisodes"
     * and list "DOWNLOADED".
     * @throws AppFunctionElementNotFoundException If "episodeUuid" is unknown; call "findEpisodes" for a valid ID.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun downloadEpisode(episodeUuid: String): EpisodeResult = withContext(Dispatchers.IO) {
        val episode = findEpisode(episodeUuid)
        if (!episode.isDownloaded) {
            downloadQueue.enqueue(episode.uuid, DownloadType.UserTriggered(waitForWifi = false), SOURCE)
        }
        episode.toEpisodeResult()
    }

    /**
     * Star an episode to save it to the user's Starred list, or remove the star.
     * Required workflow: Call "findEpisodes" first to obtain "episodeUuid".
     *
     * @param episodeUuid Episode ID from "findEpisodes". Uploaded files cannot be starred.
     * @param starred True adds the star. False removes it.
     * @return The updated episode.
     * @throws AppFunctionElementNotFoundException If "episodeUuid" is unknown; call "findEpisodes" for a valid ID.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun starEpisode(episodeUuid: String, starred: Boolean): EpisodeResult = withContext(Dispatchers.IO) {
        val episode = findEpisode(episodeUuid) as? PodcastEpisode
            ?: throw AppFunctionInvalidArgumentException("Only podcast episodes can be starred")
        episodeManager.starEpisode(episode, starred, SOURCE)
        episode.isStarred = starred
        episode.toEpisodeResult()
    }

    private suspend fun smartEpisodes(rules: SmartRules, searchTerm: String?): List<PodcastEpisode> {
        return playlistManager.smartEpisodesFlow(rules = rules, searchTerm = searchTerm)
            .first()
            .map(PlaylistEpisode.Available::episode)
    }

    private suspend fun findOrDownloadPodcast(podcastUuid: String): Podcast {
        return try {
            podcastManager.findOrDownloadPodcast(podcastUuid)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            throw AppFunctionElementNotFoundException("No podcast with ID $podcastUuid, call searchPodcasts for a valid ID")
        }
    }

    private suspend fun findEpisode(episodeUuid: String): BaseEpisode {
        return episodeManager.findEpisodeByUuid(episodeUuid)
            ?: throw AppFunctionElementNotFoundException("No episode with ID $episodeUuid, call findEpisodes for a valid ID")
    }

    private suspend fun currentUpNext(): UpNextResult {
        val queue = playbackManager.upNextQueue
        return UpNextResult(
            currentEpisode = queue.currentEpisode?.toEpisodeResult(),
            isPlaying = playbackManager.isPlaying(),
            playbackSpeed = playbackManager.getPlaybackSpeed(),
            queue = queue.queueEpisodes.take(RESULT_LIMIT).toEpisodeResults(),
            queueSize = queue.queueEpisodes.size,
        )
    }

    private fun currentSleepTimer(): SleepTimerResult {
        val state = sleepTimer.state
        return SleepTimerResult(
            isRunning = state.isSleepTimerRunning,
            minutesLeft = state.timeLeft.inWholeMinutes.toInt(),
            stopsAtEndOfEpisode = state.isSleepEndOfEpisodeRunning,
        )
    }

    private suspend fun BaseEpisode.toEpisodeResult(): EpisodeResult = listOf(this).toEpisodeResults().single()

    private suspend fun List<BaseEpisode>.toEpisodeResults(): List<EpisodeResult> {
        val podcastTitles = filterIsInstance<PodcastEpisode>()
            .map(PodcastEpisode::podcastUuid)
            .distinct()
            .associateWith { uuid -> podcastManager.findPodcastByUuid(uuid)?.title }
        return map { episode ->
            val podcastUuid = (episode as? PodcastEpisode)?.podcastUuid
            EpisodeResult(
                uuid = episode.uuid,
                title = episode.title,
                podcastUuid = podcastUuid,
                podcastTitle = podcastUuid?.let(podcastTitles::get),
                publishedAt = episode.publishedDate.toInstant(),
                durationSeconds = episode.duration.toInt(),
                playedUpToSeconds = episode.playedUpTo.toInt(),
                isPlayed = episode.isFinished,
                isStarred = episode.isStarred,
                isDownloaded = episode.isDownloaded,
            )
        }
    }

    private companion object {
        // TODO: Replace with a dedicated AppFunction source once EventHorizon's SourceViewType has one
        val SOURCE = SourceView.UNKNOWN

        const val RESULT_LIMIT = 20
        const val MIN_SPEED = 0.5
        const val MAX_SPEED = 5.0
        const val MAX_SLEEP_MINUTES = 1440

        const val LIST_IN_PROGRESS = "IN_PROGRESS"
        const val LIST_NEW_RELEASES = "NEW_RELEASES"
        const val LIST_STARRED = "STARRED"
        const val LIST_DOWNLOADED = "DOWNLOADED"

        const val POSITION_TOP = "TOP"
        const val POSITION_BOTTOM = "BOTTOM"

        const val ACTION_PLAY = "PLAY"
        const val ACTION_PAUSE = "PAUSE"
        const val ACTION_SKIP_FORWARD = "SKIP_FORWARD"
        const val ACTION_SKIP_BACK = "SKIP_BACK"
        const val ACTION_NEXT_EPISODE = "NEXT_EPISODE"

        const val MATCH_UP_NEXT = "UP_NEXT"
        const val MATCH_PODCAST_OR_EPISODE = "PODCAST_OR_EPISODE"
        const val MATCH_PLAYLIST = "PLAYLIST"
    }
}
