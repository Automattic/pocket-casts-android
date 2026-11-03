package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.ChapterOrigin
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.GeneratedChaptersFixture
import au.com.shiftyjelly.pocketcasts.repositories.podcast.testSubscription
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.fingerprint.FingerprintDecodePolicy
import au.com.shiftyjelly.pocketcasts.utils.fingerprint.FingerprintPolicy
import dagger.Lazy
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FingerprintTimingManagerPlaybackTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val episode = PodcastEpisode(
        uuid = "episode-uuid",
        publishedDate = Date(),
        duration = 600.0,
        downloadUrl = "https://example.com/episode.mp3",
        podcastUuid = "podcast-uuid",
    )

    private val generatedChapters = Chapters(
        listOf(
            Chapter(
                title = "Generated",
                startTime = 0.seconds,
                endTime = 60.seconds,
                index = 0,
                uiIndex = 1,
                origin = ChapterOrigin.Generated,
            ),
        ),
    )

    private val playbackState = MutableStateFlow(PlaybackState())
    private val warnOnMeteredNetwork = MutableStateFlow(false)
    private val isActiveNetworkMetered = MutableStateFlow(false)

    private lateinit var referenceRetriever: FingerprintReferenceRetriever
    private lateinit var fixture: GeneratedChaptersFixture

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.SYNCED_TRANSCRIPTS, true)
        FeatureFlag.setEnabled(Feature.GENERATED_CHAPTERS, true)
        referenceRetriever = mock {
            on { loadCachedReference(any()) } doReturn null
            on { fetchReferenceData(any(), any(), any()) } doReturn FingerprintReferenceRetriever.FetchResult.NotFound
        }
    }

    @Test
    fun `free user does not fetch the reference on playback`() {
        createManager(GeneratedChaptersFixture(initialSubscription = null))

        play(generatedChapters)

        verifyNoFetch()
    }

    @Test
    fun `Plus user who hides generated chapters does not fetch the reference on playback`() {
        createManager(GeneratedChaptersFixture(initialShowGeneratedChapters = false))

        play(generatedChapters)

        verifyNoFetch()
    }

    @Test
    fun `Plus user without generated chapters does not fetch the reference on playback`() {
        createManager(GeneratedChaptersFixture())

        play(Chapters())

        verifyNoFetch()
    }

    @Test
    fun `Plus user who shows generated chapters fetches the reference on playback`() {
        createManager(GeneratedChaptersFixture(initialSubscription = testSubscription(SubscriptionTier.Plus)))

        play(generatedChapters)

        verifyFetch()
    }

    @Test
    fun `fetches the reference once generated chapters load mid-episode`() {
        createManager(GeneratedChaptersFixture())

        play(Chapters())
        verifyNoFetch()
        play(generatedChapters)

        verifyFetch()
    }

    @Test
    fun `does not fetch the reference on playback over a metered network when the user asked to be warned`() {
        warnOnMeteredNetwork.value = true
        isActiveNetworkMetered.value = true
        createManager(GeneratedChaptersFixture())

        play(generatedChapters)

        verifyNoFetch()
    }

    @Test
    fun `fetches the reference on playback over a metered network by default`() {
        isActiveNetworkMetered.value = true
        createManager(GeneratedChaptersFixture())

        play(generatedChapters)

        verifyFetch()
    }

    @Test
    fun `open transcript fetches the reference for a free user`() {
        val manager = createManager(GeneratedChaptersFixture(initialSubscription = null))
        play(generatedChapters)

        manager.onTranscriptShown(episode.uuid)

        verifyFetch()
    }

    @Test
    fun `hiding generated chapters mid-episode stops the playback preparation`() {
        val manager = createManager(GeneratedChaptersFixture())
        play(generatedChapters)
        awaitState(manager) { it is FingerprintTimingManager.State.Unavailable }

        fixture.showGeneratedChaptersFlow.value = false
        play(generatedChapters)

        awaitState(manager) { it is FingerprintTimingManager.State.Idle }
    }

    @Test
    fun `hiding generated chapters mid-episode keeps the transcript preparation`() {
        val manager = createManager(GeneratedChaptersFixture())
        play(generatedChapters)
        awaitState(manager) { it is FingerprintTimingManager.State.Unavailable }
        manager.onTranscriptShown(episode.uuid)
        Thread.sleep(NO_FETCH_WAIT_MS)

        fixture.showGeneratedChaptersFlow.value = false
        play(generatedChapters)
        Thread.sleep(NO_FETCH_WAIT_MS)

        assertTrue(manager.state is FingerprintTimingManager.State.Unavailable)
    }

    private fun createManager(fixture: GeneratedChaptersFixture): FingerprintTimingManager {
        this.fixture = fixture
        val playbackManager = mock<PlaybackManager> {
            on { playbackStateFlow } doReturn playbackState
            on { getCurrentEpisode() } doReturn episode
            on { isPlaybackRemote() } doReturn false
        }
        val warnSetting = mock<UserSetting<Boolean>>()
        whenever(warnSetting.value).thenAnswer { warnOnMeteredNetwork.value }
        val settings = mock<Settings>(defaultAnswer = RETURNS_DEEP_STUBS)
        whenever(settings.warnOnMeteredNetwork).thenReturn(warnSetting)
        whenever(settings.cacheEntirePlayingEpisode.value).thenReturn(false)
        return FingerprintTimingManager(
            playbackManager = playbackManager,
            referenceRetriever = referenceRetriever,
            eventHorizon = mock(),
            context = context(),
            chapterManager = Lazy { mock<ChapterManager> { on { hasGeneratedChapters(any()) } doReturn true } },
            settings = settings,
            dataSourceFactory = Lazy { mock() },
            pcmTap = mock(),
            decodePolicy = mock<FingerprintDecodePolicy> { on { current() } doReturn FingerprintPolicy.PLATFORM },
            generatedChaptersAvailability = fixture.availability,
        )
    }

    @Suppress("DEPRECATION")
    private fun context(): Context {
        val networkInfo = mock<NetworkInfo> {
            on { isConnected } doReturn true
            on { isAvailable } doReturn true
        }
        val connectivityManager = mock<ConnectivityManager> {
            on { activeNetworkInfo } doReturn networkInfo
        }
        whenever(connectivityManager.isActiveNetworkMetered).thenAnswer { isActiveNetworkMetered.value }
        val packageManager = mock<PackageManager> {
            on { getApplicationInfo(anyOrNull<String>(), any<Int>()) } doReturn ApplicationInfo()
        }
        return mock {
            on { this.packageManager } doReturn packageManager
            on { packageName } doReturn "au.com.shiftyjelly.pocketcasts.test"
            on { getSystemService(Context.CONNECTIVITY_SERVICE) } doReturn connectivityManager
        }
    }

    private fun play(chapters: Chapters) {
        playbackState.value = PlaybackState(
            state = PlaybackState.State.PLAYING,
            episodeUuid = episode.uuid,
            positionMs = playbackState.value.positionMs + 1_000,
            chapters = chapters,
        )
    }

    private fun verifyFetch() {
        verifyBlocking(referenceRetriever, timeout(VERIFY_TIMEOUT_MS)) { fetchReferenceData(any(), any(), any()) }
    }

    private fun verifyNoFetch() {
        Thread.sleep(NO_FETCH_WAIT_MS)
        verifyBlocking(referenceRetriever, never()) { fetchReferenceData(any(), any(), any()) }
    }

    private fun awaitState(manager: FingerprintTimingManager, predicate: (FingerprintTimingManager.State) -> Boolean) {
        runBlocking {
            withTimeout(VERIFY_TIMEOUT_MS) { manager.stateFlow.first(predicate) }
        }
    }

    private companion object {
        const val VERIFY_TIMEOUT_MS = 2_000L
        const val NO_FETCH_WAIT_MS = 300L
    }
}
