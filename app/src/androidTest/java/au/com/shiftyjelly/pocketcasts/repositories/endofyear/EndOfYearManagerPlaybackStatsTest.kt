package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackPodcastStats
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import com.squareup.moshi.Moshi
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndOfYearManagerPlaybackStatsTest {
    private lateinit var database: AppDatabase
    private lateinit var podcastDao: PodcastDao
    private lateinit var episodeDao: EpisodeDao

    private lateinit var manager: EndOfYearManager

    private val zone = ZoneId.of("Australia/Adelaide")
    private val year = Year.of(2026)

    @Before
    fun setupDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        podcastDao = database.podcastDao()
        episodeDao = database.episodeDao()

        manager = EndOfYearManagerImpl(
            endOfYearDao = database.endOfYearDao(),
            clock = Clock.fixed(Instant.EPOCH, zone),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun getStatsWithoutAnyData() = runTest {
        val stats = manager.getPlaybackStats(year)

        val expected = PlaybackStats(
            podcasts = emptyList(),
            monthlySeconds = List(12) { 0.0 },
            lastYearSeconds = 0.0,
            startedEpisodeCount = 0,
            completedEpisodeCount = 0,
        )
        assertEquals(expected, stats)
    }

    @Test
    fun getPodcastStatsMostListenedFirst() = runTest {
        podcastDao.insertBlocking(Podcast(uuid = "podcast-1", title = "Title 1", author = "Author 1", podcastCategory = "Comedy\nSociety & Culture"))
        podcastDao.insertBlocking(Podcast(uuid = "podcast-2", title = "Title 2", author = "Author 2", podcastCategory = ""))
        podcastDao.insertBlocking(Podcast(uuid = "podcast-3", title = "Title 3", author = "Author 3"))
        episodeDao.insertAllBlocking(
            listOf(
                episode(uuid = "id-1", podcastUuid = "podcast-1", playedUpTo = 100.0),
                episode(uuid = "id-2", podcastUuid = "podcast-1", playedUpTo = 50.0),
                episode(uuid = "id-3", podcastUuid = "podcast-2", playedUpTo = 400.0),
                // Interacted with but never listened to
                episode(uuid = "id-4", podcastUuid = "podcast-3", playedUpTo = 0.0),
                // Listened to in another year
                episode(uuid = "id-5", podcastUuid = "podcast-1", playedUpTo = 1000.0, date = LocalDate.of(2025, 12, 31)),
            ),
        )

        val podcasts = manager.getPlaybackStats(year).podcasts

        assertEquals(listOf("podcast-2", "podcast-1"), podcasts.map(PlaybackPodcastStats::uuid))
        assertEquals(listOf(400.0, 150.0), podcasts.map(PlaybackPodcastStats::playedSeconds))
        assertEquals(listOf(1, 2), podcasts.map(PlaybackPodcastStats::playedEpisodeCount))
        assertEquals(listOf(null, "Comedy"), podcasts.map(PlaybackPodcastStats::firstCategory))
        assertEquals(listOf("Title 2", "Title 1"), podcasts.map(PlaybackPodcastStats::title))
        assertEquals(listOf("Author 2", "Author 1"), podcasts.map(PlaybackPodcastStats::author))
    }

    @Test
    fun getMonthlySecondsInTheClockZone() = runTest {
        episodeDao.insertAllBlocking(
            listOf(
                episode(uuid = "id-1", playedUpTo = 10.0, date = LocalDate.of(2026, 1, 1)),
                episode(uuid = "id-2", playedUpTo = 20.0, date = LocalDate.of(2026, 1, 31)),
                episode(uuid = "id-3", playedUpTo = 30.0, date = LocalDate.of(2026, 6, 15)),
                episode(uuid = "id-4", playedUpTo = 40.0, date = LocalDate.of(2026, 12, 31)),
                episode(uuid = "id-5", playedUpTo = 50.0, date = LocalDate.of(2027, 1, 1)),
            ),
        )

        val monthlySeconds = manager.getPlaybackStats(year).monthlySeconds

        assertEquals(listOf(30.0, 0.0, 0.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, 40.0), monthlySeconds)
    }

    @Test
    fun getLastYearSeconds() = runTest {
        episodeDao.insertAllBlocking(
            listOf(
                episode(uuid = "id-1", playedUpTo = 10.0, date = LocalDate.of(2025, 1, 1)),
                episode(uuid = "id-2", playedUpTo = 20.0, date = LocalDate.of(2025, 12, 31)),
                episode(uuid = "id-3", playedUpTo = 30.0, date = LocalDate.of(2026, 1, 1)),
                episode(uuid = "id-4", playedUpTo = 40.0, date = LocalDate.of(2024, 12, 31)),
            ),
        )

        assertEquals(30.0, manager.getPlaybackStats(year).lastYearSeconds, 0.0)
    }

    @Test
    fun countStartedAndCompletedEpisodes() = runTest {
        episodeDao.insertAllBlocking(
            listOf(
                episode(uuid = "id-1", playedUpTo = 10.0, duration = 100.0),
                episode(uuid = "id-2", playedUpTo = 10.0, duration = 100.0, playingStatus = EpisodePlayingStatus.COMPLETED),
                episode(uuid = "id-3", playedUpTo = 90.0, duration = 100.0),
                episode(uuid = "id-4", playedUpTo = 89.0, duration = 100.0),
                episode(uuid = "id-5", playedUpTo = 10.0, duration = 0.0),
                episode(uuid = "id-6", playedUpTo = 100.0, duration = 100.0, date = LocalDate.of(2025, 6, 1)),
            ),
        )

        val stats = manager.getPlaybackStats(year)

        assertEquals(5, stats.startedEpisodeCount)
        assertEquals(2, stats.completedEpisodeCount)
    }

    private fun episode(
        uuid: String,
        podcastUuid: String = "podcast-id",
        playedUpTo: Double,
        duration: Double = 0.0,
        playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.IN_PROGRESS,
        date: LocalDate = LocalDate.of(2026, 6, 1),
    ) = PodcastEpisode(
        uuid = uuid,
        podcastUuid = podcastUuid,
        publishedDate = Date(),
        playedUpTo = playedUpTo,
        duration = duration,
        playingStatus = playingStatus,
        lastPlaybackInteraction = date.atStartOfDay(zone).plusHours(12).toInstant().toEpochMilli(),
    )
}
