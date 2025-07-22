package au.com.shiftyjelly.pocketcasts.repositories.playlist

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PlaylistDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.AUDIO_VIDEO_FILTER_VIDEO_ONLY
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import com.squareup.moshi.Moshi
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaylistManagerTest {
    private val testDispatcher = StandardTestDispatcher()

    private val clock = MutableClock()
    private lateinit var podcastDao: PodcastDao
    private lateinit var episodeDao: EpisodeDao
    private lateinit var playlistDao: PlaylistDao

    private lateinit var manager: PlaylistManager

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val moshi = Moshi.Builder().build()
        val appDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(moshi))
            .setQueryCoroutineContext(testDispatcher)
            .build()
        podcastDao = appDatabase.podcastDao()
        episodeDao = appDatabase.episodeDao()
        playlistDao = appDatabase.playlistDao()
        manager = PlaylistManagerImpl(
            playlistDao = playlistDao,
            clock = clock,
        )
    }

    @Test
    fun observePlaylistPreviews() = runTest(testDispatcher) {
        val playlist1 = SmartPlaylist(uuid = "id-1", title = "Title 1")
        val playlist2 = SmartPlaylist(uuid = "id-2", title = "Title 2")

        manager.observePlaylistsPreview().test {
            assertTrue(awaitItem().isEmpty())

            playlistDao.upsertSmartPlaylists(listOf(playlist1, playlist2))
            assertEquals(
                listOf(
                    PlaylistPreview(uuid = "id-1", title = "Title 1", episodeCount = 0, podcasts = emptyList()),
                    PlaylistPreview(uuid = "id-2", title = "Title 2", episodeCount = 0, podcasts = emptyList()),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun observePodcastsInPreviews() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylist(SmartPlaylist())
        val podcast1 = Podcast(uuid = "podcast-id-1", isSubscribed = true)
        podcastDao.insertSuspend(podcast1)
        val podcast2 = Podcast(uuid = "podcast-id-2", isSubscribed = true)
        podcastDao.insertSuspend(podcast2)
        episodeDao.insert(PodcastEpisode(uuid = "episode-id-1", podcastUuid = "podcast-id-1", imageUrl = "image-url", publishedDate = Date(1)))

        manager.observePlaylistsPreview().test {
            assertEquals(
                listOf(
                    PlaylistPreview(podcasts = listOf(podcast1), episodeCount = 1, uuid = "", title = ""),
                ),
                awaitItem(),
            )

            val episode2 = PodcastEpisode(uuid = "episode-id-2", podcastUuid = "podcast-id-2", publishedDate = Date(0))
            episodeDao.insert(episode2)
            assertEquals(
                listOf(
                    PlaylistPreview(podcasts = listOf(podcast1, podcast2), episodeCount = 2, uuid = "", title = ""),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun doNotObserveManualPlaylistPreviews() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(manual = true))

        val plalylists = manager.observePlaylistsPreview().first()

        assertTrue(plalylists.isEmpty())
    }

    @Test
    fun doNotObserveDeletedPlaylistPreviews() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(deleted = true))

        val plalylists = manager.observePlaylistsPreview().first()

        assertTrue(plalylists.isEmpty())
    }

    @Test
    fun doNotObserveDraftPlaylistPreviews() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(draft = true))

        val plalylists = manager.observePlaylistsPreview().first()

        assertTrue(plalylists.isEmpty())
    }

    @Test
    fun sortPlaylistPreviewsByPosition() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylists(
            listOf(
                SmartPlaylist(uuid = "id-1", sortPosition = 1),
                SmartPlaylist(uuid = "id-2", sortPosition = 0),
                SmartPlaylist(uuid = "id-3", sortPosition = null),
            ),
        )

        val playlistUuids = manager.observePlaylistsPreview().first().map(PlaylistPreview::uuid)

        assertEquals(listOf("id-3", "id-2", "id-1"), playlistUuids)
    }

    @Test
    fun doNotIncludeNotFollowedPodcastsInPreviews() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylist(SmartPlaylist())
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id-1", isSubscribed = false))
        episodeDao.insert(PodcastEpisode(uuid = "episode-id-1", podcastUuid = "podcast-id-1", publishedDate = Date()))
        episodeDao.insert(PodcastEpisode(uuid = "episode-id-2", podcastUuid = "podcast-id-2", publishedDate = Date()))

        val playlist = manager.observePlaylistsPreview().first().single()

        assertEquals(0, playlist.episodeCount)
        assertTrue(playlist.podcasts.isEmpty())
    }

    @Test
    fun applySmartRulesInPreviews() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylist(
            SmartPlaylist(
                unplayed = true,
                partiallyPlayed = false,
                finished = true,
                audioVideo = AUDIO_VIDEO_FILTER_VIDEO_ONLY,
            ),
        )
        val podcasts = listOf(
            Podcast(uuid = "podcast-id-1", isSubscribed = true),
            Podcast(uuid = "podcast-id-2", isSubscribed = true),
            Podcast(uuid = "podcast-id-3", isSubscribed = true),
            Podcast(uuid = "podcast-id-4", isSubscribed = true),
        )
        podcasts.forEach { podcastDao.insertSuspend(it) }
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "episode-id-1",
                    podcastUuid = "podcast-id-1",
                    publishedDate = Date(4),
                    playingStatus = EpisodePlayingStatus.NOT_PLAYED,
                    fileType = "video/mp4",
                ),
                PodcastEpisode(
                    uuid = "episode-id-2",
                    podcastUuid = "podcast-id-2",
                    publishedDate = Date(3),
                    playingStatus = EpisodePlayingStatus.COMPLETED,
                    fileType = "video/mov",
                ),
                PodcastEpisode(
                    uuid = "episode-id-3",
                    podcastUuid = "podcast-id-3",
                    publishedDate = Date(2),
                    playingStatus = EpisodePlayingStatus.NOT_PLAYED,
                    fileType = "audio/mp3",
                ),
                PodcastEpisode(
                    uuid = "episode-id-4",
                    podcastUuid = "podcast-id-4",
                    publishedDate = Date(1),
                    playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                    fileType = "video/mp4",
                ),
            ),
        )

        val playlist = manager.observePlaylistsPreview().first().single()

        assertEquals(2, playlist.episodeCount)
        assertEquals(listOf(podcasts[0], podcasts[1]), playlist.podcasts)
    }

    @Test
    fun sortPodcastsInPlaylistPreviewByNewestToOldest() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(sortType = PlaylistEpisodeSortType.NewestToOldest))
        val podcasts = listOf(
            Podcast(uuid = "podcast-id-1", isSubscribed = true),
            Podcast(uuid = "podcast-id-2", isSubscribed = true),
            Podcast(uuid = "podcast-id-3", isSubscribed = true),
            Podcast(uuid = "podcast-id-4", isSubscribed = true),
        )
        podcasts.forEach { podcastDao.insertSuspend(it) }
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "episode-id-1",
                    podcastUuid = "podcast-id-1",
                    publishedDate = Date(0),
                ),
                PodcastEpisode(
                    uuid = "episode-id-2",
                    podcastUuid = "podcast-id-2",
                    publishedDate = Date(1),
                    addedDate = Date(0),
                ),
                PodcastEpisode(
                    uuid = "episode-id-3",
                    podcastUuid = "podcast-id-3",
                    publishedDate = Date(1),
                    addedDate = Date(1),
                ),
                PodcastEpisode(
                    uuid = "episode-id-4",
                    podcastUuid = "podcast-id-4",
                    publishedDate = Date(2),
                ),
            ),
        )

        val playlist = manager.observePlaylistsPreview().first().single()

        assertEquals(4, playlist.episodeCount)
        assertEquals(podcasts.reversed(), playlist.podcasts)
    }

    @Test
    fun sortPodcastsInPlaylistPreviewByOldestToNewest() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(sortType = PlaylistEpisodeSortType.OldestToNewest))
        val podcasts = listOf(
            Podcast(uuid = "podcast-id-1", isSubscribed = true),
            Podcast(uuid = "podcast-id-2", isSubscribed = true),
            Podcast(uuid = "podcast-id-3", isSubscribed = true),
            Podcast(uuid = "podcast-id-4", isSubscribed = true),
        )
        podcasts.forEach { podcastDao.insertSuspend(it) }
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "episode-id-1",
                    podcastUuid = "podcast-id-1",
                    publishedDate = Date(2),
                ),
                PodcastEpisode(
                    uuid = "episode-id-2",
                    podcastUuid = "podcast-id-2",
                    publishedDate = Date(1),
                    addedDate = Date(1),
                ),
                PodcastEpisode(
                    uuid = "episode-id-3",
                    podcastUuid = "podcast-id-3",
                    publishedDate = Date(1),
                    addedDate = Date(0),
                ),
                PodcastEpisode(
                    uuid = "episode-id-4",
                    podcastUuid = "podcast-id-4",
                    publishedDate = Date(0),
                ),
            ),
        )

        val playlist = manager.observePlaylistsPreview().first().single()

        assertEquals(4, playlist.episodeCount)
        assertEquals(podcasts.reversed(), playlist.podcasts)
    }

    @Test
    fun sortPodcastsInPlaylistPreviewByShortestToLongest() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(sortType = PlaylistEpisodeSortType.ShortestToLongest))
        val podcasts = listOf(
            Podcast(uuid = "podcast-id-1", isSubscribed = true),
            Podcast(uuid = "podcast-id-2", isSubscribed = true),
            Podcast(uuid = "podcast-id-3", isSubscribed = true),
            Podcast(uuid = "podcast-id-4", isSubscribed = true),
        )
        podcasts.forEach { podcastDao.insertSuspend(it) }
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "episode-id-1",
                    podcastUuid = "podcast-id-1",
                    publishedDate = Date(),
                    duration = 2.0,
                ),
                PodcastEpisode(
                    uuid = "episode-id-2",
                    podcastUuid = "podcast-id-2",
                    publishedDate = Date(),
                    duration = 1.0,
                    addedDate = Date(0),
                ),
                PodcastEpisode(
                    uuid = "episode-id-3",
                    podcastUuid = "podcast-id-3",
                    publishedDate = Date(),
                    duration = 1.0,
                    addedDate = Date(1),
                ),
                PodcastEpisode(
                    uuid = "episode-id-4",
                    podcastUuid = "podcast-id-4",
                    publishedDate = Date(),
                    duration = 0.0,
                ),
            ),
        )

        val playlist = manager.observePlaylistsPreview().first().single()

        assertEquals(4, playlist.episodeCount)
        assertEquals(podcasts.reversed(), playlist.podcasts)
    }

    @Test
    fun sortPodcastsInPlaylistPreviewByLongestToShortest() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(sortType = PlaylistEpisodeSortType.LongestToShortest))
        val podcasts = listOf(
            Podcast(uuid = "podcast-id-1", isSubscribed = true),
            Podcast(uuid = "podcast-id-2", isSubscribed = true),
            Podcast(uuid = "podcast-id-3", isSubscribed = true),
            Podcast(uuid = "podcast-id-4", isSubscribed = true),
        )
        podcasts.forEach { podcastDao.insertSuspend(it) }
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "episode-id-1",
                    podcastUuid = "podcast-id-1",
                    publishedDate = Date(),
                    duration = 0.0,
                ),
                PodcastEpisode(
                    uuid = "episode-id-2",
                    podcastUuid = "podcast-id-2",
                    publishedDate = Date(),
                    duration = 1.0,
                    addedDate = Date(0),
                ),
                PodcastEpisode(
                    uuid = "episode-id-3",
                    podcastUuid = "podcast-id-3",
                    publishedDate = Date(),
                    duration = 1.0,
                    addedDate = Date(1),
                ),
                PodcastEpisode(
                    uuid = "episode-id-4",
                    podcastUuid = "podcast-id-4",
                    publishedDate = Date(),
                    duration = 2.0,
                ),
            ),
        )

        val playlist = manager.observePlaylistsPreview().first().single()

        assertEquals(4, playlist.episodeCount)
        assertEquals(podcasts.reversed(), playlist.podcasts)
    }

    @Test
    fun sortPodcastsInPlaylistPreviewByLastDownloadAttempt() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(sortType = PlaylistEpisodeSortType.LastDownloadAttempt))
        val podcasts = listOf(
            Podcast(uuid = "podcast-id-1", isSubscribed = true),
            Podcast(uuid = "podcast-id-2", isSubscribed = true),
            Podcast(uuid = "podcast-id-3", isSubscribed = true),
            Podcast(uuid = "podcast-id-4", isSubscribed = true),
        )
        podcasts.forEach { podcastDao.insertSuspend(it) }
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "episode-id-1",
                    podcastUuid = "podcast-id-1",
                    publishedDate = Date(),
                    lastDownloadAttemptDate = null,
                ),
                PodcastEpisode(
                    uuid = "episode-id-2",
                    podcastUuid = "podcast-id-2",
                    publishedDate = Date(),
                    lastDownloadAttemptDate = Date(0),
                ),
                PodcastEpisode(
                    uuid = "episode-id-4",
                    podcastUuid = "podcast-id-3",
                    publishedDate = Date(1),
                    lastDownloadAttemptDate = Date(1),
                ),
                PodcastEpisode(
                    uuid = "episode-id-5",
                    podcastUuid = "podcast-id-4",
                    publishedDate = Date(),
                    lastDownloadAttemptDate = Date(2),
                ),
            ),
        )

        val playlist = manager.observePlaylistsPreview().first().single()

        assertEquals(4, playlist.episodeCount)
        assertEquals(podcasts.reversed(), playlist.podcasts)
    }
}
