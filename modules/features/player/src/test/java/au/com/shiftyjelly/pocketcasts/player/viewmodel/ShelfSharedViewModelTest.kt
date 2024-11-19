package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.NavigationState
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.ShelfItemSource
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.SnackbarMessage
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.TransitionState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.ChromeCastAnalytics
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.BookmarkFeatureControl
import io.reactivex.Observable
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ShelfSharedViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var applicationScope: CoroutineScope

    @Mock
    private lateinit var analyticsTracker: AnalyticsTracker

    @Mock
    private lateinit var bookmarkFeature: BookmarkFeatureControl

    @Mock
    private lateinit var chromeCastAnalytics: ChromeCastAnalytics

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var playbackManager: PlaybackManager

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var upNextQueue: UpNextQueue

    @Mock
    private lateinit var userEpisodeManager: UserEpisodeManager

    private lateinit var shelfSharedViewModel: ShelfSharedViewModel

    @Test
    fun `when effects button clicked, then effects options are shown`() = runTest {
        initViewModel()

        shelfSharedViewModel.navigationState.test {
            shelfSharedViewModel.onEffectsClick(ShelfItemSource.Shelf)
            assertEquals(NavigationState.ShowEffectsOption, awaitItem())
        }
    }

    @Test
    fun `when sleep button clicked, then sleep timer options are shown`() = runTest {
        initViewModel()

        shelfSharedViewModel.navigationState.test {
            shelfSharedViewModel.onSleepClick(ShelfItemSource.Shelf)
            assertEquals(NavigationState.ShowSleepTimerOptions, awaitItem())
        }
    }

    @Test
    fun `when star button clicked, then episode star state is toggled`() = runTest {
        val episode = PodcastEpisode("uuid", publishedDate = Date())
        whenever(upNextQueue.currentEpisode).thenReturn(episode)
        initViewModel()

        shelfSharedViewModel.onStarClick(ShelfItemSource.Shelf)
        verify(episodeManager).toggleStarEpisode(episode, SourceView.PLAYER)
    }

    @Test
    fun `given transcript available, when transcript button clicked, then transcript is opened with transition`() =
        runTest {
            initViewModel()

            shelfSharedViewModel.transitionState.test {
                shelfSharedViewModel.onTranscriptClick(true, ShelfItemSource.Shelf)
                assertEquals(TransitionState.OpenTranscript, awaitItem())
            }
        }

    @Test
    fun `given transcript not available, when transcript button clicked, then transcript not available snackbar message is shown`() =
        runTest {
            initViewModel()

            shelfSharedViewModel.snackbarMessages.test {
                shelfSharedViewModel.onTranscriptClick(false, ShelfItemSource.Shelf)
                assertEquals(SnackbarMessage.TranscriptNotAvailable, awaitItem())
            }
        }

    @Test
    fun `given with transition true, when close transcript called, then transcript is closed with transition`() =
        runTest {
            val podcast = Podcast("podcastUuid")
            val episode = PodcastEpisode("episodeUuid", publishedDate = Date())
            initViewModel()

            shelfSharedViewModel.transitionState.test {
                shelfSharedViewModel.closeTranscript(podcast, episode, true)
                assertEquals(TransitionState.CloseTranscript(true), awaitItem())
            }
        }

    @Test
    fun `given with transition false, when close transcript called, then transcript is closed without transition`() =
        runTest {
            val podcast = Podcast("podcastUuid")
            val episode = PodcastEpisode("episodeUuid", publishedDate = Date())
            initViewModel()

            shelfSharedViewModel.transitionState.test {
                shelfSharedViewModel.closeTranscript(podcast, episode, false)
                assertEquals(TransitionState.CloseTranscript(false), awaitItem())
            }
        }

    @Test
    fun `when download button clicked, then episode download started snackbar message is shown`() =
        runTest {
            initViewModel()

            shelfSharedViewModel.snackbarMessages.test {
                shelfSharedViewModel.onEpisodeDownloadStart(ShelfItemSource.Shelf)
                assertEquals(SnackbarMessage.EpisodeDownloadStarted, awaitItem())
            }
        }

    @Test
    fun `when remove button clicked, then episode removed snackbar message is shown`() = runTest {
        initViewModel()

        shelfSharedViewModel.snackbarMessages.test {
            shelfSharedViewModel.onEpisodeRemoveClick(ShelfItemSource.Shelf)
            assertEquals(SnackbarMessage.EpisodeRemoved, awaitItem())
        }
    }

    @Test
    fun `when share button clicked, then share dialog is shown`() = runTest {
        val podcast = Podcast("podcastUuid")
        val episode = PodcastEpisode("episodeUuid", publishedDate = Date())
        initViewModel()

        shelfSharedViewModel.navigationState.test {
            shelfSharedViewModel.onShareClick(podcast, episode, ShelfItemSource.Shelf)
            assertEquals(NavigationState.ShowShareDialog(podcast, episode), awaitItem())
        }
    }

    @Test
    fun `given podcast available, when show podcast or cloud files button clicked, then podcast is shown`() =
        runTest {
            val podcast = Podcast("podcastUuid")
            initViewModel()

            shelfSharedViewModel.navigationState.test {
                shelfSharedViewModel.onShowPodcastOrCloudFiles(podcast, ShelfItemSource.Shelf)
                assertEquals(NavigationState.ShowPodcast(podcast), awaitItem())
            }
        }

    @Test
    fun `given podcast not available, when show podcast or cloud files button clicked, then cloud files are shown`() =
        runTest {
            initViewModel()

            shelfSharedViewModel.navigationState.test {
                shelfSharedViewModel.onShowPodcastOrCloudFiles(null, ShelfItemSource.Shelf)
                assertEquals(NavigationState.ShowCloudFiles, awaitItem())
            }
        }

    @Test
    fun `when played button clicked, then mark as played confirmation is shown`() = runTest {
        val episode = PodcastEpisode("episodeUuid", publishedDate = Date())
        whenever(upNextQueue.currentEpisode).thenReturn(episode)
        initViewModel()

        shelfSharedViewModel.navigationState.test {
            shelfSharedViewModel.onPlayedClick({ _, _ -> }, ShelfItemSource.Shelf)
            assertTrue(awaitItem() is NavigationState.ShowMarkAsPlayedConfirmation)
        }
    }

    @Test
    fun `given bookmark feature available, when add bookmark button clicked, then add bookmark is shown`() =
        runTest {
            whenever(bookmarkFeature.isAvailable(anyOrNull())).thenReturn(true)
            initViewModel()

            shelfSharedViewModel.navigationState.test {
                shelfSharedViewModel.onAddBookmarkClick(
                    OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION,
                    ShelfItemSource.Shelf,
                )
                assertEquals(NavigationState.ShowAddBookmark, awaitItem())
            }
        }

    @Test
    fun `given bookmark feature not available, when add bookmark button clicked, then upsell flow is started`() =
        runTest {
            whenever(bookmarkFeature.isAvailable(anyOrNull())).thenReturn(false)
            initViewModel()

            shelfSharedViewModel.navigationState.test {
                shelfSharedViewModel.onAddBookmarkClick(
                    OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION,
                    ShelfItemSource.Shelf,
                )
                assertEquals(
                    NavigationState.StartUpsellFlow(OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `given episode is podcast episode, when archive button clicked, then podcast episode archive confirmation is shown`() =
        runTest {
            val episode = PodcastEpisode("episodeUuid", publishedDate = Date())
            whenever(upNextQueue.currentEpisode).thenReturn(episode)
            initViewModel()

            shelfSharedViewModel.navigationState.test {
                shelfSharedViewModel.onArchiveClick({}, ShelfItemSource.Shelf)
                assertTrue(awaitItem() is NavigationState.ShowPodcastEpisodeArchiveConfirmation)
            }
        }

    @Test
    fun `given episode is user episode, when archive button clicked, then user episode delete confirmation is shown`() =
        runTest {
            val episode = UserEpisode("episodeUuid", publishedDate = Date())
            whenever(upNextQueue.currentEpisode).thenReturn(episode)
            initViewModel()

            shelfSharedViewModel.navigationState.test {
                shelfSharedViewModel.onArchiveClick({}, ShelfItemSource.Shelf)
                assertTrue(awaitItem() is NavigationState.ShowUserEpisodeDeleteConfirmation)
            }
        }

    @Test
    fun `when more button clicked, then more actions are shown`() = runTest {
        initViewModel()

        shelfSharedViewModel.navigationState.test {
            shelfSharedViewModel.onMoreClick()
            assertEquals(NavigationState.ShowMoreActions, awaitItem())
        }
    }

    private fun initViewModel() {
        whenever(playbackManager.upNextQueue).thenReturn(upNextQueue)
        whenever(
            upNextQueue.getChangesObservableWithLiveCurrentEpisode(
                episodeManager,
                podcastManager,
            ),
        ).thenReturn(Observable.just(UpNextQueue.State.Empty))
        val userSetting = mock<UserSetting<List<ShelfItem>>>()
        whenever(userSetting.flow).thenReturn(MutableStateFlow(ShelfItem.entries))
        whenever(settings.shelfItems).thenReturn(userSetting)
        whenever(settings.getReportViolationUrl()).thenReturn("")
        shelfSharedViewModel = ShelfSharedViewModel(
            analyticsTracker = analyticsTracker,
            applicationScope = applicationScope,
            bookmarkFeature = bookmarkFeature,
            chromeCastAnalytics = chromeCastAnalytics,
            episodeManager = episodeManager,
            playbackManager = playbackManager,
            podcastManager = podcastManager,
            settings = settings,
            userEpisodeManager = userEpisodeManager,
        )
    }
}
