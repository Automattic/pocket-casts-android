package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.os.SystemClock
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.extensions.notModified
import au.com.shiftyjelly.pocketcasts.servers.extensions.wasCached
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.utils.DateUtil
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.tracks.crashlogging.CrashLogging
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class PodcastRefresherImpl @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val appDatabase: AppDatabase,
    private val cacheServiceManager: PodcastCacheServiceManager,
    private val crashLogging: CrashLogging,
) : PodcastRefresher {
    override suspend fun refreshPodcast(existingPodcast: Podcast, playbackManager: PlaybackManager) {
        try {
            val podcastResponse = cacheServiceManager.getPodcastResponse(existingPodcast.uuid)
            if (podcastResponse.wasCached() || podcastResponse.notModified()) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refreshing podcast ${existingPodcast.uuid} not required as cached")
                return
            }

            val updatedPodcast = podcastResponse.body()?.toPodcast()
            if (updatedPodcast == null) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Refreshing podcast ${existingPodcast.uuid} not required as no response")
                return
            }

            val startTime = SystemClock.elapsedRealtime()

            val originalPodcast = existingPodcast.copy()
            existingPodcast.title = updatedPodcast.title
            existingPodcast.author = updatedPodcast.author
            existingPodcast.podcastCategory = updatedPodcast.podcastCategory
            existingPodcast.podcastDescription = updatedPodcast.podcastDescription
            existingPodcast.estimatedNextEpisode = updatedPodcast.estimatedNextEpisode
            existingPodcast.episodeFrequency = updatedPodcast.episodeFrequency
            existingPodcast.refreshAvailable = updatedPodcast.refreshAvailable
            val existingEpisodes = episodeManager.findEpisodesByPodcastOrderedByPublishDate(existingPodcast)
            val mostRecentEpisode = existingEpisodes.firstOrNull()
            val insertEpisodes = mutableListOf<PodcastEpisode>()
            updatedPodcast.episodes.map { newEpisode ->
                val existingEpisode = existingEpisodes.find { it.uuid == newEpisode.uuid }
                if (existingEpisode != null) {
                    val originalEpisode = existingEpisode.copy()
                    existingEpisode.title = newEpisode.title
                    existingEpisode.downloadUrl = newEpisode.downloadUrl
                    // as new episodes are added a task is run to get the content type and file size from the server file as it is more reliable
                    if (existingEpisode.fileType.isNullOrBlank()) {
                        existingEpisode.fileType = newEpisode.fileType
                    }
                    if (existingEpisode.sizeInBytes <= 0) {
                        existingEpisode.sizeInBytes = newEpisode.sizeInBytes
                    }
                    if (existingEpisode.duration <= 0) {
                        existingEpisode.duration = newEpisode.duration
                    }
                    existingEpisode.publishedDate = newEpisode.publishedDate
                    existingEpisode.season = newEpisode.season
                    existingEpisode.number = newEpisode.number
                    existingEpisode.type = newEpisode.type
                    // only update the db if the fields have changed
                    if (originalEpisode != existingEpisode) {
                        episodeManager.update(existingEpisode)
                    }
                } else {
                    // don't add anything newer than the latest episode so it runs through the refresh logic (auto download, auto add to Up Next etc
                    if (!existingPodcast.isSubscribed || (mostRecentEpisode != null && newEpisode.publishedDate.before(mostRecentEpisode.publishedDate))) {
                        newEpisode.podcastUuid = existingPodcast.uuid
                        newEpisode.episodeStatus = EpisodeStatusEnum.NOT_DOWNLOADED
                        newEpisode.playingStatus = EpisodePlayingStatus.NOT_PLAYED

                        // for podcast you're subscribed to, if we find episodes older than a week, we add them in as archived so they don't flood your filters, etc
                        val newEpisodeIs7DaysOld = if (mostRecentEpisode != null) {
                            DateUtil.daysBetweenTwoDates(newEpisode.publishedDate, mostRecentEpisode.publishedDate) >= 7
                        } else {
                            true
                        }
                        newEpisode.isArchived = existingPodcast.isSubscribed && newEpisodeIs7DaysOld

                        newEpisode.archivedModified = Date().time
                        newEpisode.lastArchiveInteraction = Date().time
                        // give it an old added date so it doesn't trigger a new episode notification
                        newEpisode.addedDate = existingPodcast.addedDate ?: Date()
                        existingPodcast.addEpisode(newEpisode)
                        insertEpisodes.add(newEpisode)
                    }
                }
            }
            if (insertEpisodes.isNotEmpty()) {
                episodeManager.add(
                    episodes = insertEpisodes,
                    podcastUuid = existingPodcast.uuid,
                    downloadMetaData = false,
                )
            }
            val episodeUuidsToDelete = existingEpisodes.map { it.uuid }
                .subtract(updatedPodcast.episodes.map { it.uuid }.toSet())
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -14)
            val twoWeeksAgo = calendar.time
            val episodesToDelete =
                episodeUuidsToDelete.mapNotNull { uuid -> existingEpisodes.find { it.uuid == uuid } }
                    .filter {
                        it.addedDate.before(twoWeeksAgo) && episodeManager.episodeCanBeCleanedUp(it, playbackManager)
                    }
            if (episodesToDelete.isNotEmpty()) {
                episodeManager.deleteEpisodesWithoutSync(episodesToDelete, playbackManager)
            }

            if (originalPodcast != existingPodcast) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh required update for podcast ${existingPodcast.uuid}")
                appDatabase.podcastDao().updateSuspend(existingPodcast)
            }

            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refreshing podcast ${existingPodcast.uuid} - ${String.format(Locale.ENGLISH, "%d ms", SystemClock.elapsedRealtime() - startTime)}")
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Error refreshing podcast ${existingPodcast.uuid} in background")
            crashLogging.sendReport(e)
        }
    }
}
