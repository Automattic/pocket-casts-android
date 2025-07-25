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
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.ANYTIME
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.AUDIO_VIDEO_FILTER_ALL
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.AUDIO_VIDEO_FILTER_AUDIO_ONLY
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.AUDIO_VIDEO_FILTER_VIDEO_ONLY
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.LAST_24_HOURS
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.LAST_2_WEEKS
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.LAST_3_DAYS
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.LAST_MONTH
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.LAST_WEEK
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.SYNC_STATUS_NOT_SYNCED
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.SYNC_STATUS_SYNCED
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
import kotlin.time.Duration.Companion.minutes
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
            appDatabase = appDatabase,
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
    fun selectDistinctPodcastInPlaylistPreview() = runTest(testDispatcher) {
        playlistDao.upsertSmartPlaylist(SmartPlaylist())
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
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default,
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    episodeStatus = EpisodeStatusRule(
                        unplayed = true,
                        inProgress = false,
                        completed = false,
                    ),
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    episodeStatus = EpisodeStatusRule(
                        unplayed = false,
                        inProgress = true,
                        completed = false,
                    ),
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    episodeStatus = EpisodeStatusRule(
                        unplayed = false,
                        inProgress = false,
                        completed = true,
                    ),
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    downloadStatus = DownloadStatusRule.Any,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    downloadStatus = DownloadStatusRule.Downloaded,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    downloadStatus = DownloadStatusRule.NotDownloaded,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    mediaType = MediaTypeRule.Any,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    mediaType = MediaTypeRule.Audio,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    mediaType = MediaTypeRule.Video,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    releaseDate = ReleaseDateRule.AnyTime,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    releaseDate = ReleaseDateRule.Last24Hours,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    releaseDate = ReleaseDateRule.Last3Days,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    releaseDate = ReleaseDateRule.LastWeek,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    releaseDate = ReleaseDateRule.Last2Weeks,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    releaseDate = ReleaseDateRule.LastMonth,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    starred = StarredRule.Any,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    starred = StarredRule.Starred,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    podcastsRule = PodcastsRule.Any,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    podcastsRule = PodcastsRule.Selected(uuids = listOf("id-1", "id-2")),
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    episodeDuration = EpisodeDurationRule.Any,
                ),
            ),
            PlaylistDraft(
                title = "Title",
                rules = SmartRules.Default.copy(
                    episodeDuration = EpisodeDurationRule.Constrained(longerThan = 50.minutes, shorterThan = 60.minutes),
                ),
            ),
        )

        drafts.forEach { draft -> manager.upsertPlaylist(draft) }
        val playlists = playlistDao.observeSmartPlaylists().first()

        // Check that UUIDs are unique
        assertEquals(playlists, playlists.distinctBy { it.uuid })

        val defaultPlaylist = SmartPlaylist(
            id = playlists[0].id,
            uuid = playlists[0].uuid,
            title = "Title",
            iconId = 0,
            sortPosition = playlists[0].sortPosition,
            sortType = PlaylistEpisodeSortType.NewestToOldest,
            manual = false,
            draft = false,
            deleted = false,
            syncStatus = SYNC_STATUS_NOT_SYNCED,
            autoDownload = false,
            autoDownloadUnmeteredOnly = false,
            autoDownloadPowerOnly = false,
            autodownloadLimit = 10,
            unplayed = true,
            partiallyPlayed = true,
            finished = true,
            downloaded = true,
            notDownloaded = true,
            downloading = true,
            audioVideo = AUDIO_VIDEO_FILTER_ALL,
            filterHours = ANYTIME,
            starred = false,
            allPodcasts = true,
            podcastUuids = null,
            filterDuration = false,
            longerThan = 20,
            shorterThan = 40,
        )
        fun assertPlaylist(index: Int, message: String, func: (SmartPlaylist) -> SmartPlaylist) {
            val playlist = playlists[index]
            val indexedPlaylist = defaultPlaylist.copy(
                id = playlist.id,
                uuid = playlist.uuid,
                sortPosition = playlist.sortPosition,
            )
            assertEquals(message, func(indexedPlaylist), playlist)
        }

        assertPlaylist(index = 0, "Default") { defaultPlaylist }
        assertPlaylist(index = 1, "Unplayed") { it.copy(unplayed = true, partiallyPlayed = false, finished = false) }
        assertPlaylist(index = 2, "In progress") { it.copy(unplayed = false, partiallyPlayed = true, finished = false) }
        assertPlaylist(index = 3, "Played") { it.copy(unplayed = false, partiallyPlayed = false, finished = true) }
        assertPlaylist(index = 4, "Any downloaded status") { it.copy(downloaded = true, notDownloaded = true, downloading = true) }
        assertPlaylist(index = 5, "Downloaded") { it.copy(downloaded = true, notDownloaded = false, downloading = false) }
        assertPlaylist(index = 6, "Not downloaded") { it.copy(downloaded = false, notDownloaded = true, downloading = true) }
        assertPlaylist(index = 7, "Audio / Video") { it.copy(audioVideo = AUDIO_VIDEO_FILTER_ALL) }
        assertPlaylist(index = 8, "Audio") { it.copy(audioVideo = AUDIO_VIDEO_FILTER_AUDIO_ONLY) }
        assertPlaylist(index = 9, "Video") { it.copy(audioVideo = AUDIO_VIDEO_FILTER_VIDEO_ONLY) }
        assertPlaylist(index = 10, "Released any time") { it.copy(filterHours = ANYTIME) }
        assertPlaylist(index = 11, "Last day") { it.copy(filterHours = LAST_24_HOURS) }
        assertPlaylist(index = 12, "Last 3 days") { it.copy(filterHours = LAST_3_DAYS) }
        assertPlaylist(index = 13, "Last week") { it.copy(filterHours = LAST_WEEK) }
        assertPlaylist(index = 14, "Last 2 weeks") { it.copy(filterHours = LAST_2_WEEKS) }
        assertPlaylist(index = 15, "Last month") { it.copy(filterHours = LAST_MONTH) }
        assertPlaylist(index = 16, "Any starred status") { it.copy(starred = false) }
        assertPlaylist(index = 17, "Starred") { it.copy(starred = true) }
        assertPlaylist(index = 18, "All podcasts") { it.copy(allPodcasts = true, podcastUuids = null) }
        assertPlaylist(index = 19, "Selected podcasts") { it.copy(allPodcasts = false, podcastUuids = "id-1,id-2") }
        assertPlaylist(index = 20, "Any duration") { it.copy(filterDuration = false, longerThan = 20, shorterThan = 40) }
        assertPlaylist(index = 21, "Limited duration") { it.copy(filterDuration = true, longerThan = 50, shorterThan = 60) }
    }

    @Test
    fun createDefaultNewReleasesPlaylist() = runTest(testDispatcher) {
        manager.upsertPlaylist(PlaylistDraft.NewReleases)
        val playlists = playlistDao.observeSmartPlaylists().first()

        assertEquals(
            SmartPlaylist(
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
                autoDownloadUnmeteredOnly = false,
                autoDownloadPowerOnly = false,
                autodownloadLimit = 10,
                unplayed = true,
                partiallyPlayed = true,
                finished = true,
                downloaded = true,
                notDownloaded = true,
                downloading = true,
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
        manager.upsertPlaylist(PlaylistDraft.InProgress)
        val playlists = playlistDao.observeSmartPlaylists().first()

        assertEquals(
            SmartPlaylist(
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
                autoDownloadUnmeteredOnly = false,
                autoDownloadPowerOnly = false,
                autodownloadLimit = 10,
                unplayed = false,
                partiallyPlayed = true,
                finished = false,
                downloaded = true,
                notDownloaded = true,
                downloading = true,
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
}
