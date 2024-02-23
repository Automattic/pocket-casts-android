package au.com.shiftyjelly.pocketcasts.ui

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.ui.MainActivityViewModel.NavigationState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Flowable
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private const val TEST_EPISODE_UUID = "test_episode_uuid"
private const val TEST_BOOKMARK_UUID = "test_bookmark_uuid"

@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    lateinit var episodeManager: EpisodeManager

    @Mock
    lateinit var playbackManager: PlaybackManager

    @Mock
    lateinit var userManager: UserManager

    @Mock
    lateinit var settings: Settings

    @Mock
    lateinit var endOfYearManager: EndOfYearManager

    @Mock
    lateinit var multiSelectBookmarksHelper: MultiSelectBookmarksHelper

    @Mock
    lateinit var podcastManager: PodcastManager

    @Mock
    lateinit var bookmarkManager: BookmarkManager

    @Mock
    lateinit var theme: Theme

    @Mock
    lateinit var bookmark: Bookmark

    @Mock
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var viewModel: MainActivityViewModel

    private val episode = UserEpisode(uuid = TEST_EPISODE_UUID, publishedDate = Date())

    @Before
    fun setup() = runTest {
        whenever(playbackManager.playbackStateRelay).thenReturn(BehaviorRelay.create<PlaybackState>().toSerialized())
    }

    /* What's new tests */

    @Test
    fun `given a new user, then what's new should not be displayed`() = runTest {
        initViewModel(isNewUser = true)

        viewModel.state.test {
            assertFalse(awaitItem().shouldShowWhatsNew)
        }
    }

    @Test
    fun `given an user and is previous version, then what's should be displayed`() = runTest {
        initViewModel(shouldSetPreviousVersion = true)

        viewModel.state.test {
            assertTrue(awaitItem().shouldShowWhatsNew)
        }
    }

    @Test
    fun `given an user and is current version, then what's should not be displayed`() = runTest {
        initViewModel(shouldSetPreviousVersion = false)

        viewModel.state.test {
            assertFalse(awaitItem().shouldShowWhatsNew)
        }
    }

    /* Bookmark added notification tests */

    @Test
    fun `given episode for bookmark is current playing, when bookmark viewed from notification, then bookmarks on player shown`() = runTest {
        whenever(bookmark.episodeUuid).thenReturn(TEST_EPISODE_UUID)
        whenever(bookmarkManager.findBookmark(anyString(), eq(false))).thenReturn(bookmark)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)
        initViewModel()

        viewModel.navigationState.test {
            viewModel.viewBookmark("")
            assertTrue(awaitItem() is NavigationState.BookmarksForCurrentlyPlaying)
        }
    }

    @Test
    fun `given podcast episode for bookmark currently not playing, when bookmark viewed from notification, then bookmarks for podcast episode shown`() = runTest {
        whenever(bookmark.episodeUuid).thenReturn(TEST_EPISODE_UUID)
        whenever(bookmarkManager.findBookmark(anyString(), eq(false))).thenReturn(bookmark)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(null)
        whenever(episodeManager.findEpisodeByUuid(TEST_EPISODE_UUID)).thenReturn(mock<PodcastEpisode>())
        initViewModel()

        viewModel.navigationState.test {
            viewModel.viewBookmark("")
            assertTrue(awaitItem() is NavigationState.BookmarksForPodcastEpisode)
        }
    }

    @Test
    fun `given user episode for bookmark currently not playing, when bookmark viewed from notification, then bookmarks for user episode shown`() = runTest {
        whenever(bookmark.episodeUuid).thenReturn(TEST_EPISODE_UUID)
        whenever(bookmarkManager.findBookmark(anyString(), eq(false))).thenReturn(bookmark)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(null)
        whenever(episodeManager.findEpisodeByUuid(TEST_EPISODE_UUID)).thenReturn(mock<UserEpisode>())
        initViewModel()

        viewModel.navigationState.test {
            viewModel.viewBookmark("")
            assertTrue(awaitItem() is NavigationState.BookmarksForUserEpisode)
        }
    }

    @Test
    fun `given bookmark exists, when delete tapped from bookmark added notification, then bookmark is deleted`() = runTest {
        whenever(bookmarkManager.findBookmark(TEST_BOOKMARK_UUID)).thenReturn(mock())
        initViewModel()

        viewModel.deleteBookmark(TEST_BOOKMARK_UUID)

        verify(bookmarkManager).deleteToSync(anyString())
    }

    @Test
    fun `given bookmark exists, when delete tapped from bookmark added notification, then bookmark deleted message shown`() = runTest {
        whenever(bookmarkManager.findBookmark(TEST_BOOKMARK_UUID)).thenReturn(mock())
        initViewModel()

        viewModel.snackbarMessage.test {
            viewModel.deleteBookmark(TEST_BOOKMARK_UUID)
            assertTrue(awaitItem() == R.string.bookmarks_deleted_singular)
        }
    }

    @Test
    fun `given bookmark does not exists, when delete tapped from bookmark added notification, then bookmark not found message shown`() = runTest {
        whenever(bookmarkManager.findBookmark(TEST_BOOKMARK_UUID)).thenReturn(null)
        initViewModel()

        viewModel.snackbarMessage.test {
            viewModel.deleteBookmark(TEST_BOOKMARK_UUID)
            assertTrue(awaitItem() == R.string.bookmark_not_found)
        }
    }

    private fun initViewModel(
        isNewUser: Boolean = false,
        shouldSetPreviousVersion: Boolean = false,
    ) {
        if (isNewUser) {
            whenever(settings.getMigratedVersionCode()).thenReturn(0)
        } else {
            whenever(settings.getMigratedVersionCode()).thenReturn(1)
        }
        if (shouldSetPreviousVersion) {
            whenever(settings.getWhatsNewVersionCode()).thenReturn(2) // this is less than the current what's new version code and so will trigger what's new
        } else {
            whenever(settings.getWhatsNewVersionCode()).thenReturn(Settings.WHATS_NEW_VERSION_CODE)
        }

        whenever(userManager.getSignInState()).thenReturn(
            Flowable.just(
                SignInState.SignedIn(
                    email = "",
                    subscriptionStatus = SubscriptionStatus.Free(),
                ),
            ),
        )

        viewModel = MainActivityViewModel(
            episodeManager = episodeManager,
            playbackManager = playbackManager,
            userManager = userManager,
            settings = settings,
            endOfYearManager = endOfYearManager,
            multiSelectBookmarksHelper = multiSelectBookmarksHelper,
            podcastManager = podcastManager,
            bookmarkManager = bookmarkManager,
            theme = theme,
            analyticsTracker = analyticsTracker,
        )
    }
}
