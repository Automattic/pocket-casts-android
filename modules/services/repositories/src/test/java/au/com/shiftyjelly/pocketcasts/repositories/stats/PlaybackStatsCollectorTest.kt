package au.com.shiftyjelly.pocketcasts.repositories.stats

import au.com.shiftyjelly.pocketcasts.models.db.dao.PlaybackStatsDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PlaybackStatsEvent
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import au.com.shiftyjelly.pocketcasts.utils.UUIDProvider
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackStatsCollectorTest {
    private var episode: BaseEpisode? = null

    private var podcast: Podcast? = null

    private val storedEvents = mutableListOf<PlaybackStatsEvent>()

    private val clock = MutableClock()

    private val uuidProvider = object : UUIDProvider {
        private val _uuids = mutableListOf<UUID>()
        val uuids get() = _uuids.toList()

        override fun generateUUID(): UUID {
            val uuid = UUID.randomUUID()
            _uuids.add(uuid)
            return uuid
        }
    }

    private val testScope = TestScope()

    private val collector = PersistentPlaybackStatsCollector(
        queue = mock<UpNextQueue> {
            on { currentEpisode } doAnswer { episode }
        },
        podcastDao = mock<PodcastDao> {
            on { findPodcastByUuid(any()) } doAnswer { podcast }
        },
        playbackStatsDao = mock<PlaybackStatsDao> {
            on { insertOrIgnore(any()) } doAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                storedEvents.addAll(invocation.arguments[0] as List<PlaybackStatsEvent>)
                Unit
            }
        },
        clock = clock,
        uuidProvider = uuidProvider,
        scope = testScope,
    )

    @Test
    fun `track podcast event`() = testScope.runTest {
        episode = createPodcastEpisode()
        podcast = createPodcast()

        clock += 34.seconds
        collector.onStart()
        runCurrent()

        clock += 10.minutes
        collector.onStop()
        runCurrent()

        assertEquals(
            listOf(
                expectedEvent(
                    uuidIndex = 0,
                    startedAtMs = 34.seconds.inWholeMilliseconds,
                    durationMs = 10.minutes.inWholeMilliseconds,
                ),
            ),
            storedEvents,
        )
    }

    @Test
    fun `track user event`() = testScope.runTest {
        episode = createUserEpisode()

        clock += 34.seconds
        collector.onStart()
        runCurrent()

        clock += 10.minutes
        collector.onStop()
        runCurrent()

        assertEquals(
            listOf(
                expectedEvent(
                    uuidIndex = 0,
                    startedAtMs = 34.seconds.inWholeMilliseconds,
                    durationMs = 10.minutes.inWholeMilliseconds,
                    episodeUuid = "user-episode-id",
                    podcastUuid = Podcast.userPodcast.uuid,
                    episodeTitle = "User episode title",
                    podcastTitle = Podcast.userPodcast.title,
                    podcastCategory = Podcast.userPodcast.getFirstCategoryUnlocalised(),
                ),
            ),
            storedEvents,
        )
    }

    @Test
    fun `split played event at midnight UTC`() = testScope.runTest {
        episode = createPodcastEpisode()
        podcast = createPodcast()

        clock += 23.hours + 59.minutes + 59.seconds
        collector.onStart()
        runCurrent()

        clock += 2.seconds
        collector.onStop()
        runCurrent()

        assertEquals(
            listOf(
                expectedEvent(
                    uuidIndex = 0,
                    startedAtMs = (23.hours + 59.minutes + 59.seconds).inWholeMilliseconds,
                    durationMs = 1.seconds.inWholeMilliseconds,
                ),
                expectedEvent(
                    uuidIndex = 1,
                    startedAtMs = 24.hours.inWholeMilliseconds,
                    durationMs = 1.seconds.inWholeMilliseconds,
                ),
            ),
            storedEvents,
        )
    }

    @Test
    fun `reuse cached podcast across playback sessions`() = testScope.runTest {
        episode = createPodcastEpisode()
        podcast = createPodcast()

        collector.onStart()
        runCurrent()

        clock += 10.seconds
        collector.onStop()
        runCurrent()

        episode = createPodcastEpisode(uuid = "episode-id-2", title = "Episode title 2")
        podcast = null

        clock += 5.seconds
        collector.onStart()
        runCurrent()

        clock += 20.seconds
        collector.onStop()
        runCurrent()

        assertEquals(
            listOf(
                expectedEvent(
                    uuidIndex = 0,
                    startedAtMs = 0,
                    durationMs = 10.seconds.inWholeMilliseconds,
                ),
                expectedEvent(
                    uuidIndex = 1,
                    episodeUuid = "episode-id-2",
                    episodeTitle = "Episode title 2",
                    startedAtMs = 15.seconds.inWholeMilliseconds,
                    durationMs = 20.seconds.inWholeMilliseconds,
                ),
            ),
            storedEvents,
        )
    }

    @Test
    fun `ignore repeated onStart while already playing`() = testScope.runTest {
        episode = createPodcastEpisode()
        podcast = createPodcast()

        clock += 34.seconds
        collector.onStart()

        clock += 10.minutes
        collector.onStart()
        runCurrent()

        clock += 5.minutes
        collector.onStop()
        runCurrent()

        assertEquals(
            listOf(
                expectedEvent(
                    uuidIndex = 0,
                    startedAtMs = 34.seconds.inWholeMilliseconds,
                    durationMs = 15.minutes.inWholeMilliseconds,
                ),
            ),
            storedEvents,
        )
    }

    @Test
    fun `ignore repeated onStop after playback ends`() = testScope.runTest {
        episode = createPodcastEpisode()
        podcast = createPodcast()

        clock += 34.seconds
        collector.onStart()
        runCurrent()

        clock += 10.minutes
        collector.onStop()

        clock += 5.minutes
        collector.onStop()
        runCurrent()

        assertEquals(
            listOf(
                expectedEvent(
                    uuidIndex = 0,
                    startedAtMs = 34.seconds.inWholeMilliseconds,
                    durationMs = 10.minutes.inWholeMilliseconds,
                ),
            ),
            storedEvents,
        )
    }

    private fun createPodcastEpisode(
        uuid: String = "episode-id",
        title: String = "Episode title",
        podcastUuid: String = "podcast-id",
    ) = PodcastEpisode(
        uuid = uuid,
        title = title,
        podcastUuid = podcastUuid,
        publishedDate = Date(),
    )

    private fun createUserEpisode(
        uuid: String = "user-episode-id",
        title: String = "User episode title",
    ) = UserEpisode(
        uuid = uuid,
        title = title,
        publishedDate = Date(),
    )

    private fun createPodcast(
        uuid: String = "podcast-id",
        title: String = "Podcast title",
        category: String = "Podcast category",
    ) = Podcast(
        uuid = uuid,
        title = title,
        podcastCategory = category,
    )

    private fun expectedEvent(
        uuidIndex: Int,
        startedAtMs: Long,
        durationMs: Long,
        episodeUuid: String = "episode-id",
        podcastUuid: String = "podcast-id",
        episodeTitle: String = "Episode title",
        podcastTitle: String = "Podcast title",
        podcastCategory: String = "Podcast category",
    ) = PlaybackStatsEvent(
        uuid = uuidProvider.uuids[uuidIndex].toString(),
        episodeUuid = episodeUuid,
        podcastUuid = podcastUuid,
        episodeTitle = episodeTitle,
        podcastTitle = podcastTitle,
        podcastCategory = podcastCategory,
        startedAtMs = startedAtMs,
        durationMs = durationMs,
        isSynced = false,
    )
}
