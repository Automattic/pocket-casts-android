package au.com.shiftyjelly.pocketcasts.appfunctions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionNotSupportedException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import androidx.core.text.parseAsHtml
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.toPodcastEpisodes
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadType
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayFromSearchResolver
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayFromSearchResult
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackEffectsUpdater
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.SleepTimer
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistDraft
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.servers.ServiceManager
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import com.automattic.eventhorizon.BookmarkSourceType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

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

    @Inject internal lateinit var bookmarkManager: BookmarkManager

    @Inject internal lateinit var chapterManager: ChapterManager

    @Inject internal lateinit var showNotesManager: ShowNotesManager

    @Inject internal lateinit var transcriptManager: TranscriptManager

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
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun searchPodcasts(query: String, searchCatalog: Boolean = false): List<PodcastResult> = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("searchPodcasts(query=$query, searchCatalog=$searchCatalog)")
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
     * "downloadEpisode" and "starEpisode". Set at least one of "list", "podcastUuid", "query" or
     * "maxDurationMinutes"; they combine as filters.
     *
     * @param list Optional list to read from. "IN_PROGRESS": episodes the user started but has not finished.
     * "NEW_RELEASES": recent episodes from followed podcasts. "STARRED": episodes the user starred. "DOWNLOADED":
     * episodes stored on the device for offline listening.
     * @param podcastUuid Optional podcast ID from "searchPodcasts". Limits results to that podcast, including
     * podcasts the user does not follow.
     * @param query Optional words matched against episode titles, case insensitive.
     * @param maxDurationMinutes Optional longest episode length in minutes, for requests such as "something under 20
     * minutes". 0 (default) allows any length. Episodes with an unknown length are left out when set.
     * @return Up to 20 matching episodes, newest first. Empty when nothing matches.
     * @throws AppFunctionInvalidArgumentException If no filter is set; retry with at least one.
     * @throws AppFunctionElementNotFoundException If "podcastUuid" is unknown; call "searchPodcasts" for a valid ID.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun findEpisodes(
        @AppFunctionStringValueConstraint(enumValues = [LIST_IN_PROGRESS, LIST_NEW_RELEASES, LIST_STARRED, LIST_DOWNLOADED])
        list: String? = null,
        podcastUuid: String? = null,
        query: String? = null,
        maxDurationMinutes: Int = 0,
    ): List<EpisodeResult> = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("findEpisodes(list=$list, podcastUuid=$podcastUuid, query=$query, maxDurationMinutes=$maxDurationMinutes)")
        val searchTerm = query?.trim()?.takeIf { it.isNotEmpty() }
        if (maxDurationMinutes < 0) throw AppFunctionInvalidArgumentException("maxDurationMinutes must not be negative")
        if (list == null && podcastUuid == null && searchTerm == null && maxDurationMinutes == 0) {
            throw AppFunctionInvalidArgumentException("Set at least one of list, podcastUuid, query or maxDurationMinutes")
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
            .filter { maxDurationMinutes == 0 || it.duration in 1.0..maxDurationMinutes * 60.0 }
            .sortedByDescending(PodcastEpisode::publishedDate)
            .take(RESULT_LIMIT)
        matches.toEpisodeResults()
    }

    /**
     * Get what is loaded in the player, the playback position and chapter, whether it is playing, the playback
     * speed, and the Up Next queue. Answers questions such as "what am I listening to?", "how long is left?" or
     * "what plays next?". Call this to find the "episodeUuid" when the user says "this episode".
     *
     * @return The player state and up to 20 queued episodes in play order.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getUpNext(): UpNextResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("getUpNext()")
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
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun playPodcast(query: String): PlaySearchResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("playPodcast(query=$query)")
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
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun playEpisode(episodeUuid: String): EpisodeResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("playEpisode(episodeUuid=$episodeUuid)")
        val episode = findEpisode(episodeUuid)
        playbackManager.playNowSuspend(episode = episode, sourceView = SOURCE)
        episode.toEpisodeResult()
    }

    /**
     * Queue an episode in Up Next without interrupting what is playing. An episode already in Up Next moves to the
     * requested position, so this also reorders the queue.
     * Required workflow: Call "findEpisodes" first to obtain "episodeUuid".
     *
     * @param episodeUuid Episode ID from "findEpisodes".
     * @param position "TOP" plays the episode next, after the current one. "BOTTOM" or null (default) adds it to the
     * end of the queue.
     * @return The episode that was queued.
     * @throws AppFunctionElementNotFoundException If "episodeUuid" is unknown; call "findEpisodes" for a valid ID.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun addToUpNext(
        episodeUuid: String,
        @AppFunctionStringValueConstraint(enumValues = [POSITION_TOP, POSITION_BOTTOM])
        position: String? = null,
    ): EpisodeResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("addToUpNext(episodeUuid=$episodeUuid, position=$position)")
        val episode = findEpisode(episodeUuid)
        when (position) {
            POSITION_TOP -> playbackManager.playNext(episode = episode, source = SOURCE)
            POSITION_BOTTOM, null -> playbackManager.playLast(episode = episode, source = SOURCE)
            else -> throw AppFunctionInvalidArgumentException("Unknown position: $position")
        }
        episode.toEpisodeResult()
    }

    /**
     * Remove an episode from Up Next. Removing the episode loaded in the player stops it and loads the next one.
     * Required workflow: Call "getUpNext" first to obtain "episodeUuid".
     *
     * @param episodeUuid Episode ID from "getUpNext".
     * @return The removed episode. The change can take a moment to show in "getUpNext".
     * @throws AppFunctionElementNotFoundException If the episode is unknown or not in Up Next; call "getUpNext" for a
     * valid ID.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun removeFromUpNext(episodeUuid: String): EpisodeResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("removeFromUpNext(episodeUuid=$episodeUuid)")
        val episode = findEpisode(episodeUuid)
        if (!playbackManager.upNextQueue.contains(episode.uuid)) {
            throw AppFunctionElementNotFoundException("Episode $episodeUuid is not in Up Next, call getUpNext for a valid ID")
        }
        playbackManager.removeEpisode(episode, SOURCE)
        episode.toEpisodeResult()
    }

    /**
     * Play, pause, skip within, or move past the episode loaded in the player.
     *
     * @param action "PLAY" resumes the loaded episode. "PAUSE" pauses it. "SKIP_FORWARD" and "SKIP_BACK" jump
     * within it. "NEXT_CHAPTER" and "PREVIOUS_CHAPTER" jump between its chapters, for requests such as "skip this
     * chapter" or "skip the ad". "NEXT_EPISODE" finishes with it and starts the next episode in Up Next.
     * @param seconds How far "SKIP_FORWARD" or "SKIP_BACK" jumps, for example 120 for "go back 2 minutes". 0
     * (default) uses the user's skip setting. Ignored by other actions.
     * @return The player state right after the request. Changes can take a moment to show; call "getUpNext" to
     * check again.
     * @throws AppFunctionElementNotFoundException If nothing is loaded in the player, a chapter action is used on an
     * episode without chapters, or "NEXT_EPISODE" is used with an empty queue; use "playPodcast" or "playEpisode" to
     * start something.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun controlPlayback(
        @AppFunctionStringValueConstraint(
            enumValues = [
                ACTION_PLAY,
                ACTION_PAUSE,
                ACTION_SKIP_FORWARD,
                ACTION_SKIP_BACK,
                ACTION_NEXT_CHAPTER,
                ACTION_PREVIOUS_CHAPTER,
                ACTION_NEXT_EPISODE,
            ],
        )
        action: String,
        seconds: Int = 0,
    ): UpNextResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("controlPlayback(action=$action, seconds=$seconds)")
        if (seconds < 0) throw AppFunctionInvalidArgumentException("seconds must not be negative")
        requireCurrentEpisode()
        val jumpAmountSeconds = seconds.takeIf { it > 0 }
        when (action) {
            ACTION_PLAY -> playbackManager.playQueueSuspend(sourceView = SOURCE)

            ACTION_PAUSE -> playbackManager.pauseSuspend(sourceView = SOURCE)

            ACTION_SKIP_FORWARD -> playbackManager.skipForwardSuspend(SOURCE, jumpAmountSeconds)

            ACTION_SKIP_BACK -> playbackManager.skipBackwardSuspend(SOURCE, jumpAmountSeconds)

            ACTION_NEXT_CHAPTER -> {
                requireChapters()
                playbackManager.skipToNextSelectedOrLastChapter()
            }

            ACTION_PREVIOUS_CHAPTER -> {
                requireChapters()
                playbackManager.skipToPreviousSelectedOrLastChapter()
            }

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
     * Jump to a position in the episode loaded in the player, for requests such as "go to 42 minutes in", "start
     * from the beginning", or jumping to a chapter or transcript line by its time.
     *
     * @param seconds Position from the start of the episode, in seconds. 0 restarts it. Get chapter times from
     * "getEpisodeDetails" and line times from "getTranscript".
     * @return The player state right after the jump.
     * @throws AppFunctionElementNotFoundException If nothing is loaded in the player; use "playPodcast" or
     * "playEpisode" to start something.
     * @throws AppFunctionInvalidArgumentException If "seconds" is negative or past the end of the episode.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun seekTo(seconds: Int): UpNextResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("seekTo(seconds=$seconds)")
        val episode = requireCurrentEpisode()
        if (seconds < 0 || (episode.duration > 0 && seconds > episode.duration)) {
            throw AppFunctionInvalidArgumentException("seconds must be between 0 and the episode length of ${episode.duration.toInt()}")
        }
        playbackManager.seekToTimeMsSuspend(seconds * 1000)
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
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun setPlaybackSpeed(speed: Double): Double = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("setPlaybackSpeed(speed=$speed)")
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
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun setSleepTimer(minutes: Int = 0, endOfEpisode: Boolean = false): SleepTimerResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("setSleepTimer(minutes=$minutes, endOfEpisode=$endOfEpisode)")
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
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun cancelSleepTimer(): SleepTimerResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("cancelSleepTimer()")
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
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun subscribeToPodcast(podcastUuid: String): PodcastResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("subscribeToPodcast(podcastUuid=$podcastUuid)")
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
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun downloadEpisode(episodeUuid: String): EpisodeResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("downloadEpisode(episodeUuid=$episodeUuid)")
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
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun starEpisode(episodeUuid: String, starred: Boolean): EpisodeResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("starEpisode(episodeUuid=$episodeUuid, starred=$starred)")
        val episode = findEpisode(episodeUuid) as? PodcastEpisode
            ?: throw AppFunctionInvalidArgumentException("Only podcast episodes can be starred")
        episodeManager.starEpisode(episode, starred, SOURCE)
        episode.isStarred = starred
        episode.toEpisodeResult()
    }

    /**
     * Mark an episode as played or unplayed. Marking it played also removes it from Up Next, and may archive it
     * depending on the user's auto archive settings.
     * Required workflow: Call "findEpisodes" or "getUpNext" first to obtain "episodeUuid".
     *
     * @param episodeUuid Episode ID from "findEpisodes" or "getUpNext".
     * @param played True marks the episode as finished. False marks it as not played.
     * @return The updated episode.
     * @throws AppFunctionElementNotFoundException If "episodeUuid" is unknown; call "findEpisodes" for a valid ID.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun markEpisodePlayed(episodeUuid: String, played: Boolean): EpisodeResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("markEpisodePlayed(episodeUuid=$episodeUuid, played=$played)")
        val episode = findEpisode(episodeUuid)
        if (played) {
            episodeManager.markAsPlayedBlocking(episode, playbackManager, podcastManager)
        } else {
            episodeManager.markAsNotPlayedBlocking(episode)
        }
        findEpisode(episodeUuid).toEpisodeResult()
    }

    /**
     * Archive an episode to hide it from the user's episode lists, or restore an archived episode. Archiving also
     * removes it from Up Next and deletes its download.
     * Required workflow: Call "findEpisodes" or "getUpNext" first to obtain "episodeUuid".
     *
     * @param episodeUuid Episode ID from "findEpisodes" or "getUpNext". Uploaded files cannot be archived.
     * @param archived True archives the episode. False restores it.
     * @return The updated episode.
     * @throws AppFunctionElementNotFoundException If "episodeUuid" is unknown; call "findEpisodes" for a valid ID.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun archiveEpisode(episodeUuid: String, archived: Boolean): EpisodeResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("archiveEpisode(episodeUuid=$episodeUuid, archived=$archived)")
        val episode = findEpisode(episodeUuid) as? PodcastEpisode
            ?: throw AppFunctionInvalidArgumentException("Only podcast episodes can be archived")
        if (archived) {
            episodeManager.archiveBlocking(episode, playbackManager)
        } else {
            episodeManager.unarchiveBlocking(episode)
        }
        findEpisode(episodeUuid).toEpisodeResult()
    }

    /**
     * List the user's playlists, including smart playlists built from rules and manual playlists the user curates.
     * Required workflow: Call this first to obtain the "playlistUuid" needed by "getPlaylistEpisodes".
     *
     * @return All playlists in the order the user arranged them. Empty when the user has none.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getPlaylists(): List<PlaylistResult> = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("getPlaylists()")
        playlistManager.playlistPreviewsFlow().first().map { playlist ->
            PlaylistResult(uuid = playlist.uuid, title = playlist.title)
        }
    }

    /**
     * Get the episodes in one of the user's playlists. Use "publishedAt" to pick an episode such as the most recent,
     * then pass its ID to "playEpisode" or "addToUpNext".
     * Required workflow: Call "getPlaylists" first to obtain "playlistUuid".
     *
     * @param playlistUuid Playlist ID from "getPlaylists".
     * @return Up to 50 episodes in the playlist's own order, which the user chooses and may not be newest first.
     * Archived and unavailable episodes are left out.
     * @throws AppFunctionElementNotFoundException If "playlistUuid" is unknown; call "getPlaylists" for a valid ID.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getPlaylistEpisodes(playlistUuid: String): List<EpisodeResult> = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("getPlaylistEpisodes(playlistUuid=$playlistUuid)")
        val playlist = playlistManager.smartPlaylistFlow(playlistUuid).first()
            ?: playlistManager.manualPlaylistFlow(playlistUuid).first()
            ?: throw AppFunctionElementNotFoundException("No playlist with ID $playlistUuid, call getPlaylists for a valid ID")
        playlist.episodes.toPodcastEpisodes().take(PLAYLIST_EPISODE_LIMIT).toEpisodeResults()
    }

    /**
     * Get an episode's show notes and chapters. Use the show notes to answer questions such as "what links did they
     * mention?", and chapter times with "seekTo" for requests such as "jump to the part about pricing".
     * Required workflow: Call "findEpisodes" or "getUpNext" first to obtain "episodeUuid".
     *
     * @param episodeUuid Episode ID from "findEpisodes" or "getUpNext".
     * @return The episode, its show notes and its chapters.
     * @throws AppFunctionElementNotFoundException If "episodeUuid" is unknown; call "findEpisodes" for a valid ID.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getEpisodeDetails(episodeUuid: String): EpisodeDetailsResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("getEpisodeDetails(episodeUuid=$episodeUuid)")
        val episode = findEpisode(episodeUuid)
        val showNotesHtml = loadShowNotes(episode) ?: episode.episodeDescription
        EpisodeDetailsResult(
            episode = episode.toEpisodeResult(),
            showNotes = showNotesHtml.parseAsHtml().toString().trim().takeIf { it.isNotEmpty() },
            chapters = chapterManager.observerChaptersForEpisode(episode.uuid).first().map { chapter ->
                ChapterResult(
                    title = chapter.title,
                    startSeconds = chapter.startTime.inWholeSeconds.toInt(),
                    endSeconds = chapter.endTime.inWholeSeconds.toInt(),
                )
            },
        )
    }

    /**
     * Get the transcript of an episode, or the part between two times. Use it to summarise an episode, answer
     * questions about what was said, recap the last few minutes, or find when something was mentioned and pass that
     * time to "seekTo". Read long episodes in parts by setting "fromSeconds".
     * Required workflow: Call "findEpisodes" or "getUpNext" first to obtain "episodeUuid".
     *
     * @param episodeUuid Episode ID from "findEpisodes" or "getUpNext".
     * @param fromSeconds Start of the part to read, in seconds. 0 (default) starts at the beginning.
     * @param toSeconds End of the part to read, in seconds. 0 (default) reads to the end.
     * @return The transcript text, cut short after about 50,000 characters.
     * @throws AppFunctionElementNotFoundException If "episodeUuid" is unknown or the episode has no transcript; the
     * podcast may not publish one.
     * @throws AppFunctionInvalidArgumentException If a time is negative, or "toSeconds" is before "fromSeconds".
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getTranscript(episodeUuid: String, fromSeconds: Int = 0, toSeconds: Int = 0): TranscriptResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("getTranscript(episodeUuid=$episodeUuid, fromSeconds=$fromSeconds, toSeconds=$toSeconds)")
        if (fromSeconds < 0 || toSeconds < 0) throw AppFunctionInvalidArgumentException("Times must not be negative")
        if (toSeconds in 1 until fromSeconds) throw AppFunctionInvalidArgumentException("toSeconds must not be before fromSeconds")
        val episode = findEpisode(episodeUuid)
        // Transcript links are saved while the show notes are processed
        loadShowNotes(episode)
        val transcript = transcriptManager.loadTranscript(episode.uuid) as? Transcript.Text
            ?: transcriptManager.loadGeneratedTranscript(episode.uuid)
            ?: throw AppFunctionElementNotFoundException("No transcript is available for episode $episodeUuid")
        transcript.toTranscriptResult(fromSeconds, toSeconds, MAX_TRANSCRIPT_CHARS)
    }

    /**
     * Bookmark the current moment in the episode loaded in the player, for requests such as "bookmark this".
     * Requires a Pocket Casts Plus or Patron subscription.
     *
     * @param title Optional bookmark name, for example "pasta recipe". Null or blank (default) uses "Bookmark".
     * @return The bookmark. An existing bookmark at the same second is reused and renamed when "title" is set.
     * @throws AppFunctionNotSupportedException If the user does not have a subscription; tell them bookmarks need
     * Pocket Casts Plus.
     * @throws AppFunctionElementNotFoundException If nothing is loaded in the player; use "playPodcast" or
     * "playEpisode" to start something.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun addBookmark(title: String? = null): BookmarkResult = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("addBookmark(title=$title)")
        if (settings.cachedSubscription.value == null) {
            throw AppFunctionNotSupportedException("Bookmarks need a Pocket Casts Plus subscription")
        }
        val episode = requireCurrentEpisode()
        val timeSecs = playbackManager.getCurrentTimeMs(episode) / 1000
        val customTitle = title?.trim()?.takeIf { it.isNotEmpty() }
        val existing = bookmarkManager.findByEpisodeTime(episode = episode, timeSecs = timeSecs)
        val bookmark = when {
            existing == null -> bookmarkManager.add(
                episode = episode,
                timeSecs = timeSecs,
                title = customTitle ?: getString(LR.string.bookmark),
                creationSource = BOOKMARK_SOURCE,
            )

            customTitle != null -> existing.also {
                bookmarkManager.updateTitle(it.uuid, customTitle)
                it.title = customTitle
            }

            else -> existing
        }
        BookmarkResult(uuid = bookmark.uuid, title = bookmark.title, episodeUuid = episode.uuid, timeSeconds = bookmark.timeSecs)
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

    private fun requireCurrentEpisode(): BaseEpisode {
        return playbackManager.upNextQueue.currentEpisode
            ?: throw AppFunctionElementNotFoundException("Nothing is loaded in the player, start something with playPodcast or playEpisode")
    }

    private suspend fun requireChapters() {
        if (playbackManager.playbackStateFlow.first().chapters.isEmpty()) {
            throw AppFunctionElementNotFoundException("The episode in the player has no chapters, use SKIP_FORWARD instead")
        }
    }

    private suspend fun loadShowNotes(episode: BaseEpisode): String? {
        if (episode !is PodcastEpisode) return null
        val state = showNotesManager.loadShowNotes(podcastUuid = episode.podcastUuid, episodeUuid = episode.uuid)
        return (state as? ShowNotesState.Loaded)?.showNotes
    }

    private suspend fun currentUpNext(): UpNextResult {
        val queue = playbackManager.upNextQueue
        val currentEpisode = queue.currentEpisode
        val positionMs = currentEpisode?.let { playbackManager.getCurrentTimeMs(it) } ?: 0
        val currentRemainingSeconds = currentEpisode?.let { (it.duration - positionMs / 1000.0).coerceAtLeast(0.0) } ?: 0.0
        val queueRemainingSeconds = queue.queueEpisodes.sumOf { (it.duration - it.playedUpTo).coerceAtLeast(0.0) }
        return UpNextResult(
            currentEpisode = currentEpisode?.toEpisodeResult()?.copy(playedUpToSeconds = positionMs / 1000),
            isPlaying = playbackManager.isPlaying(),
            playbackSpeed = playbackManager.getPlaybackSpeed(),
            queue = queue.queueEpisodes.take(RESULT_LIMIT).toEpisodeResults(),
            queueSize = queue.queueEpisodes.size,
            currentChapterTitle = currentEpisode?.let {
                playbackManager.playbackStateFlow.first().chapters.getChapter(positionMs.milliseconds)?.title
            },
            upNextRemainingSeconds = (currentRemainingSeconds + queueRemainingSeconds).toInt(),
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
                isArchived = episode.isArchived,
            )
        }
    }

    private companion object {
        const val TAG = "AppFunctions"

        // TODO: Replace with a dedicated AppFunction source once EventHorizon's SourceViewType has one
        val SOURCE = SourceView.UNKNOWN

        // TODO: Replace with a dedicated AppFunction source once EventHorizon's BookmarkSourceType has one
        val BOOKMARK_SOURCE = BookmarkSourceType.Unknown

        const val RESULT_LIMIT = 20
        const val PLAYLIST_EPISODE_LIMIT = 50
        const val MAX_TRANSCRIPT_CHARS = 50_000
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
        const val ACTION_NEXT_CHAPTER = "NEXT_CHAPTER"
        const val ACTION_PREVIOUS_CHAPTER = "PREVIOUS_CHAPTER"
        const val ACTION_NEXT_EPISODE = "NEXT_EPISODE"

        const val MATCH_UP_NEXT = "UP_NEXT"
        const val MATCH_PODCAST_OR_EPISODE = "PODCAST_OR_EPISODE"
        const val MATCH_PLAYLIST = "PLAYLIST"
    }
}
