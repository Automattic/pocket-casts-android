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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaylistManagerTest {
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
    fun observePlaylistPreviews() = runTest {
        val playlist1 = SmartPlaylist(uuid = "id-1", title = "Title 1")
        val playlist2 = SmartPlaylist(uuid = "id-2", title = "Title 2")

        manager.observePlaylistsPreview().test {
            assertTrue(awaitItem().isEmpty())

            playlistDao.upsertSmartPlaylists(listOf(playlist1, playlist2))
            assertEquals(
                listOf(
                    PlaylistPreview(uuid = "id-1", title = "Title 1", episodeCount = 0, artworkEpisodes = emptyList()),
                    PlaylistPreview(uuid = "id-2", title = "Title 2", episodeCount = 0, artworkEpisodes = emptyList()),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun observeEpisodesInPreviews() = runTest {
        playlistDao.upsertSmartPlaylist(SmartPlaylist())
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id-1", isSubscribed = true))
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id-2", isSubscribed = true))
        val episode1 = PodcastEpisode(uuid = "episode-id-1", podcastUuid = "podcast-id-1", imageUrl = "image-url", publishedDate = Date(1))
        episodeDao.insert(episode1)

        manager.observePlaylistsPreview().test {
            assertEquals(
                listOf(
                    PlaylistPreview(artworkEpisodes = listOf(episode1), episodeCount = 1, uuid = "", title = ""),
                ),
                awaitItem(),
            )

            val episode2 = PodcastEpisode(uuid = "episode-id-2", podcastUuid = "podcast-id-2", publishedDate = Date(0))
            episodeDao.insert(episode2)
            skipItems(1) // Skip sync between episode count and artwork episodes
            assertEquals(
                listOf(
                    PlaylistPreview(artworkEpisodes = listOf(episode1, episode2), episodeCount = 2, uuid = "", title = ""),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun doNotObserveManualPlaylistPreviews() = runTest {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(manual = true))

        val plalylists = manager.observePlaylistsPreview().first()

        assertTrue(plalylists.isEmpty())
    }

    @Test
    fun doNotObserveDeletedPlaylistPreviews() = runTest {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(deleted = true))

        val plalylists = manager.observePlaylistsPreview().first()

        assertTrue(plalylists.isEmpty())
    }

    @Test
    fun doNotObserveDraftPlaylistPreviews() = runTest {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(draft = true))

        val plalylists = manager.observePlaylistsPreview().first()

        assertTrue(plalylists.isEmpty())
    }

    @Test
    fun sortPlaylistPreviewsByPosition() = runTest {
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
    fun doNotIncludeEpisodesFromNotFollowedPodcastsInPreviews() = runTest {
        playlistDao.upsertSmartPlaylist(SmartPlaylist())
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id-1", isSubscribed = false))
        episodeDao.insert(PodcastEpisode(uuid = "episode-id-1", podcastUuid = "podcast-id-1", publishedDate = Date()))
        episodeDao.insert(PodcastEpisode(uuid = "episode-id-2", podcastUuid = "podcast-id-2", publishedDate = Date()))

        val playlist = manager.observePlaylistsPreview().first().single()

        assertEquals(0, playlist.episodeCount)
        assertTrue(playlist.artworkEpisodes.isEmpty())
    }

    @Test
    fun applySmartRulesInPreviews() = runTest {
        playlistDao.upsertSmartPlaylist(
            SmartPlaylist(
                unplayed = true,
                partiallyPlayed = false,
                finished = true,
                audioVideo = AUDIO_VIDEO_FILTER_VIDEO_ONLY,
            ),
        )
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id", isSubscribed = true))
        val episodes = listOf(
            PodcastEpisode(
                uuid = "episode-id-1",
                podcastUuid = "podcast-id",
                publishedDate = Date(4),
                playingStatus = EpisodePlayingStatus.NOT_PLAYED,
                fileType = "video/mp4",
            ),
            PodcastEpisode(
                uuid = "episode-id-2",
                podcastUuid = "podcast-id",
                publishedDate = Date(3),
                playingStatus = EpisodePlayingStatus.COMPLETED,
                fileType = "video/mov",
            ),
            PodcastEpisode(
                uuid = "episode-id-3",
                podcastUuid = "podcast-id",
                publishedDate = Date(2),
                playingStatus = EpisodePlayingStatus.NOT_PLAYED,
                fileType = "audio/mp3",
            ),
            PodcastEpisode(
                uuid = "episode-id-4",
                podcastUuid = "podcast-id",
                publishedDate = Date(1),
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                fileType = "video/mp4",
            ),
        )
        episodeDao.insertAll(episodes)

        val previewEpisodes = manager.observePlaylistsPreview().first().single().artworkEpisodes

        assertEquals(
            listOf(
                episodes[0],
                episodes[1],
            ),
            previewEpisodes,
        )
    }

    @Test
    fun sortEpisodesInPlaylistPreviewByNewestToOldest() = runTest {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(sortType = PlaylistEpisodeSortType.NewestToOldest))
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id", isSubscribed = true))
        val episodes = listOf(
            PodcastEpisode(
                uuid = "episode-id-1",
                podcastUuid = "podcast-id",
                publishedDate = Date(0),
            ),
            PodcastEpisode(
                uuid = "episode-id-2",
                podcastUuid = "podcast-id",
                publishedDate = Date(1),
                addedDate = Date(0),
            ),
            PodcastEpisode(
                uuid = "episode-id-3",
                podcastUuid = "podcast-id",
                publishedDate = Date(1),
                addedDate = Date(1),
            ),
            PodcastEpisode(
                uuid = "episode-id-4",
                podcastUuid = "podcast-id",
                publishedDate = Date(2),
            ),
        )
        episodeDao.insertAll(episodes)

        val previewEpisodes = manager.observePlaylistsPreview().first().single().artworkEpisodes

        assertEquals(episodes.reversed(), previewEpisodes)
    }

    @Test
    fun sortEpisodesInPlaylistPreviewByOldestToNewest() = runTest {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(sortType = PlaylistEpisodeSortType.OldestToNewest))
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id", isSubscribed = true))
        val episodes = listOf(
            PodcastEpisode(
                uuid = "episode-id-1",
                podcastUuid = "podcast-id",
                publishedDate = Date(2),
            ),
            PodcastEpisode(
                uuid = "episode-id-2",
                podcastUuid = "podcast-id",
                publishedDate = Date(1),
                addedDate = Date(1),
            ),
            PodcastEpisode(
                uuid = "episode-id-3",
                podcastUuid = "podcast-id",
                publishedDate = Date(1),
                addedDate = Date(0),
            ),
            PodcastEpisode(
                uuid = "episode-id-4",
                podcastUuid = "podcast-id",
                publishedDate = Date(0),
            ),
        )
        episodeDao.insertAll(episodes)

        val previewEpisodes = manager.observePlaylistsPreview().first().single().artworkEpisodes

        assertEquals(episodes.reversed(), previewEpisodes)
    }

    @Test
    fun sortEpisodesInPlaylistPreviewByShortestToLongest() = runTest {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(sortType = PlaylistEpisodeSortType.ShortestToLongest))
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id", isSubscribed = true))
        val episodes = listOf(
            PodcastEpisode(
                uuid = "episode-id-1",
                podcastUuid = "podcast-id",
                publishedDate = Date(),
                duration = 2.0,
            ),
            PodcastEpisode(
                uuid = "episode-id-2",
                podcastUuid = "podcast-id",
                publishedDate = Date(),
                duration = 1.0,
                addedDate = Date(0),
            ),
            PodcastEpisode(
                uuid = "episode-id-3",
                podcastUuid = "podcast-id",
                publishedDate = Date(),
                duration = 1.0,
                addedDate = Date(1),
            ),
            PodcastEpisode(
                uuid = "episode-id-4",
                podcastUuid = "podcast-id",
                publishedDate = Date(),
                duration = 0.0,
            ),
        )
        episodeDao.insertAll(episodes)

        val previewEpisodes = manager.observePlaylistsPreview().first().single().artworkEpisodes

        assertEquals(episodes.reversed(), previewEpisodes)
    }

    @Test
    fun sortEpisodesInPlaylistPreviewByLongestToShortest() = runTest {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(sortType = PlaylistEpisodeSortType.LongestToShortest))
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id", isSubscribed = true))
        val episodes = listOf(
            PodcastEpisode(
                uuid = "episode-id-1",
                podcastUuid = "podcast-id",
                publishedDate = Date(),
                duration = 0.0,
            ),
            PodcastEpisode(
                uuid = "episode-id-2",
                podcastUuid = "podcast-id",
                publishedDate = Date(),
                duration = 1.0,
                addedDate = Date(0),
            ),
            PodcastEpisode(
                uuid = "episode-id-3",
                podcastUuid = "podcast-id",
                publishedDate = Date(),
                duration = 1.0,
                addedDate = Date(1),
            ),
            PodcastEpisode(
                uuid = "episode-id-4",
                podcastUuid = "podcast-id",
                publishedDate = Date(),
                duration = 2.0,
            ),
        )
        episodeDao.insertAll(episodes)

        val previewEpisodes = manager.observePlaylistsPreview().first().single().artworkEpisodes

        assertEquals(episodes.reversed(), previewEpisodes)
    }

    @Test
    fun sortEpisodesInPlaylistPreviewByLastDownloadAttempt() = runTest {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(sortType = PlaylistEpisodeSortType.LastDownloadAttempt))
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id", isSubscribed = true))
        val episodes = listOf(
            PodcastEpisode(
                uuid = "episode-id-1",
                podcastUuid = "podcast-id",
                publishedDate = Date(),
                lastDownloadAttemptDate = null,
            ),
            PodcastEpisode(
                uuid = "episode-id-2",
                podcastUuid = "podcast-id",
                publishedDate = Date(),
                lastDownloadAttemptDate = Date(0),
            ),
            PodcastEpisode(
                uuid = "episode-id-4",
                podcastUuid = "podcast-id",
                publishedDate = Date(1),
                lastDownloadAttemptDate = Date(1),
            ),
            PodcastEpisode(
                uuid = "episode-id-5",
                podcastUuid = "podcast-id",
                publishedDate = Date(),
                lastDownloadAttemptDate = Date(2),
            ),
        )
        episodeDao.insertAll(episodes)

        val previewEpisodes = manager.observePlaylistsPreview().first().single().artworkEpisodes

        assertEquals(episodes.reversed(), previewEpisodes)
    }

    @Test
    fun limitEpisodeCountForRegularPlaylists() = runTest {
        playlistDao.upsertSmartPlaylist(SmartPlaylist())
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id", isSubscribed = true))
        episodeDao.insertAll(List(501) { PodcastEpisode(uuid = "$it", podcastUuid = "podcast-id", publishedDate = Date()) })

        val playlist = manager.observePlaylistsPreview().first().single()

        assertEquals(500, playlist.episodeCount)
        assertEquals(4, playlist.artworkEpisodes.size)
    }

    @Test
    fun limitEpisodeCountForLastDownloadAttemptSortOrder() = runTest {
        playlistDao.upsertSmartPlaylist(SmartPlaylist(sortType = PlaylistEpisodeSortType.LastDownloadAttempt))
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id", isSubscribed = true))
        episodeDao.insertAll(List(1001) { PodcastEpisode(uuid = "$it", podcastUuid = "podcast-id", publishedDate = Date()) })

        val playlist = manager.observePlaylistsPreview().first().single()

        assertEquals(1000, playlist.episodeCount)
        assertEquals(4, playlist.artworkEpisodes.size)
    }
}
