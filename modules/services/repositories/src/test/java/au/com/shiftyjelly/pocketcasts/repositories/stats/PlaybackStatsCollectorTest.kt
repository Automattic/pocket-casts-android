package au.com.shiftyjelly.pocketcasts.repositories.stats

import au.com.shiftyjelly.pocketcasts.models.db.dao.PlaybackStatsDao
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PlaybackStatsEvent
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.appreview.TestSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import au.com.shiftyjelly.pocketcasts.utils.UUIDProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.time.Instant
import java.time.ZoneId
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackStatsCollectorTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private var episode: BaseEpisode? = null

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

    private val statsSetting = TestSetting(true)

    private val testScope = TestScope()

    private val collector = PersistentPlaybackStatsCollector(
        queue = mock<UpNextQueue> {
            on { currentEpisode } doAnswer { episode }
        },
        playbackStatsDao = mock<PlaybackStatsDao> {
            on { insertOrIgnore(any()) } doAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                storedEvents.addAll(invocation.arguments[0] as List<PlaybackStatsEvent>)
                Unit
            }
        },
        settings = mock<Settings> {
            on { collectListeningStats } doAnswer { statsSetting }
        },
        clock = clock,
        uuidProvider = uuidProvider,
        scope = testScope,
    )

    @Before
    fun setup() {
        FeatureFlag.setEnabled(Feature.IMPROVED_LISTENING_STATS, true)
    }

    @Test
    fun `track podcast event`() = testScope.runTest {
        episode = createPodcastEpisode()

        clock += 34.seconds
        collector.onStart()

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
                ),
            ),
            storedEvents,
        )
    }

    @Test
    fun `save startedAtMs as epoch millis when clock zone is not UTC`() = testScope.runTest {
        episode = createPodcastEpisode()

        val startedAt = Instant.parse("2026-01-01T13:14:15Z")
        clock.setInstant(startedAt)
        clock.setZone(ZoneId.of("Australia/Sydney"))

        collector.onStart()

        clock += 10.minutes
        collector.onStop()
        runCurrent()

        assertEquals(
            listOf(
                expectedEvent(
                    uuidIndex = 0,
                    startedAtMs = startedAt.toEpochMilli(),
                    durationMs = 10.minutes.inWholeMilliseconds,
                ),
            ),
            storedEvents,
        )
    }

    @Test
    fun `split played event at midnight UTC`() = testScope.runTest {
        episode = createPodcastEpisode()

        clock += 23.hours + 59.minutes + 59.seconds
        collector.onStart()

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
    fun `ignore repeated onStart while already playing`() = testScope.runTest {
        episode = createPodcastEpisode()

        clock += 34.seconds
        collector.onStart()

        clock += 10.minutes
        collector.onStart()

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

        clock += 34.seconds
        collector.onStart()

        clock += 10.minutes
        collector.onStop()
        runCurrent()

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

    @Test
    fun `ignore event when listening stats collection is disabled`() = testScope.runTest {
        episode = createPodcastEpisode()
        statsSetting.set(false)

        clock += 34.seconds
        collector.onStart()

        clock += 10.minutes
        collector.onStop()
        runCurrent()

        assertEquals(emptyList<PlaybackStatsEvent>(), storedEvents)
    }

    @Test
    fun `track event when listening stats collection is disabled and feature flag is disabled`() = testScope.runTest {
        FeatureFlag.setEnabled(Feature.IMPROVED_LISTENING_STATS, false)
        episode = createPodcastEpisode()
        statsSetting.set(false)

        clock += 34.seconds
        collector.onStart()

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

    private fun createPodcastEpisode(
        uuid: String = "episode-id",
        podcastUuid: String = "podcast-id",
    ) = PodcastEpisode(
        uuid = uuid,
        podcastUuid = podcastUuid,
        publishedDate = Date(),
    )

    private fun createUserEpisode(
        uuid: String = "user-episode-id",
    ) = UserEpisode(
        uuid = uuid,
        publishedDate = Date(),
    )

    private fun expectedEvent(
        uuidIndex: Int,
        startedAtMs: Long,
        durationMs: Long,
        episodeUuid: String = "episode-id",
        podcastUuid: String = "podcast-id",
    ) = PlaybackStatsEvent(
        uuid = uuidProvider.uuids[uuidIndex].toString(),
        episodeUuid = episodeUuid,
        podcastUuid = podcastUuid,
        startedAtMs = startedAtMs,
        durationMs = durationMs,
    )
}
