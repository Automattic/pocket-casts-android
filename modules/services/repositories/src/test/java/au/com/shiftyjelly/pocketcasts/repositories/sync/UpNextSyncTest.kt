package au.com.shiftyjelly.pocketcasts.repositories.sync

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextChangeDao
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextChange
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.history.upnext.UpNextHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncResponse
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.google.protobuf.Timestamp
import com.pocketcasts.service.api.UpNextResponse
import com.pocketcasts.service.api.upNextResponse
import io.reactivex.Maybe
import io.reactivex.Single
import java.util.Date
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

class UpNextSyncTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Mock
    private lateinit var appDatabase: AppDatabase

    @Mock
    private lateinit var upNextChangeDao: UpNextChangeDao

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var playbackManager: PlaybackManager

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var syncManager: SyncManager

    @Mock
    private lateinit var upNextQueue: UpNextQueue

    @Mock
    private lateinit var userEpisodeManager: UserEpisodeManager

    @Mock
    private lateinit var upNextHistoryManager: UpNextHistoryManager

    private lateinit var upNextSync: UpNextSync

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        whenever(appDatabase.upNextChangeDao()).thenReturn(upNextChangeDao)
        whenever(settings.getUniqueDeviceId()).thenReturn("device-123")

        upNextSync = UpNextSync(
            appDatabase = appDatabase,
            episodeManager = episodeManager,
            playbackManager = playbackManager,
            podcastManager = podcastManager,
            settings = settings,
            syncManager = syncManager,
            upNextQueue = upNextQueue,
            userEpisodeManager = userEpisodeManager,
            upNextHistoryManager = upNextHistoryManager,
        )
    }

    // Feature Flag Routing Tests

    @Test
    fun `sync uses JSON implementation when protobuf feature flag is disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.UP_NEXT_SYNC_PROTOBUF, false)

        whenever(upNextChangeDao.findAllBlocking()).thenReturn(emptyList())
        whenever(settings.getUpNextServerModified()).thenReturn(0L)
        whenever(syncManager.upNextSync(any())).thenReturn(createJsonResponse())

        upNextSync.sync()

        verify(syncManager).upNextSync(any())
        verify(syncManager, never()).upNextSyncProtobuf(any())
    }

    @Test
    fun `sync uses protobuf implementation when feature flag is enabled`() = runTest {
        FeatureFlag.setEnabled(Feature.UP_NEXT_SYNC_PROTOBUF, true)

        whenever(upNextChangeDao.findAllBlocking()).thenReturn(emptyList())
        whenever(settings.getUpNextServerModified()).thenReturn(0L)

        whenever(syncManager.upNextSyncProtobuf(any())).thenReturn(createProtobufResponse())

        upNextSync.sync()

        verify(syncManager, never()).upNextSync(any())
        verify(syncManager).upNextSyncProtobuf(any())
    }

    // JSON Sync Tests

    @Test
    fun `performJsonSync handles 304 Not Modified response`() = runTest {
        FeatureFlag.setEnabled(Feature.UP_NEXT_SYNC_PROTOBUF, false)

        whenever(upNextChangeDao.findAllBlocking()).thenReturn(emptyList())
        whenever(settings.getUpNextServerModified()).thenReturn(123L)
        whenever(syncManager.upNextSync(any())).thenThrow(httpExceptionNotModified())

        upNextSync.sync()

        // Should not throw exception, 304 is handled gracefully
        verify(settings, never()).setUpNextServerModified(any())
    }

    @Test
    fun `performJsonSync clears synced changes after successful sync`() = runTest {
        FeatureFlag.setEnabled(Feature.UP_NEXT_SYNC_PROTOBUF, false)

        val change1 = createUpNextChange(
            type = UpNextChange.ACTION_PLAY_NEXT,
            uuid = "episode1",
            modified = 1000L,
        )
        val change2 = createUpNextChange(
            type = UpNextChange.ACTION_PLAY_LAST,
            uuid = "episode2",
            modified = 2000L,
        )

        whenever(upNextChangeDao.findAllBlocking()).thenReturn(listOf(change1, change2))
        whenever(settings.getUpNextServerModified()).thenReturn(0L)
        whenever(episodeManager.findEpisodeByUuid(any())).thenReturn(createPodcastEpisode())
        whenever(syncManager.upNextSync(any())).thenReturn(createJsonResponse())

        upNextSync.sync()

        verify(upNextChangeDao).deleteChangesOlderOrEqualTo(2000L)
    }

    @Test
    fun `buildJsonRequest creates REPLACE change with multiple episodes`() = runTest {
        FeatureFlag.setEnabled(Feature.UP_NEXT_SYNC_PROTOBUF, false)

        val episode1 = createPodcastEpisode(uuid = "ep1", title = "Episode 1")
        val episode2 = createPodcastEpisode(uuid = "ep2", title = "Episode 2")

        val change = createUpNextChange(
            type = UpNextChange.ACTION_REPLACE,
            uuids = "ep1,ep2",
            modified = 1000L,
        )

        whenever(upNextChangeDao.findAllBlocking()).thenReturn(listOf(change))
        whenever(settings.getUpNextServerModified()).thenReturn(0L)
        whenever(episodeManager.findEpisodeByUuid("ep1")).thenReturn(episode1)
        whenever(episodeManager.findEpisodeByUuid("ep2")).thenReturn(episode2)
        whenever(syncManager.upNextSync(any())).thenReturn(createJsonResponse())

        upNextSync.sync()

        verify(syncManager).upNextSync(any())
    }

    // Protobuf Sync Tests

    @Test
    fun `performProtobufSync handles 304 Not Modified response`() = runTest {
        FeatureFlag.setEnabled(Feature.UP_NEXT_SYNC_PROTOBUF, true)

        whenever(upNextChangeDao.findAllBlocking()).thenReturn(emptyList())
        whenever(settings.getUpNextServerModified()).thenReturn(123L)
        whenever(syncManager.upNextSyncProtobuf(any())).thenThrow(httpExceptionNotModified())

        upNextSync.sync()

        // Should not throw exception, 304 is handled gracefully
        verify(settings, never()).setUpNextServerModified(any())
    }

    @Test
    fun `performProtobufSync clears synced changes after successful sync`() = runTest {
        FeatureFlag.setEnabled(Feature.UP_NEXT_SYNC_PROTOBUF, true)

        val change1 = createUpNextChange(
            type = UpNextChange.ACTION_PLAY_NEXT,
            uuid = "episode1",
            modified = 1000L,
        )
        val change2 = createUpNextChange(
            type = UpNextChange.ACTION_PLAY_LAST,
            uuid = "episode2",
            modified = 2000L,
        )

        whenever(upNextChangeDao.findAllBlocking()).thenReturn(listOf(change1, change2))
        whenever(settings.getUpNextServerModified()).thenReturn(0L)
        whenever(episodeManager.findEpisodeByUuid(any())).thenReturn(createPodcastEpisode())
        whenever(syncManager.upNextSyncProtobuf(any())).thenReturn(createProtobufResponse())

        upNextSync.sync()

        verify(upNextChangeDao).deleteChangesOlderOrEqualTo(2000L)
    }

    @Test
    fun `buildProtobufRequest creates REPLACE change with multiple episodes`() = runTest {
        FeatureFlag.setEnabled(Feature.UP_NEXT_SYNC_PROTOBUF, true)

        val episode1 = createPodcastEpisode(uuid = "ep1", title = "Episode 1")
        val episode2 = createPodcastEpisode(uuid = "ep2", title = "Episode 2")

        val change = createUpNextChange(
            type = UpNextChange.ACTION_REPLACE,
            uuids = "ep1,ep2",
            modified = 1000L,
        )

        whenever(upNextChangeDao.findAllBlocking()).thenReturn(listOf(change))
        whenever(settings.getUpNextServerModified()).thenReturn(0L)
        whenever(episodeManager.findEpisodeByUuid("ep1")).thenReturn(episode1)
        whenever(episodeManager.findEpisodeByUuid("ep2")).thenReturn(episode2)
        whenever(syncManager.upNextSyncProtobuf(any())).thenReturn(createProtobufResponse())

        upNextSync.sync()

        verify(syncManager).upNextSyncProtobuf(any())
    }

    // Response Processing Tests

    @Test
    fun `readJsonResponse keeps local queue on first login with empty server response`() = runTest {
        FeatureFlag.setEnabled(Feature.UP_NEXT_SYNC_PROTOBUF, false)

        val currentEpisode = createPodcastEpisode(uuid = "current")
        val queueEpisodes = listOf(createPodcastEpisode(uuid = "next"))

        whenever(upNextChangeDao.findAllBlocking()).thenReturn(emptyList())
        whenever(settings.getUpNextServerModified()).thenReturn(0L) // First login
        whenever(playbackManager.getCurrentEpisode()).thenReturn(currentEpisode)
        whenever(playbackManager.upNextQueue).thenReturn(upNextQueue)
        whenever(upNextQueue.queueEpisodes).thenReturn(queueEpisodes)
        whenever(syncManager.upNextSync(any())).thenReturn(
            createJsonResponse(episodes = emptyList(), serverModified = 1000L),
        )

        upNextSync.sync()

        verify(upNextQueue).changeList(queueEpisodes)
        verify(settings, never()).setUpNextServerModified(any())
    }

    @Test
    fun `readJsonResponse returns early when server modified hasn't changed`() = runTest {
        FeatureFlag.setEnabled(Feature.UP_NEXT_SYNC_PROTOBUF, false)

        val serverModified = 5000L

        whenever(upNextChangeDao.findAllBlocking()).thenReturn(emptyList())
        whenever(settings.getUpNextServerModified()).thenReturn(serverModified)
        whenever(syncManager.upNextSync(any())).thenReturn(
            createJsonResponse(serverModified = serverModified),
        )

        upNextSync.sync()

        verify(upNextQueue, never()).importServerChangesBlocking(any(), any())
        verify(playbackManager, never()).loadQueue()
    }

    @Test
    fun `readProtobufResponse keeps local queue on first login with empty server response`() = runTest {
        FeatureFlag.setEnabled(Feature.UP_NEXT_SYNC_PROTOBUF, true)

        val currentEpisode = createPodcastEpisode(uuid = "current")
        val queueEpisodes = listOf(createPodcastEpisode(uuid = "next"))

        whenever(upNextChangeDao.findAllBlocking()).thenReturn(emptyList())
        whenever(settings.getUpNextServerModified()).thenReturn(0L) // First login
        whenever(playbackManager.getCurrentEpisode()).thenReturn(currentEpisode)
        whenever(playbackManager.upNextQueue).thenReturn(upNextQueue)
        whenever(upNextQueue.queueEpisodes).thenReturn(queueEpisodes)
        whenever(syncManager.upNextSyncProtobuf(any())).thenReturn(
            createProtobufResponse(episodes = emptyList(), serverModified = 1000L),
        )

        upNextSync.sync()

        verify(upNextQueue).changeList(queueEpisodes)
        verify(settings, never()).setUpNextServerModified(any())
    }

    @Test
    fun `readProtobufResponse returns early when server modified hasn't changed`() = runTest {
        FeatureFlag.setEnabled(Feature.UP_NEXT_SYNC_PROTOBUF, true)

        val serverModified = 5000L

        whenever(upNextChangeDao.findAllBlocking()).thenReturn(emptyList())
        whenever(settings.getUpNextServerModified()).thenReturn(serverModified)
        whenever(syncManager.upNextSyncProtobuf(any())).thenReturn(
            createProtobufResponse(serverModified = serverModified),
        )

        upNextSync.sync()

        verify(upNextQueue, never()).importServerChangesBlocking(any(), any())
        verify(playbackManager, never()).loadQueue()
    }

    // Episode Matching Tests

    @Test
    fun `serverAndLocalEpisodesMatch returns true when episodes match exactly`() = runTest {
        val serverUuids = listOf("ep1", "ep2", "ep3")
        val localUuids = listOf("ep1", "ep2", "ep3")

        val result = upNextSync.serverAndLocalEpisodesMatch(serverUuids, localUuids)

        assertTrue(result)
    }

    @Test
    fun `serverAndLocalEpisodesMatch returns false when episodes order doesn't match`() = runTest {
        val serverUuids = listOf("ep1", "ep2", "ep3")
        val localUuids = listOf("ep3", "ep1", "ep2")

        val result = upNextSync.serverAndLocalEpisodesMatch(serverUuids, localUuids)

        assertFalse(result)
    }

    @Test
    fun `serverAndLocalEpisodesMatch returns false when counts differ`() = runTest {
        val serverUuids = listOf("ep1", "ep2", "ep3")
        val localUuids = listOf("ep1", "ep2")

        val result = upNextSync.serverAndLocalEpisodesMatch(serverUuids, localUuids)

        assertFalse(result)
    }

    @Test
    fun `serverAndLocalEpisodesMatch returns false when episodes are different`() = runTest {
        val serverUuids = listOf("ep1", "ep2", "ep3")
        val localUuids = listOf("ep1", "ep2", "ep4")

        val result = upNextSync.serverAndLocalEpisodesMatch(serverUuids, localUuids)

        assertFalse(result)
    }

    @Test
    fun `serverAndLocalEpisodesMatch returns true for empty lists`() = runTest {
        val serverUuids = emptyList<String>()
        val localUuids = emptyList<String>()

        val result = upNextSync.serverAndLocalEpisodesMatch(serverUuids, localUuids)

        assertTrue(result)
    }

    @Test
    fun `serverAndLocalEpisodesMatch returns false when server has episodes but local is empty`() = runTest {
        val serverUuids = listOf("ep1", "ep2")
        val localUuids = emptyList<String>()

        val result = upNextSync.serverAndLocalEpisodesMatch(serverUuids, localUuids)

        assertFalse(result)
    }

    // Import Podcasts Tests

    @Test
    fun `importMissingPodcasts downloads missing podcasts`() = runTest {
        val podcast1 = createPodcast(uuid = "podcast1")
        val podcast2 = createPodcast(uuid = "podcast2")

        whenever(podcastManager.findOrDownloadPodcastRxSingle("podcast1")).thenReturn(Single.just(podcast1))
        whenever(podcastManager.findOrDownloadPodcastRxSingle("podcast2")).thenReturn(Single.just(podcast2))

        upNextSync.importMissingPodcasts(listOf("podcast1", "podcast2"))

        verify(podcastManager).findOrDownloadPodcastRxSingle("podcast1")
        verify(podcastManager).findOrDownloadPodcastRxSingle("podcast2")
    }

    @Test
    fun `importMissingPodcasts filters out user podcast uuid`() = runTest {
        val podcast = createPodcast(uuid = "podcast1")

        whenever(podcastManager.findOrDownloadPodcastRxSingle("podcast1")).thenReturn(Single.just(podcast))

        upNextSync.importMissingPodcasts(listOf("podcast1", Podcast.userPodcast.uuid))

        verify(podcastManager).findOrDownloadPodcastRxSingle("podcast1")
        verify(podcastManager, never()).findOrDownloadPodcastRxSingle(Podcast.userPodcast.uuid)
    }

    // Import Episode Tests

    @Test
    fun `importMissingEpisode returns null when podcastUuid is null`() = runTest {
        val result = upNextSync.importMissingEpisode(
            podcastUuid = null,
            episodeUuid = "episode1",
            title = "Test Episode",
            published = Date(),
        ) { createPodcastEpisode() }

        assertEquals(null, result)
        verify(userEpisodeManager, never()).downloadMissingUserEpisodeRxMaybe(any(), anyOrNull(), anyOrNull())
        verify(episodeManager, never()).downloadMissingEpisodeRxMaybe(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `importMissingEpisode downloads user episode when podcast uuid is user podcast`() = runTest {
        val userEpisode = createUserEpisode(uuid = "episode1")
        val published = Date()

        whenever(
            userEpisodeManager.downloadMissingUserEpisodeRxMaybe(
                uuid = "episode1",
                placeholderTitle = "Test Episode",
                placeholderPublished = published,
            ),
        ).thenReturn(Maybe.just(userEpisode))

        val result = upNextSync.importMissingEpisode(
            podcastUuid = Podcast.userPodcast.uuid,
            episodeUuid = "episode1",
            title = "Test Episode",
            published = published,
        ) { createPodcastEpisode() }

        assertEquals(userEpisode, result)
        verify(userEpisodeManager).downloadMissingUserEpisodeRxMaybe(
            uuid = eq("episode1"),
            placeholderTitle = eq("Test Episode"),
            placeholderPublished = any(),
        )
    }

    @Test
    fun `importMissingEpisode downloads podcast episode when podcast uuid is regular podcast`() = runTest {
        val episode = createPodcastEpisode(uuid = "episode1", podcastUuid = "podcast1")

        whenever(
            episodeManager.downloadMissingEpisodeRxMaybe(
                episodeUuid = eq("episode1"),
                podcastUuid = eq("podcast1"),
                skeletonEpisode = any(),
                podcastManager = eq(podcastManager),
                downloadMetaData = eq(false),
                source = any(),
            ),
        ).thenReturn(Maybe.just(episode))

        val result = upNextSync.importMissingEpisode(
            podcastUuid = "podcast1",
            episodeUuid = "episode1",
            title = "Test Episode",
            published = Date(),
        ) { createPodcastEpisode(uuid = "episode1", podcastUuid = "podcast1") }

        assertEquals(episode, result)
        verify(episodeManager).downloadMissingEpisodeRxMaybe(
            episodeUuid = eq("episode1"),
            podcastUuid = eq("podcast1"),
            skeletonEpisode = any(),
            podcastManager = eq(podcastManager),
            downloadMetaData = eq(false),
            source = any(),
        )
    }

    // Helper Functions

    private fun createUpNextChange(
        type: Int,
        uuid: String? = null,
        uuids: String? = null,
        modified: Long = System.currentTimeMillis(),
    ): UpNextChange {
        return UpNextChange(
            id = null,
            type = type,
            uuid = uuid,
            uuids = uuids,
            modified = modified,
        )
    }

    private fun createPodcast(uuid: String = "podcast-uuid"): Podcast {
        return Podcast(uuid = uuid)
    }

    private fun createPodcastEpisode(
        uuid: String = "episode-uuid",
        podcastUuid: String = "podcast-uuid",
        title: String = "Test Episode",
    ): PodcastEpisode {
        return PodcastEpisode(
            uuid = uuid,
            podcastUuid = podcastUuid,
            publishedDate = Date(),
            title = title,
            downloadUrl = "https://example.com/$uuid.mp3",
            playingStatus = EpisodePlayingStatus.NOT_PLAYED,
            downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        )
    }

    private fun createUserEpisode(
        uuid: String = "user-episode-uuid",
        title: String = "User Episode",
    ): UserEpisode {
        return UserEpisode(
            uuid = uuid,
            publishedDate = Date(),
            title = title,
            downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        )
    }

    private fun createJsonResponse(
        episodes: List<UpNextSyncResponse.Episode>? = emptyList(),
        serverModified: Long = 1000L,
    ): UpNextSyncResponse {
        return UpNextSyncResponse(
            episodes = episodes,
            serverModified = serverModified,
        )
    }

    private fun createProtobufResponse(
        episodes: List<UpNextResponse.EpisodeResponse> = emptyList(),
        serverModified: Long = 1000L,
    ): UpNextResponse {
        return upNextResponse {
            this.serverModified = serverModified
            this.episodes += episodes
        }
    }

    private fun createProtobufEpisode(
        uuid: String = "episode-uuid",
        podcastUuid: String = "podcast-uuid",
        title: String = "Test Episode",
    ): UpNextResponse.EpisodeResponse {
        return UpNextResponse.EpisodeResponse
            .newBuilder()
            .setUuid(uuid)
            .setPodcast(podcastUuid)
            .setTitle(title)
            .setUrl("https://example.com/$uuid.mp3")
            .setPublished(
                Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build(),
            )
            .build()
    }

    private fun httpExceptionNotModified() = HttpException(
        Response.error<Any>(
            "".toResponseBody(),
            okhttp3.Response.Builder()
                .code(304)
                .message("Not Modified")
                .protocol(Protocol.HTTP_1_1)
                .request(Request.Builder().url("http://localhost/").build())
                .build(),
        ),
    )
}
