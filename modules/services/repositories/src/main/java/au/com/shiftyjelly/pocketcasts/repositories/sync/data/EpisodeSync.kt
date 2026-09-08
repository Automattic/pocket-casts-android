package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.protobuf.boolValue
import com.google.protobuf.int32Value
import com.google.protobuf.int64Value
import com.pocketcasts.service.api.Record
import com.pocketcasts.service.api.SyncUserEpisode
import com.pocketcasts.service.api.durationOrNull
import com.pocketcasts.service.api.isDeletedOrNull
import com.pocketcasts.service.api.playedUpToOrNull
import com.pocketcasts.service.api.playingStatusOrNull
import com.pocketcasts.service.api.podcastsEpisodesRequest
import com.pocketcasts.service.api.record
import com.pocketcasts.service.api.starredModifiedOrNull
import com.pocketcasts.service.api.starredOrNull
import com.pocketcasts.service.api.syncUserEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class EpisodeSync(
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
    private val syncManager: SyncManager,
) {
    suspend fun incrementalData(): List<Record> {
        val episodes = episodeManager.findEpisodesToSync()
        return withContext(Dispatchers.Default) {
            episodes.map { localEpisode ->
                record {
                    episode = syncUserEpisode {
                        uuid = localEpisode.uuid
                        podcastUuid = localEpisode.podcastUuid
                        localEpisode.playingStatusModified?.let { modifiedAt ->
                            playingStatus = int32Value { value = localEpisode.playingStatus.toInt() }
                            playingStatusModified = int64Value { value = modifiedAt }
                        }
                        localEpisode.starredModified?.let { modifiedAt ->
                            starred = boolValue { value = localEpisode.isStarred }
                            starredModified = int64Value { value = modifiedAt }
                        }
                        localEpisode.playedUpToModified?.let { modifiedAt ->
                            playedUpTo = int64Value { value = localEpisode.playedUpTo.toLong() }
                            playedUpToModified = int64Value { value = modifiedAt }
                        }
                        localEpisode.durationModified?.takeIf { localEpisode.duration != 0.0 }?.let { modifiedAt ->
                            duration = int64Value { value = localEpisode.duration.toLong() }
                            durationModified = int64Value { value = modifiedAt }
                        }
                        localEpisode.archivedModified?.let { modifiedAt ->
                            isDeleted = boolValue { value = localEpisode.isArchived }
                            isDeletedModified = int64Value { value = modifiedAt }
                        }
                        localEpisode.deselectedChaptersModified?.let { modifiedAt ->
                            deselectedChapters = ChapterIndices.toString(localEpisode.deselectedChapters)
                            deselectedChaptersModified = int64Value { value = modifiedAt.time }
                        }
                    }
                }
            }
        }
    }

    suspend fun processIncrementalResponse(serverEpisodes: List<SyncUserEpisode>) {
        val serverEpisodesMap = serverEpisodes.associateBy(SyncUserEpisode::getUuid)
        val presentEpisodes = episodeManager.findByUuids(serverEpisodesMap.keys)
        val presentUuids = presentEpisodes.mapTo(mutableSetOf(), PodcastEpisode::uuid)
        val localEpisodes = presentEpisodes + fetchMissingEpisodes(serverEpisodesMap, presentUuids)

        val episodesToArchive = mutableListOf<PodcastEpisode>()
        val episodeToFinish = mutableListOf<PodcastEpisode>()
        localEpisodes.forEach { localEpisode ->
            val serverEpisode = serverEpisodesMap[localEpisode.uuid] ?: return@forEach
            localEpisode.applyServerEpisode(
                serverEpisode,
                onShouldBeArchived = { episodesToArchive += localEpisode },
                onShouldBeFinished = { episodeToFinish += localEpisode },
            )
        }
        episodeManager.archiveAllInList(episodesToArchive, playbackManager)
        episodesToArchive.forEach { episode ->
            episode.isArchived = true
        }
        episodeToFinish.forEach { episode ->
            episodeManager.markedAsPlayedExternally(episode, playbackManager, podcastManager)
        }
        episodeManager.updateAllSyncFields(localEpisodes)
    }

    private suspend fun fetchMissingEpisodes(
        serverEpisodesMap: Map<String, SyncUserEpisode>,
        presentUuids: Set<String>,
    ): List<PodcastEpisode> {
        val missingEpisodes = serverEpisodesMap.values.filter { it.uuid !in presentUuids }
        if (missingEpisodes.isEmpty()) {
            return emptyList()
        }
        val subscribedPodcastUuids = podcastManager.findSubscribedUuids().toSet()
        val episodesToFetch = missingEpisodes.filter { it.podcastUuid in subscribedPodcastUuids }
        if (episodesToFetch.isEmpty()) {
            return emptyList()
        }
        return runCatching {
            episodesToFetch.chunked(MISSING_EPISODES_BATCH_SIZE).flatMap { batch ->
                val request = podcastsEpisodesRequest {
                    batch.forEach { episode ->
                        podcastUuids.add(episode.podcastUuid)
                        episodeUuids.add(episode.uuid)
                    }
                }
                syncManager.getEpisodesOrThrow(request).episodesList.map { serverEpisode ->
                    serverEpisode.toPodcastEpisode().copy(
                        playingStatus = EpisodePlayingStatus.NOT_PLAYED,
                        playedUpTo = 0.0,
                        isStarred = false,
                        isArchived = false,
                    )
                }
            }.also { shells ->
                shells.groupBy(PodcastEpisode::podcastUuid).forEach { (podcastUuid, episodes) ->
                    episodeManager.add(episodes, podcastUuid, downloadMetaData = false)
                }
            }
        }.getOrElse { error ->
            LogBuffer.e("DataSync", error, "Failed to fetch missing episodes during incremental sync")
            emptyList()
        }
    }

    private fun PodcastEpisode.applyServerEpisode(
        serverEpisode: SyncUserEpisode,
        onShouldBeArchived: () -> Unit,
        onShouldBeFinished: () -> Unit,
    ) = apply {
        val isEpisodePlaying = playbackManager.isPlaying() && playbackManager.getCurrentEpisode()?.uuid == uuid
        val seekThresholdSecs = settings.getPlaybackEpisodePositionChangedOnSyncThresholdSecs()

        serverEpisode.starredOrNull?.value?.let { value ->
            isStarred = value
            starredModified = null
            serverEpisode.starredModifiedOrNull?.value?.let { lastStarredDate = it }
        }
        serverEpisode.durationOrNull?.value?.takeIf { it > 0 }?.let { value ->
            duration = value.toDouble()
            durationModified = null
        }
        serverEpisode.deselectedChapters?.let { value ->
            deselectedChapters = ChapterIndices.fromString(value)
            deselectedChaptersModified = null
        }
        serverEpisode.isDeletedOrNull?.value
            ?.also { archivedModified = null }
            ?.takeIf { it != isArchived }
            ?.let { value ->
                if (isEpisodePlaying) {
                    // If we're playing this episode, marked the archive status as unsynced because the server might have a different one to us now
                    archivedModified = System.currentTimeMillis()
                } else {
                    if (value) {
                        onShouldBeArchived()
                    } else {
                        isArchived = false
                        lastArchiveInteraction = System.currentTimeMillis()
                    }
                }
            }
        serverEpisode.playingStatusOrNull?.value
            ?.also { playingStatusModified = null }
            ?.let(EpisodePlayingStatus::fromInt)
            ?.takeIf { it != playingStatus }
            ?.let { value ->
                if (isEpisodePlaying) {
                    playingStatusModified = System.currentTimeMillis()
                } else {
                    playingStatus = value
                    if (isFinished) {
                        onShouldBeFinished()
                    }
                }
            }
        serverEpisode.playedUpToOrNull?.value
            ?.also { playedUpToModified = null }
            ?.toDouble()
            ?.takeIf { !isEpisodePlaying && it >= 0 && it !in (playedUpTo - seekThresholdSecs)..(playedUpTo + 2) }
            ?.let { value ->
                playedUpTo = value
                playbackManager.seekIfPlayingToTimeMs(uuid, playedUpToMs)
            }
    }

    companion object {
        private const val MISSING_EPISODES_BATCH_SIZE = 100
    }
}
