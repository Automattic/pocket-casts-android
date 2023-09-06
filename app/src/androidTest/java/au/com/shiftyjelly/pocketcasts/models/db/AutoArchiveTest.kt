package au.com.shiftyjelly.pocketcasts.models.db

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveAfterPlayingSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveInactiveSetting
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueueImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.Calendar
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AutoArchiveTest {
    lateinit var testDb: AppDatabase
    lateinit var episodeDao: EpisodeDao
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val fileStorage = mock<FileStorage> {}
    val downloadManager = mock<DownloadManager> {}
    val podcastCacheServerManager = mock<PodcastCacheServerManager> {}
    val userEpisodeManager = mock<UserEpisodeManager> {}
    val episodeAnalytics = EpisodeAnalytics(mock())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        episodeDao = testDb.episodeDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    private fun episodeManagerFor(
        db: AppDatabase,
        played: AutoArchiveAfterPlayingSetting,
        inactive: AutoArchiveInactiveSetting,
        includeStarred: Boolean = false,
        testDispatcher: CoroutineDispatcher,
    ): EpisodeManager {
        val settings = mock<Settings> {
            on { autoArchiveInactive } doReturn UserSetting.Mock(inactive, mock())
            on { autoArchiveAfterPlaying } doReturn UserSetting.Mock(played, mock())
            on { autoArchiveIncludeStarred } doReturn UserSetting.Mock(includeStarred, mock())
        }
        return EpisodeManagerImpl(
            settings = settings,
            fileStorage = fileStorage,
            downloadManager = downloadManager,
            context = context,
            appDatabase = db,
            podcastCacheServerManager = podcastCacheServerManager,
            userEpisodeManager = userEpisodeManager,
            ioDispatcher = testDispatcher,
            episodeAnalytics = episodeAnalytics,
        )
    }

    private fun podcastManagerThatReturns(podcast: Podcast): PodcastManager {
        return mock {
            on { findPodcastByUuid(any()) } doReturn podcast
            on { findSubscribed() } doReturn listOf(podcast)
        }
    }

    private fun upNextQueueFor(db: AppDatabase, episodeManager: EpisodeManager): UpNextQueue {
        val settings = mock<Settings>() {
            on { autoDownloadUpNext } doReturn UserSetting.Mock(false, mock())
        }
        val context = mock<Context>()
        val syncManager = mock<SyncManager>()
        return UpNextQueueImpl(db, settings, episodeManager, syncManager, context)
    }

    @Test
    fun testNever() = runTest {
        val uuid = UUID.randomUUID().toString()
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Never, testDispatcher = testDispatcher)
        val podcast = Podcast(UUID.randomUUID().toString())
        val podcastManager = podcastManagerThatReturns(podcast)
        val episode = PodcastEpisode(uuid = uuid, podcastUuid = podcast.uuid, isArchived = false, publishedDate = Date())
        episodeDao.insert(episode)
        assertTrue("Episode should not be archived before running", !episode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedEpisode = episodeDao.findByUuid(uuid)!!
        assertTrue("Episode should not be archived after running", !updatedEpisode.isArchived)
    }

    @Test
    fun testInactive30Days() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Days30, testDispatcher = testDispatcher)
        val podcastUUID = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = podcastUUID, isSubscribed = true)
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -31)
        val date = calendar.time
        val uuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = uuid, isArchived = false, publishedDate = date, podcastUuid = podcastUUID, addedDate = date)
        val newUUID = UUID.randomUUID().toString()
        val newEpisode = PodcastEpisode(uuid = newUUID, podcastUuid = podcastUUID, addedDate = Date(), publishedDate = Date(), isArchived = false)
        testDb.podcastDao().insert(podcast)
        episodeDao.insert(episode)
        episodeDao.insert(newEpisode)
        assertTrue("Episode should not be archived before running", !episode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedEpisode = episodeDao.findByUuid(uuid)!!
        assertTrue("Episode should be archived as it is older than 30 days", updatedEpisode.isArchived)

        val updatedNewEpisode = episodeDao.findByUuid(newUUID)!!
        assertTrue("Episode should not be archived as it is new", !updatedNewEpisode.isArchived)
    }

    @Test
    fun testPlayedRecently() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Days30, testDispatcher = testDispatcher)
        val podcastUUID = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = podcastUUID, isSubscribed = true)
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -31)
        val date = calendar.time
        val uuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = uuid, isArchived = false, publishedDate = date, podcastUuid = podcastUUID, addedDate = date, lastPlaybackInteraction = Date().time)

        testDb.podcastDao().insert(podcast)
        episodeDao.insert(episode)
        assertTrue("Episode should not be archived before running", !episode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedEpisode = episodeDao.findByUuid(uuid)!!
        assertTrue("Episode should be not be archived as it has been played recently", !updatedEpisode.isArchived)
    }

    @Test
    fun testDownloadedRecently() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Days30, testDispatcher = testDispatcher)
        val podcastUUID = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = podcastUUID, isSubscribed = true)
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -31)
        val date = calendar.time
        val uuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = uuid, isArchived = false, publishedDate = date, podcastUuid = podcastUUID, addedDate = date, lastDownloadAttemptDate = Date())

        testDb.podcastDao().insert(podcast)
        episodeDao.insert(episode)
        assertTrue("Episode should not be archived before running", !episode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedEpisode = episodeDao.findByUuid(uuid)!!
        assertTrue("Episode should be not be archived as it has been downloaded recently", !updatedEpisode.isArchived)
    }

    @Test
    fun testPlayed24Hours() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Hours24, AutoArchiveInactiveSetting.Never, testDispatcher = testDispatcher)
        val podcast = Podcast(UUID.randomUUID().toString())
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -2)
        val date = calendar.time
        val playedUuid = UUID.randomUUID().toString()
        val unplayedUuid = UUID.randomUUID().toString()
        val playedEpisode = PodcastEpisode(uuid = playedUuid, podcastUuid = podcast.uuid, isArchived = false, publishedDate = date, playingStatus = EpisodePlayingStatus.COMPLETED, lastPlaybackInteraction = date.time)
        val unplayedEpisode = PodcastEpisode(uuid = unplayedUuid, podcastUuid = podcast.uuid, isArchived = false, publishedDate = Date(), playingStatus = EpisodePlayingStatus.NOT_PLAYED)

        episodeDao.insert(playedEpisode)
        episodeDao.insert(unplayedEpisode)

        assertTrue("Episode should not be archived before running", !playedEpisode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedPlayedEpisode = episodeDao.findByUuid(playedUuid)!!
        assertTrue("Episode should be archived as it was played 2 days ago", updatedPlayedEpisode.isArchived)
        val updatedUnplayedEpisode = episodeDao.findByUuid(unplayedUuid)!!
        assertTrue("Episode should be not be archived as it hasn't been played", !updatedUnplayedEpisode.isArchived)
    }

    @Test
    fun testPlayedNotIncludeStarred() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Hours24, AutoArchiveInactiveSetting.Never, testDispatcher = testDispatcher)
        val podcast = Podcast(UUID.randomUUID().toString())
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -2)
        val date = calendar.time
        val playedUuid = UUID.randomUUID().toString()
        val unplayedUuid = UUID.randomUUID().toString()
        val playedEpisode = PodcastEpisode(uuid = playedUuid, podcastUuid = podcast.uuid, isArchived = false, publishedDate = date, playingStatus = EpisodePlayingStatus.COMPLETED, lastPlaybackInteraction = date.time, isStarred = true)
        val unplayedEpisode = PodcastEpisode(uuid = unplayedUuid, podcastUuid = podcast.uuid, isArchived = false, publishedDate = Date(), playingStatus = EpisodePlayingStatus.NOT_PLAYED)

        episodeDao.insert(playedEpisode)
        episodeDao.insert(unplayedEpisode)

        assertTrue("Episode should not be archived before running", !playedEpisode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedPlayedEpisode = episodeDao.findByUuid(playedUuid)!!
        assertTrue("Episode should not be archived as it is starred", !updatedPlayedEpisode.isArchived)
        val updatedUnplayedEpisode = episodeDao.findByUuid(unplayedUuid)!!
        assertTrue("Episode should be not be archived as it hasn't been played", !updatedUnplayedEpisode.isArchived)
    }

    @Test
    fun testPlayedIncludeStarred() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Hours24, AutoArchiveInactiveSetting.Never, includeStarred = true, testDispatcher = testDispatcher)
        val podcast = Podcast(UUID.randomUUID().toString())
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -2)
        val date = calendar.time
        val playedUuid = UUID.randomUUID().toString()
        val unplayedUuid = UUID.randomUUID().toString()
        val playedEpisode = PodcastEpisode(uuid = playedUuid, podcastUuid = podcast.uuid, isArchived = false, publishedDate = date, playingStatus = EpisodePlayingStatus.COMPLETED, lastPlaybackInteraction = date.time, isStarred = true)
        val unplayedEpisode = PodcastEpisode(uuid = unplayedUuid, podcastUuid = podcast.uuid, isArchived = false, publishedDate = Date(), playingStatus = EpisodePlayingStatus.NOT_PLAYED)

        episodeDao.insert(playedEpisode)
        episodeDao.insert(unplayedEpisode)

        assertTrue("Episode should not be archived before running", !playedEpisode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedPlayedEpisode = episodeDao.findByUuid(playedUuid)!!
        assertTrue("Episode should be archived as it is starred and include starred is on", updatedPlayedEpisode.isArchived)
        val updatedUnplayedEpisode = episodeDao.findByUuid(unplayedUuid)!!
        assertTrue("Episode should be not be archived as it hasn't been played", !updatedUnplayedEpisode.isArchived)
    }

    @Test
    fun inactiveNotIncludeStarred() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Days30, testDispatcher = testDispatcher)
        val podcastUUID = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = podcastUUID, isSubscribed = true)
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -31)
        val date = calendar.time
        val uuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = uuid, isArchived = false, publishedDate = date, podcastUuid = podcastUUID, addedDate = date, isStarred = true)
        val newUUID = UUID.randomUUID().toString()
        val newEpisode = PodcastEpisode(uuid = newUUID, podcastUuid = podcastUUID, addedDate = Date(), publishedDate = Date(), isArchived = false)
        testDb.podcastDao().insert(podcast)
        episodeDao.insert(episode)
        episodeDao.insert(newEpisode)
        assertTrue("Episode should not be archived before running", !episode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedEpisode = episodeDao.findByUuid(uuid)!!
        assertTrue("Episode should not be archived as it is starred", !updatedEpisode.isArchived)

        val updatedNewEpisode = episodeDao.findByUuid(newUUID)!!
        assertTrue("Episode should not be archived as it is new", !updatedNewEpisode.isArchived)
    }

    @Test
    fun inactiveIncludeStarred() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Days30, includeStarred = true, testDispatcher = testDispatcher)
        val podcastUUID = UUID.randomUUID().toString()

        val podcast = Podcast(uuid = podcastUUID, isSubscribed = true)
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -31)
        val date = calendar.time
        val uuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = uuid, isArchived = false, publishedDate = date, podcastUuid = podcastUUID, addedDate = date, isStarred = true)
        val newUUID = UUID.randomUUID().toString()
        val newEpisode = PodcastEpisode(uuid = newUUID, podcastUuid = podcastUUID, addedDate = Date(), publishedDate = Date(), isArchived = false)
        testDb.podcastDao().insert(podcast)
        episodeDao.insert(episode)
        episodeDao.insert(newEpisode)
        assertTrue("Episode should not be archived before running", !episode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedEpisode = episodeDao.findByUuid(uuid)!!
        assertTrue("Episode should be archived as it is starred and starred is included", updatedEpisode.isArchived)

        val updatedNewEpisode = episodeDao.findByUuid(newUUID)!!
        assertTrue("Episode should not be archived as it is new", !updatedNewEpisode.isArchived)
    }

    @Test
    fun inactiveArchiveModified() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Weeks1, includeStarred = true, testDispatcher = testDispatcher)
        val podcastUUID = UUID.randomUUID().toString()

        val podcast = Podcast(uuid = podcastUUID, isSubscribed = true)
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -10)
        val date = calendar.time
        val calendar6Day = Calendar.getInstance()
        calendar6Day.add(Calendar.DATE, -6)
        val time6Day = calendar6Day.timeInMillis
        val calendar8Day = Calendar.getInstance()
        calendar8Day.add(Calendar.DATE, -8)
        val time8Day = calendar8Day.timeInMillis

        val uuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = uuid, isArchived = false, publishedDate = date, podcastUuid = podcastUUID, addedDate = date, lastArchiveInteraction = time6Day)

        val newUUID = UUID.randomUUID().toString()
        val newEpisode = PodcastEpisode(uuid = newUUID, isArchived = false, publishedDate = date, podcastUuid = podcastUUID, addedDate = date, lastArchiveInteraction = time8Day)

        testDb.podcastDao().insert(podcast)
        episodeDao.insert(episode)
        episodeDao.insert(newEpisode)

        assertTrue("Episode should not be archived before running", !episode.isArchived || !newEpisode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedEpisode = episodeDao.findByUuid(uuid)!!
        assertTrue("Episode should not be archived as it was archive modified 6 days ago (inactive setting = 7d)", !updatedEpisode.isArchived)

        val updatedNewEpisode = episodeDao.findByUuid(newUUID)!!
        assertTrue("Episode should be archived as it was archive modified 8 day ago (inactive setting = 7d)", updatedNewEpisode.isArchived)
    }

    @Test
    fun testPlayed24HoursPodcastOverride() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Never, testDispatcher = testDispatcher)
        val podcast = Podcast(UUID.randomUUID().toString(), overrideGlobalArchive = true, autoArchiveAfterPlaying = AutoArchiveAfterPlayingSetting.Hours24.toIndex())
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -2)
        val date = calendar.time
        val playedUuid = UUID.randomUUID().toString()
        val unplayedUuid = UUID.randomUUID().toString()
        val playedEpisode = PodcastEpisode(uuid = playedUuid, podcastUuid = podcast.uuid, isArchived = false, publishedDate = date, playingStatus = EpisodePlayingStatus.COMPLETED, lastPlaybackInteraction = date.time)
        val unplayedEpisode = PodcastEpisode(uuid = unplayedUuid, podcastUuid = podcast.uuid, isArchived = false, publishedDate = Date(), playingStatus = EpisodePlayingStatus.NOT_PLAYED)

        episodeDao.insert(playedEpisode)
        episodeDao.insert(unplayedEpisode)

        assertTrue("Episode should not be archived before running", !playedEpisode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedPlayedEpisode = episodeDao.findByUuid(playedUuid)!!
        assertTrue("Episode should be archived as it was played 2 days ago and podcast settings are on override", updatedPlayedEpisode.isArchived)
        val updatedUnplayedEpisode = episodeDao.findByUuid(unplayedUuid)!!
        assertTrue("Episode should be not be archived as it hasn't been played", !updatedUnplayedEpisode.isArchived)
    }

    @Test
    fun testInactive30DaysPodcastOverride() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Never, testDispatcher = testDispatcher)
        val podcastUUID = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = podcastUUID, isSubscribed = true, overrideGlobalArchive = true, autoArchiveInactive = AutoArchiveInactiveSetting.Days30.toIndex())
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -31)
        val date = calendar.time
        val uuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = uuid, isArchived = false, publishedDate = date, podcastUuid = podcastUUID, addedDate = date)
        val newUUID = UUID.randomUUID().toString()
        val newEpisode = PodcastEpisode(uuid = newUUID, podcastUuid = podcastUUID, addedDate = Date(), publishedDate = Date(), isArchived = false)
        testDb.podcastDao().insert(podcast)
        episodeDao.insert(episode)
        episodeDao.insert(newEpisode)
        assertTrue("Episode should not be archived before running", !episode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedEpisode = episodeDao.findByUuid(uuid)!!
        assertTrue("Episode should be archived as it is older than 30 days and podcast is overriding global", updatedEpisode.isArchived)

        val updatedNewEpisode = episodeDao.findByUuid(newUUID)!!
        assertTrue("Episode should not be archived as it is new", !updatedNewEpisode.isArchived)
    }

    @Test
    fun testInactive24HoursAddedRecentlyPodcastOverride() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Never, testDispatcher = testDispatcher)
        val podcastUUID = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = podcastUUID, isSubscribed = true, overrideGlobalArchive = true, autoArchiveInactive = AutoArchiveInactiveSetting.Hours24.toIndex())
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, -30)
        val date = calendar.time
        val uuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = uuid, isArchived = false, publishedDate = date, podcastUuid = podcastUUID, addedDate = date)
        val newUUID = UUID.randomUUID().toString()
        val newEpisode = PodcastEpisode(uuid = newUUID, podcastUuid = podcastUUID, addedDate = Date(), publishedDate = date, isArchived = false)
        testDb.podcastDao().insert(podcast)
        episodeDao.insert(episode)
        episodeDao.insert(newEpisode)
        assertTrue("Episode should not be archived before running", !episode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedEpisode = episodeDao.findByUuid(uuid)!!
        assertTrue("Episode should be archived as it is older than 24 hours and podcast is overriding global", updatedEpisode.isArchived)

        val updatedNewEpisode = episodeDao.findByUuid(newUUID)!!
        assertTrue("Episode should not be archived as it is new", !updatedNewEpisode.isArchived)
    }

    @Test
    fun testInactive2DaysAndAfterPlayingPodcastOverride() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.AfterPlaying, AutoArchiveInactiveSetting.Weeks2, testDispatcher = testDispatcher)
        val podcastUUID = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = podcastUUID, isSubscribed = true, overrideGlobalArchive = true, autoArchiveInactive = AutoArchiveInactiveSetting.Days2.toIndex(), autoArchiveAfterPlaying = AutoArchiveAfterPlayingSetting.AfterPlaying.toIndex())
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.set(2019, 0, 24, 11, 30)
        val date = calendar.time
        val uuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = uuid, isArchived = false, publishedDate = date, podcastUuid = podcastUUID, addedDate = date, lastPlaybackInteraction = null, lastDownloadAttemptDate = null)
        val newUUID = UUID.randomUUID().toString()
        val newEpisode = PodcastEpisode(uuid = newUUID, podcastUuid = podcastUUID, addedDate = Date(), publishedDate = date, isArchived = false)
        testDb.podcastDao().insert(podcast)
        episodeDao.insert(episode)
        episodeDao.insert(newEpisode)
        assertTrue("Episode should not be archived before running", !episode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedEpisode = episodeDao.findByUuid(uuid)!!
        assertTrue("Episode should be archived as it is older than 2 days and podcast is overriding global", updatedEpisode.isArchived)

        val updatedNewEpisode = episodeDao.findByUuid(newUUID)!!
        assertTrue("Episode should not be archived as it is new", !updatedNewEpisode.isArchived)
    }

    @Test
    fun testEpisodeLimit() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Never, testDispatcher = testDispatcher)
        val podcast = Podcast(UUID.randomUUID().toString(), autoArchiveEpisodeLimit = 1, overrideGlobalArchive = true)
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -2)
        val date = calendar.time
        val oldestUuid = UUID.randomUUID().toString()
        val unplayedUuid = UUID.randomUUID().toString()
        val oldestEpisode = PodcastEpisode(title = "Oldest", uuid = oldestUuid, podcastUuid = podcast.uuid, isArchived = false, publishedDate = date, playingStatus = EpisodePlayingStatus.COMPLETED, lastPlaybackInteraction = date.time)
        val unplayedEpisode = PodcastEpisode(title = "Newest", uuid = unplayedUuid, podcastUuid = podcast.uuid, isArchived = false, publishedDate = Date(), playingStatus = EpisodePlayingStatus.NOT_PLAYED)

        episodeDao.insert(oldestEpisode)
        episodeDao.insert(unplayedEpisode)

        assertTrue("Episode should not be archived before running", !oldestEpisode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedOldestEpisode = episodeDao.findByUuid(oldestUuid)!!
        assertTrue("Episode should be archived as it was the oldest", updatedOldestEpisode.isArchived)
        val updatedUnplayedEpisode = episodeDao.findByUuid(unplayedUuid)!!
        assertTrue("Episode should be not be archived as it is outside the limit", !updatedUnplayedEpisode.isArchived)
    }

    @Test
    fun testEpisodeLimitIgnoresManualUnarchiveInCount() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Never, testDispatcher = testDispatcher)
        val podcast = Podcast(UUID.randomUUID().toString(), autoArchiveEpisodeLimit = 0, overrideGlobalArchive = true)
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -2)
        val date = calendar.time
        val oldestUuid = UUID.randomUUID().toString()
        val unplayedUuid = UUID.randomUUID().toString()
        val oldestEpisode = PodcastEpisode(title = "Oldest", uuid = oldestUuid, podcastUuid = podcast.uuid, isArchived = false, excludeFromEpisodeLimit = true, publishedDate = date, playingStatus = EpisodePlayingStatus.COMPLETED, lastPlaybackInteraction = date.time)
        val unplayedEpisode = PodcastEpisode(title = "Newest", uuid = unplayedUuid, podcastUuid = podcast.uuid, isArchived = false, publishedDate = Date(), playingStatus = EpisodePlayingStatus.NOT_PLAYED)

        episodeDao.insert(oldestEpisode)
        episodeDao.insert(unplayedEpisode)

        assertTrue("Episode should not be archived before running", !oldestEpisode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedOldestEpisode = episodeDao.findByUuid(oldestUuid)!!
        assertTrue("Episode should not be archived as it was the manually unarchived", !updatedOldestEpisode.isArchived)
        val updatedUnplayedEpisode = episodeDao.findByUuid(unplayedUuid)!!
        assertTrue("Episode should be archived", updatedUnplayedEpisode.isArchived)
    }

    @Test
    fun testEpisodeLimitRespectsIgnoreGlobal() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Never, testDispatcher = testDispatcher)
        val podcast = Podcast(UUID.randomUUID().toString(), autoArchiveEpisodeLimit = 0, overrideGlobalArchive = false)
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -2)
        val date = calendar.time
        val oldestUuid = UUID.randomUUID().toString()
        val oldestEpisode = PodcastEpisode(title = "Oldest", uuid = oldestUuid, podcastUuid = podcast.uuid, isArchived = false, excludeFromEpisodeLimit = true, publishedDate = date, playingStatus = EpisodePlayingStatus.COMPLETED, lastPlaybackInteraction = date.time)

        episodeDao.insert(oldestEpisode)

        assertTrue("Episode should not be archived before running", !oldestEpisode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedOldestEpisode = episodeDao.findByUuid(oldestUuid)!!
        assertTrue("Episode should not be archived as global is off", !updatedOldestEpisode.isArchived)
    }

    @Test
    fun testAddingInactiveEpisodeToUpNext() = runTest {
        val episodeManager = episodeManagerFor(testDb, AutoArchiveAfterPlayingSetting.Never, AutoArchiveInactiveSetting.Weeks1, includeStarred = true, testDispatcher = testDispatcher)
        val upNext = upNextQueueFor(testDb, episodeManager)

        val podcastUUID = UUID.randomUUID().toString()

        val podcast = Podcast(uuid = podcastUUID, isSubscribed = true)
        val podcastManager = podcastManagerThatReturns(podcast)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -10)
        val date = calendar.time
        val calendar6Day = Calendar.getInstance()
        calendar6Day.add(Calendar.DATE, -6)
        val calendar8Day = Calendar.getInstance()
        calendar8Day.add(Calendar.DATE, -8)
        val time8Day = calendar8Day.timeInMillis

        val newUUID = UUID.randomUUID().toString()
        val newEpisode = PodcastEpisode(uuid = newUUID, isArchived = false, publishedDate = date, podcastUuid = podcastUUID, addedDate = date, archivedModified = time8Day)

        testDb.podcastDao().insert(podcast)
        episodeDao.insert(newEpisode)

        assertTrue("Episode should not be archived before running", !newEpisode.isArchived)
        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedNewEpisode = episodeDao.findByUuid(newUUID)!!
        assertTrue("Episode should be archived as it was archive modified 8 day ago (inactive setting = 7d)", updatedNewEpisode.isArchived)

        runBlocking { upNext.playLast(updatedNewEpisode, downloadManager, null) }

        val updatedNewEpisodeInUpNext = episodeDao.findByUuid(newUUID)!!
        assertTrue("Episode should not be archived as it was added to up next", !updatedNewEpisodeInUpNext.isArchived)

        episodeManager.checkForEpisodesToAutoArchive(null, podcastManager)

        val updatedNewEpisodeInUpNextAfterInactive = episodeDao.findByUuid(newUUID)!!
        assertTrue("Episode should not be archived as it was added to up next after being inactive", !updatedNewEpisodeInUpNextAfterInactive.isArchived)
    }
}
