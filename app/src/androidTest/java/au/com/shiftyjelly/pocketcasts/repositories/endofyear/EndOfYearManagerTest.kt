package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastRatingsDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserPodcastRating
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.to.RatingStats
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.utils.extensions.toEpochMillis
import com.squareup.moshi.Moshi
import java.time.Clock
import java.time.Instant
import java.time.Year
import java.time.ZoneId
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndOfYearManagerTest {
    private lateinit var database: AppDatabase
    private lateinit var podcastDao: PodcastDao
    private lateinit var episodeDao: EpisodeDao
    private lateinit var ratingsDao: PodcastRatingsDao

    private lateinit var manager: EndOfYearManager

    private val zone = ZoneId.of("UTC")

    @Before
    fun setupDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        podcastDao = database.podcastDao()
        episodeDao = database.episodeDao()
        ratingsDao = database.podcastRatingsDao()

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
        val stats = manager.getStats()

        val expected = EndOfYearStats(
            playedEpisodeCount = 0,
            completedEpisodeCount = 0,
            playedPodcastIds = emptyList(),
            playbackTime = Duration.ZERO,
            lastYearPlaybackTime = Duration.ZERO,
            topPodcasts = emptyList(),
            longestEpisode = null,
            ratingStats = RatingStats(
                ones = 0,
                twos = 0,
                threes = 0,
                fours = 0,
                fives = 0,
            ),
        )
        assertEquals(expected, stats)
    }

    @Test
    fun getPlayedEpisodeCount() = runTest {
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1001).toEpochMillis(zone) - 1,
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(2, stats.playedEpisodeCount)
    }

    @Test
    fun doNotCountOurOfRangeEpisodesForPlayedEpisodeCount() = runTest {
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone) - 1,
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1001).toEpochMillis(zone),
                ),
                PodcastEpisode(
                    uuid = "id-3",
                    publishedDate = Date(),
                    lastPlaybackInteraction = null,
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(0, stats.playedEpisodeCount)
    }

    @Test
    fun getCompletedEpisodeCount() = runTest {
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                    playingStatus = EpisodePlayingStatus.COMPLETED,
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1001).toEpochMillis(zone) - 1,
                    playingStatus = EpisodePlayingStatus.COMPLETED,
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(2, stats.completedEpisodeCount)
    }

    @Test
    fun doNotCountOurOfRangeEpisodesForCompletedEpisodeCount() = runTest {
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone) - 1,
                    playingStatus = EpisodePlayingStatus.COMPLETED,
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1001).toEpochMillis(zone),
                    playingStatus = EpisodePlayingStatus.COMPLETED,
                ),
                PodcastEpisode(
                    uuid = "id-3",
                    publishedDate = Date(),
                    lastPlaybackInteraction = null,
                    playingStatus = EpisodePlayingStatus.COMPLETED,
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(0, stats.completedEpisodeCount)
    }

    @Test
    fun doNotCountUncompletedEpisodesForCompletedEpisodeCount() = runTest {
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                    playingStatus = EpisodePlayingStatus.NOT_PLAYED,
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                    playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(0, stats.completedEpisodeCount)
    }

    @Test
    fun getPlayedPodcast() = runTest {
        podcastDao.insert(Podcast("p-id-1"))
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1001).toEpochMillis(zone),
                ),
            ),
        )

        podcastDao.insert(Podcast("p-id-2"))
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-3",
                    podcastUuid = "p-id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1001).toEpochMillis(zone) - 1,
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(2, stats.playedPodcastCount)
        assertEquals(listOf("p-id-1", "p-id-2"), stats.playedPodcastIds)
    }

    @Test
    fun doNotCountTheSamePodcastTwiceForPlayedPodcastCount() = runTest {
        podcastDao.insert(Podcast("p-id-1"))
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(1, stats.playedPodcastCount)
        assertEquals(listOf("p-id-1"), stats.playedPodcastIds)
    }

    @Test
    fun doNotCountEpisodesOutOfRangeForPlayedPodcastCount() = runBlocking {
        podcastDao.insert(Podcast("p-id-1"))
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone) - 1,
                ),
            ),
        )

        podcastDao.insert(Podcast("p-id-2"))
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-2",
                    podcastUuid = "p-id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1001).toEpochMillis(zone),
                ),
            ),
        )

        podcastDao.insert(Podcast("p-id-3"))
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-3",
                    podcastUuid = "p-id-3",
                    publishedDate = Date(),
                    lastPlaybackInteraction = null,
                ),
            ),
        )

        podcastDao.insert(Podcast("p-id-4"))

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(0, stats.playedPodcastCount)
        assertEquals(emptyList<String>(), stats.playedPodcastIds)
    }

    @Test
    fun getThisYearPlaybackTime() = runTest {
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                    playedUpTo = 10.seconds.inWholeSeconds.toDouble(),
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1001).toEpochMillis(zone) - 1,
                    playedUpTo = 10.minutes.inWholeSeconds.toDouble(),
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(10.minutes + 10.seconds, stats.playbackTime)
    }

    @Test
    fun doNotCountEpisodesOutOfRangeForThisYearPlaybackTime() = runTest {
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone) - 1,
                    playedUpTo = 10.days.inWholeSeconds.toDouble(),
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1001).toEpochMillis(zone),
                    playedUpTo = 10.hours.inWholeSeconds.toDouble(),
                ),
                PodcastEpisode(
                    uuid = "id-3",
                    publishedDate = Date(),
                    lastPlaybackInteraction = null,
                    playedUpTo = 10.days.inWholeSeconds.toDouble(),
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(Duration.ZERO, stats.playbackTime)
    }

    @Test
    fun getLastYearPlaybackTime() = runTest {
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(999).toEpochMillis(zone),
                    playedUpTo = 10.seconds.inWholeSeconds.toDouble(),
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone) - 1,
                    playedUpTo = 10.minutes.inWholeSeconds.toDouble(),
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(10.minutes + 10.seconds, stats.lastYearPlaybackTime)
    }

    @Test
    fun doNotCountEpisodesOutOfRangeForLastYearPlaybackTime() = runTest {
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(999).toEpochMillis(zone) - 1,
                    playedUpTo = 10.days.inWholeSeconds.toDouble(),
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                    playedUpTo = 10.hours.inWholeSeconds.toDouble(),
                ),
                PodcastEpisode(
                    uuid = "id-3",
                    publishedDate = Date(),
                    lastPlaybackInteraction = null,
                    playedUpTo = 10.days.inWholeSeconds.toDouble(),
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(Duration.ZERO, stats.lastYearPlaybackTime)
    }

    @Test
    fun getTopPodcasts() = runTest {
        podcastDao.insert(Podcast("p-id-1", title = "title-1", author = "author-1"))
        episodeDao.insert(
            PodcastEpisode(
                uuid = "id-1",
                podcastUuid = "p-id-1",
                publishedDate = Date(),
                lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                playedUpTo = 1.0,
            ),
        )

        podcastDao.insert(Podcast("p-id-2", title = "title-2", author = "author-2"))
        episodeDao.insert(
            PodcastEpisode(
                uuid = "id-2",
                podcastUuid = "p-id-2",
                publishedDate = Date(),
                lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                playedUpTo = 10.0,
            ),
        )

        podcastDao.insert(Podcast("p-id-3", title = "title-3", author = "author-3"))
        episodeDao.insert(
            PodcastEpisode(
                uuid = "id-3",
                podcastUuid = "p-id-3",
                publishedDate = Date(),
                lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                playedUpTo = 100.0,
            ),
        )

        podcastDao.insert(Podcast("p-id-4", title = "title-4", author = "author-4"))
        episodeDao.insert(
            PodcastEpisode(
                uuid = "id-4",
                podcastUuid = "p-id-4",
                publishedDate = Date(),
                lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                playedUpTo = 1_000.0,
            ),
        )

        podcastDao.insert(Podcast("p-id-5", title = "title-5", author = "author-5"))
        episodeDao.insert(
            PodcastEpisode(
                uuid = "id-5",
                podcastUuid = "p-id-5",
                publishedDate = Date(),
                lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                playedUpTo = 10_000.0,
            ),
        )

        podcastDao.insert(Podcast("p-id-6", title = "title-6", author = "author-6"))
        episodeDao.insert(
            PodcastEpisode(
                uuid = "id-6",
                podcastUuid = "p-id-6",
                publishedDate = Date(),
                lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                playedUpTo = 100_000.0,
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        val expected = listOf(
            TopPodcast(
                uuid = "p-id-6",
                title = "title-6",
                author = "author-6",
                playbackTimeSeconds = 100_000.0,
                playedEpisodeCount = 1,
            ),
            TopPodcast(
                uuid = "p-id-5",
                title = "title-5",
                author = "author-5",
                playbackTimeSeconds = 10_000.0,
                playedEpisodeCount = 1,
            ),
            TopPodcast(
                uuid = "p-id-4",
                title = "title-4",
                author = "author-4",
                playbackTimeSeconds = 1_000.0,
                playedEpisodeCount = 1,
            ),
            TopPodcast(
                uuid = "p-id-3",
                title = "title-3",
                author = "author-3",
                playbackTimeSeconds = 100.0,
                playedEpisodeCount = 1,
            ),
            TopPodcast(
                uuid = "p-id-2",
                title = "title-2",
                author = "author-2",
                playbackTimeSeconds = 10.0,
                playedEpisodeCount = 1,
            ),
        )
        assertEquals(expected, stats.topPodcasts)
    }

    @Test
    fun doNotCountTheSamePodcastTwiceForTopPodcasts() = runTest {
        podcastDao.insert(Podcast("p-id-1", title = "title-1", author = "author-1"))
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                    playedUpTo = 1.0,
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                    playedUpTo = 1.0,
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(1, stats.topPodcasts.size)
    }

    @Test
    fun sortTopPodcastsByPlaybackTimeAndEpisodeCount() = runTest {
        podcastDao.insert(Podcast("p-id-1", title = "title-1", author = "author-1"))
        repeat(2) { index ->
            episodeDao.insert(
                PodcastEpisode(
                    uuid = "id-$index-1",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                    playedUpTo = 10.0,
                ),
            )
        }

        podcastDao.insert(Podcast("p-id-2", title = "title-2", author = "author-2"))
        repeat(4) { index ->
            episodeDao.insert(
                PodcastEpisode(
                    uuid = "id-$index-2",
                    podcastUuid = "p-id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                    playedUpTo = 5.0,
                ),
            )
        }

        podcastDao.insert(Podcast("p-id-3", title = "title-3", author = "author-3"))
        repeat(10) { index ->
            episodeDao.insert(
                PodcastEpisode(
                    uuid = "id-$index-3",
                    podcastUuid = "p-id-3",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                    playedUpTo = 1.0,
                ),
            )
        }

        val stats = manager.getStats(year = Year.of(1000))

        val expected = listOf(
            TopPodcast(
                uuid = "p-id-2",
                title = "title-2",
                author = "author-2",
                playbackTimeSeconds = 20.0,
                playedEpisodeCount = 4,
            ),
            TopPodcast(
                uuid = "p-id-1",
                title = "title-1",
                author = "author-1",
                playbackTimeSeconds = 20.0,
                playedEpisodeCount = 2,
            ),
            TopPodcast(
                uuid = "p-id-3",
                title = "title-3",
                author = "author-3",
                playbackTimeSeconds = 10.0,
                playedEpisodeCount = 10,
            ),
        )
        assertEquals(expected, stats.topPodcasts)
    }

    @Test
    fun doNotCountEpisodesOutOfRangeForTopPodcasts() = runTest {
        podcastDao.insert(Podcast("p-id-1", title = "title-1", author = "author-1"))
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone) - 1,
                    playedUpTo = 1.0,
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1001).toEpochMillis(zone),
                    playedUpTo = 10.0,
                ),
                PodcastEpisode(
                    uuid = "id-3",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = null,
                    playedUpTo = 100.0,
                ),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        assertEquals(emptyList<TopPodcast>(), stats.topPodcasts)
    }

    @Test
    fun getLongestPlayedEpisode() = runTest {
        podcastDao.insert(Podcast("p-id-1", title = "p-title-1"))
        episodeDao.insert(
            PodcastEpisode(
                uuid = "id-1",
                title = "title-1",
                podcastUuid = "p-id-1",
                publishedDate = Date(),
                lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                playingStatus = EpisodePlayingStatus.COMPLETED,
                duration = 20.0,
                imageUrl = "image-url-1",
                playedUpTo = 20.0,
            ),
        )

        assertEquals(
            LongestEpisode(
                episodeId = "id-1",
                episodeTitle = "title-1",
                podcastId = "p-id-1",
                podcastTitle = "p-title-1",
                durationSeconds = 20.0,
                coverUrl = "image-url-1",
            ),
            manager.getStats(year = Year.of(1000)).longestEpisode,
        )

        podcastDao.insert(Podcast("p-id-2", title = "p-title-2"))
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-2",
                    title = "title-2",
                    podcastUuid = "p-id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone),
                    playingStatus = EpisodePlayingStatus.COMPLETED,
                    duration = 40.0,
                    playedUpTo = 35.0,
                ),
                PodcastEpisode(
                    uuid = "id-3",
                    title = "title-3",
                    podcastUuid = "p-id-2",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1001).toEpochMillis(zone) - 1,
                    playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                    duration = 80.0,
                    playedUpTo = 30.0,
                ),
            ),
        )

        assertEquals(
            LongestEpisode(
                episodeId = "id-2",
                episodeTitle = "title-2",
                podcastId = "p-id-2",
                podcastTitle = "p-title-2",
                durationSeconds = 40.0,
                coverUrl = null,
            ),
            manager.getStats(year = Year.of(1000)).longestEpisode,
        )
    }

    @Test
    fun doNotCountEpisodesOutOfRangeForLongestPlayedEpisode() = runTest {
        podcastDao.insert(Podcast("p-id-1"))
        episodeDao.insertAll(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1000).toEpochMillis(zone) - 1,
                    playingStatus = EpisodePlayingStatus.COMPLETED,
                    duration = 10.0,
                    playedUpTo = 5.0,
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = Year.of(1001).toEpochMillis(zone),
                    playingStatus = EpisodePlayingStatus.COMPLETED,
                    duration = 10.0,
                    playedUpTo = 5.0,
                ),
                PodcastEpisode(
                    uuid = "id-3",
                    podcastUuid = "p-id-1",
                    publishedDate = Date(),
                    lastPlaybackInteraction = null,
                    playingStatus = EpisodePlayingStatus.COMPLETED,
                    duration = 10.0,
                    playedUpTo = 5.0,
                ),
            ),
        )

        assertNull(manager.getStats(year = Year.of(1000)).longestEpisode)
    }

    @Test
    fun getRatingStats() = runTest {
        ratingsDao.insertOrReplaceUserRatings(
            buildList {
                repeat(10) { index ->
                    add(UserPodcastRating(podcastUuid = "id-$index-1", rating = 1, modifiedAt = Date(Year.of(1000).toEpochMillis(zone))))
                }
                repeat(20) { index ->
                    add(UserPodcastRating(podcastUuid = "id-$index-2", rating = 2, modifiedAt = Date(Year.of(1001).toEpochMillis(zone) - 1)))
                }
                repeat(30) { index ->
                    add(UserPodcastRating(podcastUuid = "id-$index-3", rating = 3, modifiedAt = Date(Year.of(1000).toEpochMillis(zone))))
                }
                repeat(40) { index ->
                    add(UserPodcastRating(podcastUuid = "id-$index-4", rating = 4, modifiedAt = Date(Year.of(1000).toEpochMillis(zone))))
                }
                repeat(50) { index ->
                    add(UserPodcastRating(podcastUuid = "id-$index-5", rating = 5, modifiedAt = Date(Year.of(1000).toEpochMillis(zone))))
                }
            },
        )

        val stats = manager.getStats(year = Year.of(1000))

        val expected = RatingStats(
            ones = 10,
            twos = 20,
            threes = 30,
            fours = 40,
            fives = 50,
        )
        assertEquals(expected, stats.ratingStats)
    }

    @Test
    fun doNotCountOutOfRangeRatingsForRatingStats() = runTest {
        ratingsDao.insertOrReplaceUserRatings(
            listOf(
                UserPodcastRating(podcastUuid = "id-1", rating = 1, modifiedAt = Date(Year.of(1000).toEpochMillis(zone) - 1)),
                UserPodcastRating(podcastUuid = "id-2", rating = 1, modifiedAt = Date(Year.of(1001).toEpochMillis(zone))),
            ),
        )

        val stats = manager.getStats(year = Year.of(1000))

        val expected = RatingStats(
            ones = 0,
            twos = 0,
            threes = 0,
            fours = 0,
            fives = 0,
        )
        assertEquals(expected, stats.ratingStats)
    }
}
