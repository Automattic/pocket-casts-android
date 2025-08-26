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
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.ANYTIME
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.AUDIO_VIDEO_FILTER_ALL
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.AUDIO_VIDEO_FILTER_AUDIO_ONLY
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.AUDIO_VIDEO_FILTER_VIDEO_ONLY
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_24_HOURS
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_2_WEEKS
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_3_DAYS
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_MONTH
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_WEEK
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.SYNC_STATUS_NOT_SYNCED
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.SYNC_STATUS_SYNCED
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.StarredRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import com.squareup.moshi.Moshi
import java.util.Date
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity as DbPlaylist

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
            appDatabase = appDatabase,
            clock = clock,
        )
    }

    @Test
    fun observePlaylistPreviews() = runTest(testDispatcher) {
        val playlist1 = DbPlaylist(uuid = "id-1", title = "Title 1")
        val playlist2 = DbPlaylist(uuid = "id-2", title = "Title 2")

        manager.observePlaylistsPreview().test {
            assertTrue(awaitItem().isEmpty())

            playlistDao.upsertAllPlaylists(listOf(playlist1, playlist2))
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
        playlistDao.upsertPlaylist(DbPlaylist())
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
        playlistDao.upsertPlaylist(DbPlaylist(manual = true))

        val playlists = manager.observePlaylistsPreview().first()

        assertTrue(playlists.isEmpty())
    }

    @Test
    fun doNotObserveDeletedPlaylistPreviews() = runTest(testDispatcher) {
        playlistDao.upsertPlaylist(DbPlaylist(deleted = true))

        val playlists = manager.observePlaylistsPreview().first()

        assertTrue(playlists.isEmpty())
    }

    @Test
    fun doNotObserveDraftPlaylistPreviews() = runTest(testDispatcher) {
        playlistDao.upsertPlaylist(DbPlaylist(draft = true))

        val playlists = manager.observePlaylistsPreview().first()

        assertTrue(playlists.isEmpty())
    }

    @Test
    fun sortPlaylistPreviewsByPosition() = runTest(testDispatcher) {
        playlistDao.upsertAllPlaylists(
            listOf(
                DbPlaylist(uuid = "id-1", sortPosition = 1),
                DbPlaylist(uuid = "id-2", sortPosition = 0),
                DbPlaylist(uuid = "id-3", sortPosition = null),
            ),
        )

        val playlistUuids = manager.observePlaylistsPreview().first().map(PlaylistPreview::uuid)

        assertEquals(listOf("id-3", "id-2", "id-1"), playlistUuids)
    }

    @Test
    fun doNotIncludeNotFollowedPodcastsInPreviews() = runTest(testDispatcher) {
        playlistDao.upsertPlaylist(DbPlaylist())
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id-1", isSubscribed = false))
        episodeDao.insert(PodcastEpisode(uuid = "episode-id-1", podcastUuid = "podcast-id-1", publishedDate = Date()))
        episodeDao.insert(PodcastEpisode(uuid = "episode-id-2", podcastUuid = "podcast-id-2", publishedDate = Date()))

        val playlist = manager.observePlaylistsPreview().first().single()

        assertEquals(0, playlist.episodeCount)
        assertTrue(playlist.podcasts.isEmpty())
    }

    @Test
    fun applySmartRulesInPreviews() = runTest(testDispatcher) {
        playlistDao.upsertPlaylist(
            DbPlaylist(
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
        playlistDao.upsertPlaylist(DbPlaylist(sortType = PlaylistEpisodeSortType.NewestToOldest))
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
        playlistDao.upsertPlaylist(DbPlaylist(sortType = PlaylistEpisodeSortType.OldestToNewest))
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
        playlistDao.upsertPlaylist(DbPlaylist(sortType = PlaylistEpisodeSortType.ShortestToLongest))
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
        playlistDao.upsertPlaylist(DbPlaylist(sortType = PlaylistEpisodeSortType.LongestToShortest))
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
    fun selectDistinctPodcastInPlaylistPreview() = runTest(testDispatcher) {
        playlistDao.upsertPlaylist(DbPlaylist())
        val podcasts = listOf(
            Podcast(uuid = "podcast-id-1", isSubscribed = true),
            Podcast(uuid = "podcast-id-2", isSubscribed = true),
            Podcast(uuid = "podcast-id-3", isSubscribed = true),
            Podcast(uuid = "podcast-id-4", isSubscribed = true),
            Podcast(uuid = "podcast-id-5", isSubscribed = true),
        )
        podcasts.forEach { podcastDao.insertSuspend(it) }
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "episode-id-1",
                    podcastUuid = "podcast-id-1",
                    publishedDate = Date(6),
                ),
                PodcastEpisode(
                    uuid = "episode-id-2",
                    podcastUuid = "podcast-id-2",
                    publishedDate = Date(5),
                ),
                PodcastEpisode(
                    uuid = "episode-id-3",
                    podcastUuid = "podcast-id-2",
                    publishedDate = Date(4),
                ),
                PodcastEpisode(
                    uuid = "episode-id-4",
                    podcastUuid = "podcast-id-3",
                    publishedDate = Date(3),
                ),
                PodcastEpisode(
                    uuid = "episode-id-5",
                    podcastUuid = "podcast-id-4",
                    publishedDate = Date(2),
                ),
                PodcastEpisode(
                    uuid = "episode-id-6",
                    podcastUuid = "podcast-id-5",
                    publishedDate = Date(1),
                ),
            ),
        )

        val playlist = manager.observePlaylistsPreview().first().single()

        assertEquals(6, playlist.episodeCount)
        assertEquals(podcasts.take(4), playlist.podcasts)
    }

    @Test
    fun createPlaylists() = runTest(testDispatcher) {
        val drafts = listOf(
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default,
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    episodeStatus = EpisodeStatusRule(
                        unplayed = true,
                        inProgress = false,
                        completed = false,
                    ),
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    episodeStatus = EpisodeStatusRule(
                        unplayed = false,
                        inProgress = true,
                        completed = false,
                    ),
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    episodeStatus = EpisodeStatusRule(
                        unplayed = false,
                        inProgress = false,
                        completed = true,
                    ),
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    downloadStatus = DownloadStatusRule.Any,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    downloadStatus = DownloadStatusRule.Downloaded,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    downloadStatus = DownloadStatusRule.NotDownloaded,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    mediaType = MediaTypeRule.Any,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    mediaType = MediaTypeRule.Audio,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    mediaType = MediaTypeRule.Video,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    releaseDate = ReleaseDateRule.AnyTime,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    releaseDate = ReleaseDateRule.Last24Hours,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    releaseDate = ReleaseDateRule.Last3Days,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    releaseDate = ReleaseDateRule.LastWeek,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    releaseDate = ReleaseDateRule.Last2Weeks,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    releaseDate = ReleaseDateRule.LastMonth,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    starred = StarredRule.Any,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    starred = StarredRule.Starred,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    podcasts = PodcastsRule.Any,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    podcasts = PodcastsRule.Selected(uuids = listOf("id-1", "id-2")),
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    episodeDuration = EpisodeDurationRule.Any,
                ),
            ),
            SmartPlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    episodeDuration = EpisodeDurationRule.Constrained(longerThan = 50.minutes, shorterThan = 60.minutes),
                ),
            ),
        )
        drafts.forEach { draft -> manager.insertSmartPlaylist(draft) }
        val playlists = playlistDao.observeSmartPlaylists().first()

        // Check that UUIDs are unique
        assertEquals(playlists, playlists.distinctBy { it.uuid })

        val defaultPlaylist = DbPlaylist(
            id = playlists.last().id,
            uuid = playlists.last().uuid,
            title = "Title",
            iconId = 0,
            sortPosition = playlists.last().sortPosition,
            sortType = PlaylistEpisodeSortType.NewestToOldest,
            manual = false,
            draft = false,
            deleted = false,
            syncStatus = SYNC_STATUS_NOT_SYNCED,
            autoDownload = false,
            autodownloadLimit = 10,
            unplayed = true,
            partiallyPlayed = true,
            finished = true,
            downloaded = true,
            notDownloaded = true,
            audioVideo = AUDIO_VIDEO_FILTER_ALL,
            filterHours = ANYTIME,
            starred = false,
            allPodcasts = true,
            podcastUuids = null,
            filterDuration = false,
            longerThan = 20,
            shorterThan = 40,
        )

        fun assertPlaylist(index: Int, message: String, func: (DbPlaylist) -> DbPlaylist) {
            val playlist = playlists[index]
            val indexedPlaylist = defaultPlaylist.copy(
                id = playlist.id,
                uuid = playlist.uuid,
                sortPosition = playlist.sortPosition,
            )
            assertEquals(message, func(indexedPlaylist), playlist)
        }

        assertPlaylist(index = 21, "Default") { defaultPlaylist }
        assertPlaylist(index = 20, "Unplayed") { it.copy(unplayed = true, partiallyPlayed = false, finished = false) }
        assertPlaylist(index = 19, "In progress") { it.copy(unplayed = false, partiallyPlayed = true, finished = false) }
        assertPlaylist(index = 18, "Played") { it.copy(unplayed = false, partiallyPlayed = false, finished = true) }
        assertPlaylist(index = 17, "Any downloaded status") { it.copy(downloaded = true, notDownloaded = true) }
        assertPlaylist(index = 16, "Downloaded") { it.copy(downloaded = true, notDownloaded = false) }
        assertPlaylist(index = 15, "Not downloaded") { it.copy(downloaded = false, notDownloaded = true) }
        assertPlaylist(index = 14, "Audio / Video") { it.copy(audioVideo = AUDIO_VIDEO_FILTER_ALL) }
        assertPlaylist(index = 13, "Audio") { it.copy(audioVideo = AUDIO_VIDEO_FILTER_AUDIO_ONLY) }
        assertPlaylist(index = 12, "Video") { it.copy(audioVideo = AUDIO_VIDEO_FILTER_VIDEO_ONLY) }
        assertPlaylist(index = 11, "Released any time") { it.copy(filterHours = ANYTIME) }
        assertPlaylist(index = 10, "Last day") { it.copy(filterHours = LAST_24_HOURS) }
        assertPlaylist(index = 9, "Last 3 days") { it.copy(filterHours = LAST_3_DAYS) }
        assertPlaylist(index = 8, "Last week") { it.copy(filterHours = LAST_WEEK) }
        assertPlaylist(index = 7, "Last 2 weeks") { it.copy(filterHours = LAST_2_WEEKS) }
        assertPlaylist(index = 6, "Last month") { it.copy(filterHours = LAST_MONTH) }
        assertPlaylist(index = 5, "Any starred status") { it.copy(starred = false) }
        assertPlaylist(index = 4, "Starred") { it.copy(starred = true) }
        assertPlaylist(index = 3, "All podcasts") { it.copy(allPodcasts = true, podcastUuids = null) }
        assertPlaylist(index = 2, "Selected podcasts") { it.copy(allPodcasts = false, podcastUuids = "id-1,id-2") }
        assertPlaylist(index = 1, "Any duration") { it.copy(filterDuration = false, longerThan = 20, shorterThan = 40) }
        assertPlaylist(index = 0, "Limited duration") { it.copy(filterDuration = true, longerThan = 50, shorterThan = 60) }
    }

    @Test
    fun createDefaultNewReleasesPlaylist() = runTest(testDispatcher) {
        manager.insertSmartPlaylist(SmartPlaylistDraft.NewReleases)
        val playlists = playlistDao.observeSmartPlaylists().first()

        assertEquals(
            DbPlaylist(
                id = 1,
                uuid = Playlist.NEW_RELEASES_UUID,
                title = "New Releases",
                iconId = 10,
                sortPosition = 1,
                sortType = PlaylistEpisodeSortType.NewestToOldest,
                manual = false,
                draft = false,
                deleted = false,
                syncStatus = SYNC_STATUS_SYNCED,
                autoDownload = false,
                autodownloadLimit = 10,
                unplayed = true,
                partiallyPlayed = true,
                finished = true,
                downloaded = true,
                notDownloaded = true,
                audioVideo = AUDIO_VIDEO_FILTER_ALL,
                filterHours = LAST_2_WEEKS,
                starred = false,
                allPodcasts = true,
                podcastUuids = null,
                filterDuration = false,
                longerThan = 20,
                shorterThan = 40,
            ),
            playlists[0],
        )
    }

    @Test
    fun createDefaultInProgressPlaylist() = runTest(testDispatcher) {
        manager.insertSmartPlaylist(SmartPlaylistDraft.InProgress)
        val playlists = playlistDao.observeSmartPlaylists().first()

        assertEquals(
            DbPlaylist(
                id = 1,
                uuid = Playlist.IN_PROGRESS_UUID,
                title = "In Progress",
                iconId = 23,
                sortPosition = 1,
                sortType = PlaylistEpisodeSortType.NewestToOldest,
                manual = false,
                draft = false,
                deleted = false,
                syncStatus = SYNC_STATUS_SYNCED,
                autoDownload = false,
                autodownloadLimit = 10,
                unplayed = false,
                partiallyPlayed = true,
                finished = false,
                downloaded = true,
                notDownloaded = true,
                audioVideo = AUDIO_VIDEO_FILTER_ALL,
                filterHours = LAST_MONTH,
                starred = false,
                allPodcasts = true,
                podcastUuids = null,
                filterDuration = false,
                longerThan = 20,
                shorterThan = 40,
            ),
            playlists[0],
        )
    }

    @Test
    fun orderPlaylists() = runTest(testDispatcher) {
        val playlists = List(100) { index ->
            DbPlaylist(uuid = UUID.randomUUID().toString(), sortPosition = index)
        }
        playlistDao.upsertAllPlaylists(playlists)

        val reorderedPlaylistsUuids = playlists.shuffled().map(DbPlaylist::uuid)
        manager.updatePlaylistsOrder(reorderedPlaylistsUuids)

        val reorderedPlaylists = playlistDao.getSmartPlaylists()
        assertEquals(reorderedPlaylistsUuids, reorderedPlaylists.map(DbPlaylist::uuid))
        assertTrue(reorderedPlaylists.all { it.syncStatus == SYNC_STATUS_NOT_SYNCED })
    }

    @Test
    fun moveUnspecifiedPlaylistsToTheBottom() = runTest(testDispatcher) {
        val playlists = listOf(
            DbPlaylist(uuid = UUID.randomUUID().toString(), sortPosition = 0),
            DbPlaylist(uuid = UUID.randomUUID().toString(), sortPosition = 1),
            DbPlaylist(uuid = UUID.randomUUID().toString(), sortPosition = 2),
            DbPlaylist(uuid = UUID.randomUUID().toString(), sortPosition = 3),
            DbPlaylist(uuid = UUID.randomUUID().toString(), sortPosition = 4),
        )
        playlistDao.upsertAllPlaylists(playlists)

        val reorderedPlaylistsUuids = listOf(
            playlists[4].uuid,
            playlists[1].uuid,
            playlists[3].uuid,
        )
        manager.updatePlaylistsOrder(reorderedPlaylistsUuids)

        val reorderedPlaylists = playlistDao.getSmartPlaylists()
        assertEquals(
            listOf(
                playlists[4].uuid,
                playlists[1].uuid,
                playlists[3].uuid,
                playlists[0].uuid,
                playlists[2].uuid,
            ),
            reorderedPlaylists.map(DbPlaylist::uuid),
        )
    }

    @Test
    fun observeSmartEpisodes() = runTest(testDispatcher) {
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id", isSubscribed = true))

        val smartRules = SmartRules.Default.copy(
            episodeStatus = EpisodeStatusRule(
                unplayed = true,
                inProgress = true,
                completed = false,
            ),
            mediaType = MediaTypeRule.Audio,
            episodeDuration = EpisodeDurationRule.Constrained(longerThan = 10.minutes, shorterThan = 35.minutes),
        )

        manager.observeSmartEpisodes(smartRules).test {
            assertEquals(emptyList<PodcastEpisode>(), awaitItem())

            val episode1 = PodcastEpisode(
                uuid = "id-1",
                podcastUuid = "podcast-id",
                publishedDate = Date(100),
                playingStatus = EpisodePlayingStatus.NOT_PLAYED,
                fileType = "audio/mp3",
                duration = 15.minutes.inWholeSeconds.toDouble(),
            )
            episodeDao.insert(episode1)
            assertEquals(listOf(episode1), awaitItem())

            val episode2 = PodcastEpisode(
                uuid = "id-2",
                podcastUuid = "podcast-id",
                publishedDate = Date(99),
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                fileType = "audio/mp3",
                duration = 30.minutes.inWholeSeconds.toDouble(),
            )
            episodeDao.insert(episode2)
            assertEquals(listOf(episode1, episode2), awaitItem())

            episodeDao.insert(
                PodcastEpisode(
                    uuid = "id-3",
                    podcastUuid = "podcast-id",
                    publishedDate = Date(98),
                    playingStatus = EpisodePlayingStatus.COMPLETED,
                    fileType = "audio/mp3",
                    duration = 30.minutes.inWholeSeconds.toDouble(),
                ),
            )
            expectNoEvents()

            episodeDao.update(episode1.copy(fileType = "video/mov"))
            assertEquals(listOf(episode2), awaitItem())

            episodeDao.update(episode2.copy(duration = 0.0))
            assertEquals(emptyList<PodcastEpisode>(), awaitItem())
        }
    }

    @Test
    fun observeSmartPlaylist() = runTest(testDispatcher) {
        val podcasts = listOf(
            Podcast(uuid = "podcast-id-1", isSubscribed = true),
            Podcast(uuid = "podcast-id-2", isSubscribed = true),
        )
        podcasts.forEach { podcast -> podcastDao.insertSuspend(podcast) }

        manager.observeSmartPlaylist("playlist-id").test {
            assertNull(awaitItem())

            playlistDao.upsertPlaylist(DbPlaylist(uuid = "playlist-id", title = "Title 1"))
            assertEquals(
                SmartPlaylist(
                    uuid = "playlist-id",
                    title = "Title 1",
                    smartRules = SmartRules.Default,
                    episodes = emptyList(),
                    episodeSortType = PlaylistEpisodeSortType.NewestToOldest,
                    isAutoDownloadEnabled = false,
                    autoDownloadLimit = 10,
                    totalEpisodeCount = 0,
                    playbackDurationLeft = Duration.ZERO,
                    artworkPodcasts = emptyList(),
                ),
                awaitItem(),
            )

            val episodes = listOf(
                PodcastEpisode(uuid = "id-1", podcastUuid = "podcast-id-1", publishedDate = Date(2)),
                PodcastEpisode(uuid = "id-2", podcastUuid = "podcast-id-1", publishedDate = Date(1)),
                PodcastEpisode(uuid = "id-3", podcastUuid = "podcast-id-2", publishedDate = Date(0)),
            )
            episodeDao.insertAll(episodes)
            var playlist = awaitItem()
            assertEquals(episodes, playlist?.episodes)
            assertEquals(podcasts, playlist?.artworkPodcasts)

            playlistDao.observeSmartPlaylist("playlist-id").first()!!.let {
                playlistDao.upsertPlaylist(it.copy(allPodcasts = false, podcastUuids = "podcast-id-2"))
            }
            playlist = awaitItem()
            assertEquals(PodcastsRule.Selected(listOf("podcast-id-2")), playlist?.smartRules?.podcasts)
            assertEquals(listOf(episodes[2]), playlist?.episodes)
            assertEquals(listOf(podcasts[1]), playlist?.artworkPodcasts)
        }
    }

    @Test
    fun limitArtworkPodcastsSize() = runTest(testDispatcher) {
        val podcasts = listOf(
            Podcast(uuid = "podcast-id-1", isSubscribed = true),
            Podcast(uuid = "podcast-id-2", isSubscribed = true),
            Podcast(uuid = "podcast-id-3", isSubscribed = true),
            Podcast(uuid = "podcast-id-4", isSubscribed = true),
            Podcast(uuid = "podcast-id-5", isSubscribed = true),
        )
        podcasts.forEach { podcast -> podcastDao.insertSuspend(podcast) }
        val episodes = listOf(
            PodcastEpisode(uuid = "id-1", podcastUuid = "podcast-id-1", publishedDate = Date(5)),
            PodcastEpisode(uuid = "id-2", podcastUuid = "podcast-id-2", publishedDate = Date(4)),
            PodcastEpisode(uuid = "id-3", podcastUuid = "podcast-id-3", publishedDate = Date(3)),
            PodcastEpisode(uuid = "id-4", podcastUuid = "podcast-id-4", publishedDate = Date(2)),
            PodcastEpisode(uuid = "id-5", podcastUuid = "podcast-id-5", publishedDate = Date(1)),
        )
        episodeDao.insertAll(episodes)
        playlistDao.upsertPlaylist(DbPlaylist(uuid = "playlist-id", title = "Title 1"))

        manager.observeSmartPlaylist("playlist-id").test {
            val playlist = awaitItem()
            assertEquals(episodes, playlist?.episodes)
            assertEquals(podcasts.take(4), playlist?.artworkPodcasts)
        }
    }

    @Test
    fun playlistTotalEpisodeCountExceedsEpisodeLimit() = runTest(testDispatcher) {
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id", isSubscribed = true))
        episodeDao.insertAll(
            List(2000) { index ->
                PodcastEpisode(uuid = "id-$index", podcastUuid = "podcast-id", publishedDate = Date(10000 - index.toLong()))
            },
        )
        playlistDao.upsertPlaylist(DbPlaylist(uuid = "playlist-id", title = "Title 1"))

        manager.observeSmartPlaylist("playlist-id").test {
            val playlist = awaitItem()
            assertEquals(2000, playlist?.totalEpisodeCount)
            assertEquals(1000, playlist?.episodes?.size)
        }
    }

    @Test
    fun playlistTotalPlaybackDurationLeft() = runTest(testDispatcher) {
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id", isSubscribed = true))
        playlistDao.upsertPlaylist(DbPlaylist(uuid = "playlist-id", title = "Title 1"))

        val baseEpisode = PodcastEpisode(uuid = "", podcastUuid = "podcast-id", publishedDate = Date())
        manager.observeSmartPlaylist("playlist-id").test {
            var playlist = awaitItem()
            assertEquals(Duration.ZERO, playlist?.playbackDurationLeft)

            episodeDao.insert(baseEpisode.copy(uuid = "id-1"))
            playlist = awaitItem()
            assertEquals(Duration.ZERO, playlist?.playbackDurationLeft)

            episodeDao.insert(baseEpisode.copy(uuid = "id-2", duration = 20.0))
            playlist = awaitItem()
            assertEquals(20.seconds, playlist?.playbackDurationLeft)

            episodeDao.insert(baseEpisode.copy(uuid = "id-3", duration = 15.0))
            playlist = awaitItem()
            assertEquals(35.seconds, playlist?.playbackDurationLeft)

            episodeDao.insert(baseEpisode.copy(uuid = "id-4", duration = 15.0, playedUpTo = 10.0))
            playlist = awaitItem()
            assertEquals(40.seconds, playlist?.playbackDurationLeft)

            // Check when the duration is unknown and playedUpTo can get above it
            episodeDao.insert(baseEpisode.copy(uuid = "id-5", duration = 0.0, playedUpTo = 10.0))
            playlist = awaitItem()
            assertEquals(40.seconds, playlist?.playbackDurationLeft)
        }
    }

    @Test
    fun updateSortType() = runTest(testDispatcher) {
        playlistDao.upsertPlaylist(DbPlaylist(uuid = "playlist-id", title = "Title 1"))

        manager.observeSmartPlaylist("playlist-id").test {
            var playlist = awaitItem()
            assertEquals(PlaylistEpisodeSortType.NewestToOldest, playlist?.episodeSortType)

            manager.updateSortType("playlist-id", PlaylistEpisodeSortType.ShortestToLongest)
            playlist = awaitItem()
            assertEquals(PlaylistEpisodeSortType.ShortestToLongest, playlist?.episodeSortType)
        }
    }

    @Test
    fun updateAutoDownload() = runTest(testDispatcher) {
        playlistDao.upsertPlaylist(DbPlaylist(uuid = "playlist-id", title = "Title 1"))

        manager.observeSmartPlaylist("playlist-id").test {
            var playlist = awaitItem()
            assertEquals(false, playlist?.isAutoDownloadEnabled)

            manager.updateAutoDownload("playlist-id", true)
            playlist = awaitItem()
            assertEquals(true, playlist?.isAutoDownloadEnabled)
        }
    }

    @Test
    fun updateAutoDownloadLimit() = runTest(testDispatcher) {
        playlistDao.upsertPlaylist(DbPlaylist(uuid = "playlist-id", title = "Title 1"))

        manager.observeSmartPlaylist("playlist-id").test {
            var playlist = awaitItem()
            assertEquals(10, playlist?.autoDownloadLimit)

            manager.updateAutoDownloadLimit("playlist-id", 85)
            playlist = awaitItem()
            assertEquals(85, playlist?.autoDownloadLimit)
        }
    }

    @Test
    fun updateName() = runTest(testDispatcher) {
        playlistDao.upsertPlaylist(DbPlaylist(uuid = "playlist-id", title = "Title 1"))

        manager.observeSmartPlaylist("playlist-id").test {
            var playlist = awaitItem()
            assertEquals("Title 1", playlist?.title)

            manager.updateName("playlist-id", "Other title")
            playlist = awaitItem()
            assertEquals("Other title", playlist?.title)
        }
    }

    @Test
    fun searchSmartEpisodes() = runTest(testDispatcher) {
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id-1", title = "Podcast Title 1", isSubscribed = true))
        podcastDao.insertSuspend(Podcast(uuid = "podcast-id-2", title = "Podcast Title 2", isSubscribed = true))

        val episodes = List(2000) { index ->
            PodcastEpisode(
                uuid = "id-$index",
                title = "Episode Title $index",
                podcastUuid = if (index % 2 == 0) "podcast-id-1" else "podcast-id-2",
                publishedDate = Date(1000000 - index.toLong()),
            )
        }
        episodeDao.insertAll(episodes)
        val percentEpisode = PodcastEpisode(uuid = "id-1000000", title = "Episode % title", podcastUuid = "podcast-id-1", publishedDate = Date(0))
        episodeDao.insert(percentEpisode)
        val underscoreEpisode = PodcastEpisode(uuid = "id-2000000", title = "Episode _ title", podcastUuid = "podcast-id-1", publishedDate = Date(0))
        episodeDao.insert(underscoreEpisode)
        val backslashEpisode = PodcastEpisode(uuid = "id-3000000", title = "Episode \\ title", podcastUuid = "podcast-id-1", publishedDate = Date(0))
        episodeDao.insert(backslashEpisode)

        suspend fun getSmartEpisodes(searchTerm: String?): List<PodcastEpisode> {
            return manager.observeSmartEpisodes(SmartRules.Default, searchTerm = searchTerm).first()
        }

        assertEquals(
            "null search term",
            episodes.take(1000),
            getSmartEpisodes(searchTerm = null),
        )

        assertEquals(
            "blank search term",
            episodes.take(1000),
            getSmartEpisodes(searchTerm = " "),
        )

        assertEquals(
            "podcast title search",
            episodes.filterIndexed { index, _ -> index % 2 == 0 },
            getSmartEpisodes(searchTerm = "podcast title 1"),
        )

        assertEquals(
            "episode title search",
            listOf(
                episodes[77],
                episodes[770],
                episodes[771],
                episodes[772],
                episodes[773],
                episodes[774],
                episodes[775],
                episodes[776],
                episodes[777],
                episodes[778],
                episodes[779],
            ),
            getSmartEpisodes(searchTerm = "title 77"),
        )

        assertEquals(
            "search above episode limit",
            listOf(episodes[1515]),
            getSmartEpisodes(searchTerm = "episode title 1515"),
        )

        assertEquals(
            "percent character",
            listOf(percentEpisode),
            getSmartEpisodes(searchTerm = "%"),
        )

        assertEquals(
            "underscore character",
            listOf(underscoreEpisode),
            getSmartEpisodes(searchTerm = "_"),
        )

        assertEquals(
            "backslash character",
            listOf(backslashEpisode),
            getSmartEpisodes(searchTerm = "\\"),
        )
    }
}
