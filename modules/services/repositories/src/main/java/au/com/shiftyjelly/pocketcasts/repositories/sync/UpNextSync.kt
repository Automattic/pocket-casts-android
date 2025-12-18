package au.com.shiftyjelly.pocketcasts.repositories.sync

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextChangeDao
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextChange
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.history.upnext.UpNextHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import au.com.shiftyjelly.pocketcasts.servers.extensions.toTimestamp
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncResponse
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import au.com.shiftyjelly.pocketcasts.utils.extensions.splitIgnoreEmpty
import au.com.shiftyjelly.pocketcasts.utils.extensions.switchInvalidForNow
import au.com.shiftyjelly.pocketcasts.utils.extensions.toIsoString
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.pocketcasts.service.api.UpNextChanges
import com.pocketcasts.service.api.UpNextChangesKt.change
import com.pocketcasts.service.api.UpNextResponse
import com.pocketcasts.service.api.upNextChanges
import com.pocketcasts.service.api.upNextEpisodeRequest
import com.pocketcasts.service.api.upNextSyncRequest
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitSingleOrNull
import retrofit2.HttpException
import com.pocketcasts.service.api.UpNextSyncRequest as UpNextProtobufSyncRequest

class UpNextSync @Inject constructor(
    private val appDatabase: AppDatabase,
    private val episodeManager: EpisodeManager,
    private val downloadManager: DownloadManager,
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
    private val syncManager: SyncManager,
    private val upNextQueue: UpNextQueue,
    private val userEpisodeManager: UserEpisodeManager,
    private val upNextHistoryManager: UpNextHistoryManager,
) {

    suspend fun sync() {
        if (FeatureFlag.isEnabled(Feature.UP_NEXT_SYNC_PROTOBUF)) {
            performProtobufSync()
        } else {
            performJsonSync()
        }
    }

    private suspend fun performJsonSync() {
        val upNextChangeDao = appDatabase.upNextChangeDao()
        val changes = upNextChangeDao.findAllBlocking()
        val request = buildJsonRequest(changes)
        try {
            val response = syncManager.upNextSync(request)
            readJsonResponse(response)
            clearJsonSyncedData(request, upNextChangeDao)
        } catch (e: HttpException) {
            if (e.code() != 304) throw e
        }
    }

    private suspend fun clearJsonSyncedData(upNextSyncRequest: UpNextSyncRequest, upNextChangeDao: UpNextChangeDao) {
        val latestChange = upNextSyncRequest.upNext.changes.maxByOrNull { it.modified }
        latestChange?.let {
            val latestActionTime = it.modified
            upNextChangeDao.deleteChangesOlderOrEqualTo(latestActionTime)
        }
    }

    private suspend fun buildJsonRequest(changes: List<UpNextChange>): UpNextSyncRequest {
        val requestChanges = mutableListOf<UpNextSyncRequest.Change>()
        for (change in changes) {
            requestChanges.add(buildJsonChangeRequest(change))
        }

        val serverModified = settings.getUpNextServerModified()
        val upNext = UpNextSyncRequest.UpNext(serverModified, requestChanges)
        val deviceTime = System.currentTimeMillis()
        val version = Settings.SYNC_API_VERSION.toString()
        return UpNextSyncRequest(deviceTime, version, upNext)
    }

    private suspend fun buildJsonChangeRequest(change: UpNextChange) = when (change.type) {
        UpNextChange.ACTION_REPLACE -> {
            val uuids = change.uuids?.splitIgnoreEmpty(",") ?: listOf()
            val episodes = uuids.map { uuid ->
                val episode = episodeManager.findEpisodeByUuid(uuid)
                val podcastUuid = if (episode is PodcastEpisode) episode.podcastUuid else Podcast.userPodcast.uuid
                UpNextSyncRequest.ChangeEpisode(
                    uuid = uuid,
                    title = episode?.title,
                    url = episode?.downloadUrl,
                    podcast = podcastUuid,
                    published = episode?.publishedDate?.toIsoString(),
                )
            }
            UpNextSyncRequest.Change(
                action = UpNextChange.ACTION_REPLACE,
                modified = change.modified,
                episodes = episodes,
            )
        }

        else -> {
            val uuid = change.uuid
            val episode = if (uuid == null) null else episodeManager.findEpisodeByUuid(uuid)
            val publishedDate = episode?.publishedDate?.switchInvalidForNow()?.toIsoString()
            val podcastUuid = if (episode is PodcastEpisode) episode.podcastUuid else Podcast.userPodcast.uuid
            UpNextSyncRequest.Change(
                action = change.type,
                modified = change.modified,
                uuid = change.uuid,
                title = episode?.title,
                url = episode?.downloadUrl,
                published = publishedDate,
                podcast = podcastUuid,
            )
        }
    }

    private suspend fun readJsonResponse(response: UpNextSyncResponse) {
        if (settings.getUpNextServerModified() == 0L && response.episodes.isNullOrEmpty() && playbackManager.getCurrentEpisode() != null) {
            // Server sent empty up next for first login and we have an up next list already, we should keep the local copy
            upNextQueue.changeList(playbackManager.upNextQueue.queueEpisodes) // Change list will automatically include the current episode
            return
        }

        if (!response.hasChanged(settings.getUpNextServerModified())) {
            return
        }

        val episodesMatch = serverAndLocalEpisodesMatch(
            serverUuids = response.episodes?.map { it.uuid }.orEmpty(),
            localUuids = upNextQueue.allEpisodes.map { it.uuid },
        )

        if (!episodesMatch) {
            // import missing podcasts
            val serverPodcastUuids: List<String> = response.episodes?.mapNotNull { it.podcast }.orEmpty()
            importMissingPodcasts(serverPodcastUuids)

            // import missing episodes
            val episodes = response.episodes?.mapNotNull { responseEpisode ->
                val episodeUuid = responseEpisode.uuid
                val podcastUuid = responseEpisode.podcast
                importMissingEpisode(podcastUuid = podcastUuid, episodeUuid = episodeUuid, title = responseEpisode.title, published = responseEpisode.published?.parseIsoDate()) { podcastUuid ->
                    responseEpisode.toSkeletonEpisode(podcastUuid)
                }
            }.orEmpty()

            processResponseEpisodes(episodes)
        }

        // save the server Up Next modified so we only apply changes
        settings.setUpNextServerModified(response.serverModified)
    }

    private suspend fun performProtobufSync() {
        val upNextChangeDao = appDatabase.upNextChangeDao()
        val changes = upNextChangeDao.findAllBlocking()
        val request = buildProtobufRequest(changes)
        try {
            val response = syncManager.upNextSyncProtobuf(request)
            readProtobufResponse(response)
            clearProtobufSyncedData(request, upNextChangeDao)
        } catch (e: HttpException) {
            if (e.code() != 304) throw e
        }
    }

    private suspend fun clearProtobufSyncedData(upNextSyncRequest: UpNextProtobufSyncRequest, upNextChangeDao: UpNextChangeDao) {
        val latestChange = upNextSyncRequest.upNext.changesList.maxByOrNull { it.modified }
        latestChange?.let {
            val latestActionTime = it.modified
            upNextChangeDao.deleteChangesOlderOrEqualTo(latestActionTime)
        }
    }

    private suspend fun buildProtobufRequest(changesList: List<UpNextChange>): UpNextProtobufSyncRequest {
        return upNextSyncRequest {
            deviceTime = System.currentTimeMillis()
            version = Settings.SYNC_API_VERSION.toString()
            model = Settings.SYNC_API_MODEL
            deviceId = settings.getUniqueDeviceId()
            upNext = upNextChanges {
                serverModified = settings.getUpNextServerModified()
                changesList.forEach { change ->
                    val changeRequest = buildProtobufChangeRequest(change) ?: return@forEach
                    changes += changeRequest
                }
            }
        }
    }

    private suspend fun buildProtobufChangeRequest(upNextChange: UpNextChange): UpNextChanges.Change? = when (upNextChange.type) {
        UpNextChange.ACTION_REPLACE -> {
            val uuids = upNextChange.uuids?.splitIgnoreEmpty(",") ?: listOf()
            val requestEpisodes = uuids.map { episodeUuid ->
                val episode = episodeManager.findEpisodeByUuid(episodeUuid)
                val podcastUuid = if (episode is PodcastEpisode) episode.podcastUuid else Podcast.userPodcast.uuid
                upNextEpisodeRequest {
                    uuid = episodeUuid
                    title = episode?.title.orEmpty()
                    url = episode?.downloadUrl.orEmpty()
                    podcast = podcastUuid
                    published = (episode?.publishedDate ?: Date()).toTimestamp()
                }
            }
            change {
                action = UpNextChange.ACTION_REPLACE
                modified = upNextChange.modified
                episodes += requestEpisodes
            }
        }

        else -> {
            val episodeUuid = upNextChange.uuid ?: return null
            val episode = episodeManager.findEpisodeByUuid(episodeUuid)
            val podcastUuid = if (episode is PodcastEpisode) episode.podcastUuid else Podcast.userPodcast.uuid
            change {
                action = upNextChange.type
                modified = upNextChange.modified
                uuid = episodeUuid
                title = episode?.title.orEmpty()
                url = episode?.downloadUrl.orEmpty()
                published = (episode?.publishedDate ?: Date()).toTimestamp()
                podcast = podcastUuid
            }
        }
    }

    private suspend fun readProtobufResponse(response: UpNextResponse) {
        if (settings.getUpNextServerModified() == 0L && response.episodesList.isNullOrEmpty() && playbackManager.getCurrentEpisode() != null) {
            // Server sent empty up next for first login and we have an up next list already, we should keep the local copy
            upNextQueue.changeList(playbackManager.upNextQueue.queueEpisodes) // Change list will automatically include the current episode
            return
        }

        if (response.serverModified == settings.getUpNextServerModified()) {
            return
        }

        val episodesMatch = serverAndLocalEpisodesMatch(
            serverUuids = response.episodesList?.map { it.uuid }.orEmpty(),
            localUuids = upNextQueue.allEpisodes.map { it.uuid },
        )

        if (!episodesMatch) {
            // import missing podcasts
            val serverPodcastUuids: List<String> = response.episodesList?.mapNotNull { it.podcast }.orEmpty()
            importMissingPodcasts(serverPodcastUuids)

            // import missing episodes
            val episodes = response.episodesList?.mapNotNull { responseEpisode ->
                val episodeUuid = responseEpisode.uuid
                val podcastUuid = responseEpisode.podcast
                importMissingEpisode(podcastUuid = podcastUuid, episodeUuid = episodeUuid, title = responseEpisode.title, published = responseEpisode.published?.toDate()) { podcastUuid ->
                    responseEpisode.toSkeletonEpisode(podcastUuid)
                }
            }.orEmpty()

            processResponseEpisodes(episodes)
        }

        // save the server Up Next modified so we only apply changes
        settings.setUpNextServerModified(response.serverModified)
    }

    private suspend fun processResponseEpisodes(episodes: List<BaseEpisode>) {
        // snapshot local up next queue before  making changes
        upNextHistoryManager.snapshotUpNext()
        // import the server Up Next into the database
        upNextQueue.importServerChangesBlocking(episodes, playbackManager, downloadManager)
        // check the current episode is correct
        playbackManager.loadQueue()
    }

    internal fun serverAndLocalEpisodesMatch(serverUuids: List<String>, localUuids: List<String>): Boolean {
        return serverUuids == localUuids
    }

    internal suspend fun importMissingPodcasts(podcastUuids: List<String>) {
        // remove user episodes
        val filteredUuids = podcastUuids.filter { it != Podcast.userPodcast.uuid }
        filteredUuids.forEach { podcastUuid ->
            podcastManager.findOrDownloadPodcastRxSingle(podcastUuid).await()
        }
    }

    internal suspend fun importMissingEpisode(podcastUuid: String?, episodeUuid: String, title: String?, published: Date?, buildSkeletonPodcastEpisode: (String) -> PodcastEpisode): BaseEpisode? {
        if (podcastUuid == null) {
            return null
        }
        return if (podcastUuid == Podcast.userPodcast.uuid) {
            userEpisodeManager.downloadMissingUserEpisodeRxMaybe(uuid = episodeUuid, placeholderTitle = title, placeholderPublished = published)
                .awaitSingleOrNull()
        } else {
            val skeletonEpisode = buildSkeletonPodcastEpisode(podcastUuid)
            episodeManager.downloadMissingEpisodeRxMaybe(episodeUuid = episodeUuid, podcastUuid = podcastUuid, skeletonEpisode = skeletonEpisode, podcastManager = podcastManager, downloadMetaData = false, source = SourceView.UP_NEXT)
                .awaitSingleOrNull()
        }
    }
}

private fun UpNextResponse.EpisodeResponse.toSkeletonEpisode(podcastUuid: String): PodcastEpisode {
    return PodcastEpisode(
        uuid = uuid,
        publishedDate = published.toDate() ?: Date(),
        addedDate = Date(),
        playingStatus = EpisodePlayingStatus.NOT_PLAYED,
        episodeStatus = EpisodeStatusEnum.NOT_DOWNLOADED,
        title = title.orEmpty(),
        downloadUrl = url.orEmpty(),
        podcastUuid = podcastUuid,
    )
}
