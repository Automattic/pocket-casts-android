package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragmentViewModel.EpisodeContentTab.DESCRIPTION
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragmentViewModel.EpisodeContentTab.SUMMARY
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragmentViewModel.EpisodePageState
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadProgressCache
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.automattic.eventhorizon.EpisodeSummarySourceType
import com.automattic.eventhorizon.EpisodeSummaryTappedEvent
import com.automattic.eventhorizon.EventHorizon
import io.reactivex.Flowable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class EpisodeFragmentViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    lateinit var episodeManager: EpisodeManager

    @Mock
    lateinit var podcastManager: PodcastManager

    @Mock
    lateinit var theme: Theme

    @Mock
    lateinit var playbackManager: PlaybackManager

    @Mock
    lateinit var settings: au.com.shiftyjelly.pocketcasts.preferences.Settings

    @Mock
    lateinit var downloadQueue: DownloadQueue

    @Mock
    lateinit var downloadProgressCache: DownloadProgressCache

    @Mock
    lateinit var showNotesManager: ShowNotesManager

    @Mock
    lateinit var transcriptManager: TranscriptManager

    @Mock
    lateinit var userManager: UserManager

    private val eventSink = TestEventSink()

    private lateinit var viewModel: EpisodeFragmentViewModel

    @Before
    fun setUp() {
        whenever(playbackManager.playbackStateLive).thenReturn(
            androidx.lifecycle.MutableLiveData(PlaybackState()),
        )
        whenever(userManager.getSignInState()).thenReturn(
            Flowable.just(SignInState.SignedOut),
        )
        viewModel = EpisodeFragmentViewModel(
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            theme = theme,
            playbackManager = playbackManager,
            settings = settings,
            downloadQueue = downloadQueue,
            downloadProgressCache = downloadProgressCache,
            showNotesManager = showNotesManager,
            eventHorizon = EventHorizon(eventSink),
            transcriptManager = transcriptManager,
            userManager = userManager,
        )
    }

    @Test
    fun `selecting summary tab tracks event when episode and podcast are set`() {
        val episode = PodcastEpisode(uuid = "ep-uuid", publishedDate = java.util.Date())
        val podcast = Podcast(uuid = "pod-uuid")
        viewModel.episode = episode
        viewModel.podcast = podcast

        viewModel.selectContentTab(SUMMARY)

        assertEquals(
            EpisodeSummaryTappedEvent(
                source = EpisodeSummarySourceType.EpisodeDetails,
                episodeUuid = "ep-uuid",
                podcastUuid = "pod-uuid",
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `selecting summary tab does not track when episode is null`() {
        viewModel.podcast = Podcast(uuid = "pod-uuid")

        viewModel.selectContentTab(SUMMARY)

        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `selecting summary tab does not track when podcast is null`() {
        viewModel.episode = PodcastEpisode(uuid = "ep-uuid", publishedDate = java.util.Date())

        viewModel.selectContentTab(SUMMARY)

        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `selecting description tab does not track summary event`() {
        viewModel.episode = PodcastEpisode(uuid = "ep-uuid", publishedDate = java.util.Date())
        viewModel.podcast = Podcast(uuid = "pod-uuid")

        viewModel.selectContentTab(DESCRIPTION)

        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `clearing summary resets selected tab to description`() {
        val state = EpisodePageState(
            summary = "Episode summary",
            selectedContentTab = SUMMARY,
        )

        val newState = state.withSummary(null)

        assertNull(newState.summary)
        assertEquals(DESCRIPTION, newState.selectedContentTab)
    }

    @Test
    fun `clearing summary keeps description selected`() {
        val state = EpisodePageState(
            summary = "Episode summary",
            selectedContentTab = DESCRIPTION,
        )

        val newState = state.withSummary(null)

        assertNull(newState.summary)
        assertEquals(DESCRIPTION, newState.selectedContentTab)
    }

    @Test
    fun `summary tab cannot be selected without summary`() {
        val state = EpisodePageState()

        val newState = state.selectContentTab(SUMMARY)

        assertEquals(DESCRIPTION, newState.selectedContentTab)
    }

    @Test
    fun `summary tab can be selected when summary exists`() {
        val state = EpisodePageState(summary = "Episode summary")

        val newState = state.selectContentTab(SUMMARY)

        assertEquals(SUMMARY, newState.selectedContentTab)
    }
}
