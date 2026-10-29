package au.com.shiftyjelly.pocketcasts.repositories.chat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.ReadSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.jakewharton.rxrelay2.BehaviorRelay
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChatStarterQuestionsPrefetcherTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val upNextChanges = MutableStateFlow<UpNextQueue.State>(UpNextQueue.State.Empty)
    private val upNextQueue = mock<UpNextQueue> {
        on { changesFlow } doReturn upNextChanges
    }
    private val generatedTranscripts = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val transcriptManager = mock<TranscriptManager> {
        on { observeIsGeneratedTranscriptAvailable(any()) } doReturn MutableStateFlow(false)
    }
    private val episodesWithMessages = mutableSetOf<String>()
    private var episodesWithMessagesError: Exception? = null
    private val prefetchedEpisodeUuids = mutableListOf<String>()
    private val chatManager = mock<ChatManager> {
        on { hasChatWithMessages(any()) } doSuspendableAnswer {
            episodesWithMessagesError?.let { error -> throw error }
            it.getArgument<String>(0) in episodesWithMessages
        }
        on { getStarterQuestions(any(), any()) } doSuspendableAnswer {
            prefetchedEpisodeUuids += it.getArgument<String>(0)
            listOf("Who is the guest?")
        }
    }
    private val isLoggedIn = BehaviorRelay.createDefault(true)
    private val syncManager = mock<SyncManager> {
        on { isLoggedInObservable } doReturn isLoggedIn
    }
    private val subscription = MutableStateFlow<Subscription?>(mock())
    private val settings = mock<Settings> {
        val cachedSubscription = mock<ReadSetting<Subscription?>> {
            on { flow } doReturn subscription
        }
        on { this.cachedSubscription } doReturn cachedSubscription
    }
    private val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED, coroutineRule.testDispatcher)

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT, true)
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT_FREE_BETA, false)
    }

    @Test
    fun `prefetch starter questions for the current episode with a generated transcript`() = runTest {
        startPrefetching()

        play(EPISODE, isGeneratedTranscriptAvailable = true)

        assertEquals(listOf(EPISODE.uuid), prefetchedEpisodeUuids)
    }

    @Test
    fun `prefetch once the generated transcript becomes available`() = runTest {
        startPrefetching()

        play(EPISODE)
        assertEquals(emptyList<String>(), prefetchedEpisodeUuids)

        generatedTranscript(EPISODE.uuid).value = true
        assertEquals(listOf(EPISODE.uuid), prefetchedEpisodeUuids)
    }

    @Test
    fun `do not prefetch again when the queue changes behind the current episode`() = runTest {
        startPrefetching()
        play(EPISODE, isGeneratedTranscriptAvailable = true)

        upNextChanges.value = UpNextQueue.State.Loaded(EPISODE.copy(playedUpTo = 30.0), podcast = null, queue = listOf(OTHER_EPISODE))

        assertEquals(listOf(EPISODE.uuid), prefetchedEpisodeUuids)
    }

    @Test
    fun `prefetch the next current episode`() = runTest {
        startPrefetching()
        play(EPISODE, isGeneratedTranscriptAvailable = true)

        play(OTHER_EPISODE, isGeneratedTranscriptAvailable = true)

        assertEquals(listOf(EPISODE.uuid, OTHER_EPISODE.uuid), prefetchedEpisodeUuids)
    }

    @Test
    fun `do not prefetch episodes skipped past quickly`() = runTest {
        startPrefetching()

        skipTo(EPISODE, isGeneratedTranscriptAvailable = true)
        advanceTimeBy(1.seconds)
        play(OTHER_EPISODE, isGeneratedTranscriptAvailable = true)

        assertEquals(listOf(OTHER_EPISODE.uuid), prefetchedEpisodeUuids)
    }

    @Test
    fun `keep prefetching after a failure`() = runTest {
        startPrefetching()
        episodesWithMessagesError = IllegalStateException()
        play(EPISODE, isGeneratedTranscriptAvailable = true)
        episodesWithMessagesError = null

        play(OTHER_EPISODE, isGeneratedTranscriptAvailable = true)

        assertEquals(listOf(OTHER_EPISODE.uuid), prefetchedEpisodeUuids)
    }

    @Test
    fun `do not prefetch an episode left before its transcript is available`() = runTest {
        startPrefetching()
        play(EPISODE)
        play(OTHER_EPISODE, isGeneratedTranscriptAvailable = true)

        generatedTranscript(EPISODE.uuid).value = true

        assertEquals(listOf(OTHER_EPISODE.uuid), prefetchedEpisodeUuids)
    }

    @Test
    fun `do not prefetch when up next is empty`() = runTest {
        startPrefetching()
        play(EPISODE)

        upNextChanges.value = UpNextQueue.State.Empty
        generatedTranscript(EPISODE.uuid).value = true

        assertEquals(emptyList<String>(), prefetchedEpisodeUuids)
    }

    @Test
    fun `do not prefetch for user files`() = runTest {
        startPrefetching()

        play(UserEpisode(uuid = "user-episode", publishedDate = Date()), isGeneratedTranscriptAvailable = true)

        assertEquals(emptyList<String>(), prefetchedEpisodeUuids)
    }

    @Test
    fun `do not prefetch when the episode already has a chat`() = runTest {
        episodesWithMessages += EPISODE.uuid
        startPrefetching()

        play(EPISODE, isGeneratedTranscriptAvailable = true)

        assertEquals(emptyList<String>(), prefetchedEpisodeUuids)
    }

    @Test
    fun `do not prefetch when episode chat is disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT, false)
        startPrefetching()

        play(EPISODE, isGeneratedTranscriptAvailable = true)

        assertEquals(emptyList<String>(), prefetchedEpisodeUuids)
    }

    @Test
    fun `prefetch once the user signs in`() = runTest {
        isLoggedIn.accept(false)
        startPrefetching()
        play(EPISODE, isGeneratedTranscriptAvailable = true)
        assertEquals(emptyList<String>(), prefetchedEpisodeUuids)

        isLoggedIn.accept(true)
        waitForCurrentEpisode()

        assertEquals(listOf(EPISODE.uuid), prefetchedEpisodeUuids)
    }

    @Test
    fun `do not prefetch for free users without the free beta`() = runTest {
        subscription.value = null
        startPrefetching()

        play(EPISODE, isGeneratedTranscriptAvailable = true)

        assertEquals(emptyList<String>(), prefetchedEpisodeUuids)
    }

    @Test
    fun `prefetch for free users in the free beta`() = runTest {
        subscription.value = null
        FeatureFlag.setEnabled(Feature.EPISODE_CHAT_FREE_BETA, true)
        startPrefetching()

        play(EPISODE, isGeneratedTranscriptAvailable = true)

        assertEquals(listOf(EPISODE.uuid), prefetchedEpisodeUuids)
    }

    @Test
    fun `prefetch once the app comes to the foreground`() = runTest {
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        startPrefetching()
        play(EPISODE, isGeneratedTranscriptAvailable = true)
        assertEquals(emptyList<String>(), prefetchedEpisodeUuids)

        lifecycleOwner.currentState = Lifecycle.State.STARTED
        waitForCurrentEpisode()

        assertEquals(listOf(EPISODE.uuid), prefetchedEpisodeUuids)
    }

    @Test
    fun `start prefetching only once`() = runTest {
        val prefetcher = startPrefetching()
        prefetcher.prefetchForCurrentEpisode()

        play(EPISODE, isGeneratedTranscriptAvailable = true)

        assertEquals(listOf(EPISODE.uuid), prefetchedEpisodeUuids)
    }

    private fun TestScope.startPrefetching() = ChatStarterQuestionsPrefetcher(
        upNextQueue = upNextQueue,
        transcriptManager = transcriptManager,
        chatManager = chatManager,
        syncManager = syncManager,
        settings = settings,
        processLifecycleOwner = lifecycleOwner,
        scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
    ).apply { prefetchForCurrentEpisode() }

    private fun TestScope.play(episode: BaseEpisode, isGeneratedTranscriptAvailable: Boolean = false) {
        skipTo(episode, isGeneratedTranscriptAvailable)
        waitForCurrentEpisode()
    }

    private fun TestScope.waitForCurrentEpisode() {
        advanceTimeBy(ChatStarterQuestionsPrefetcher.CURRENT_EPISODE_DELAY)
        runCurrent()
    }

    private fun skipTo(episode: BaseEpisode, isGeneratedTranscriptAvailable: Boolean = false) {
        generatedTranscript(episode.uuid).value = isGeneratedTranscriptAvailable
        upNextChanges.value = UpNextQueue.State.Loaded(episode, podcast = null, queue = emptyList())
    }

    private fun generatedTranscript(episodeUuid: String) = generatedTranscripts.getOrPut(episodeUuid) {
        MutableStateFlow<Boolean>(false).also { flow ->
            whenever(transcriptManager.observeIsGeneratedTranscriptAvailable(episodeUuid)) doReturn flow
        }
    }

    private companion object {
        val EPISODE = PodcastEpisode(uuid = "episode", podcastUuid = "podcast", publishedDate = Date())
        val OTHER_EPISODE = PodcastEpisode(uuid = "other-episode", podcastUuid = "podcast", publishedDate = Date())
    }
}
