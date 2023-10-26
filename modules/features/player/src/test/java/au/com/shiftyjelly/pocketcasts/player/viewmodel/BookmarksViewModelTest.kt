package au.com.shiftyjelly.pocketcasts.player.viewmodel

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureWrapper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BookmarksViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var bookmarkManager: BookmarkManager

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var multiSelectHelper: MultiSelectBookmarksHelper

    @Mock
    private lateinit var episode: BaseEpisode

    @Mock
    private lateinit var cachedSubscriptionStatus: SubscriptionStatus

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var playbackManager: PlaybackManager

    @Mock
    private lateinit var theme: Theme

    @Mock
    private lateinit var feature: FeatureWrapper

    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var bookmarksViewModel: BookmarksViewModel
    private val episodeUuid = UUID.randomUUID().toString()

    @Before
    fun setUp() = runTest {
        val userSetting = mock<UserSetting<SubscriptionStatus?>>()
        whenever(userSetting.flow).thenReturn(MutableStateFlow(cachedSubscriptionStatus))
        whenever(settings.cachedSubscriptionStatus).thenReturn(userSetting)
        whenever(episodeManager.findEpisodeByUuid(episodeUuid)).thenReturn(episode)
        whenever(feature.isUserEntitled(eq(Feature.BOOKMARKS_ENABLED), anyOrNull())).thenReturn(true)

        bookmarksViewModel = BookmarksViewModel(
            bookmarkManager = bookmarkManager,
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            multiSelectHelper = multiSelectHelper,
            settings = settings,
            playbackManager = playbackManager,
            theme = theme,
            feature = feature,
            ioDispatcher = UnconfinedTestDispatcher(),
            analyticsTracker = analyticsTracker,
        )
    }

    /*@Test
    fun `given no bookmarks, when bookmarks loaded, then Empty state shown`() = runTest {
        whenever(bookmarkManager.findEpisodeBookmarksFlow(episode)).thenReturn(flowOf(emptyList()))

        bookmarksViewModel.loadBookmarks(episodeUuid)

        assertTrue(bookmarksViewModel.uiState.value is BookmarksViewModel.UiState.Empty)
    }

    @Test
    fun `given bookmarks present, when bookmarks loaded, then Loaded state shown`() = runTest {
        whenever(bookmarkManager.findEpisodeBookmarksFlow(episode)).thenReturn(flowOf(listOf(mock())))

        bookmarksViewModel.loadBookmarks(episodeUuid)

        assertTrue(bookmarksViewModel.uiState.value is BookmarksViewModel.UiState.Loaded)
    }*/

    @Test
    fun `given feature not available, when bookmarks loaded, then Upsell state shown`() = runTest {
        whenever(feature.isUserEntitled(eq(Feature.BOOKMARKS_ENABLED), anyOrNull())).thenReturn(false)

        bookmarksViewModel.loadBookmarks(episodeUuid, SourceView.PLAYER)

        assertTrue(bookmarksViewModel.uiState.value is BookmarksViewModel.UiState.Upsell)
    }

    @Test
    fun `given feature available, when bookmarks loaded, then Upsell state not shown`() = runTest {
        whenever(feature.isUserEntitled(eq(Feature.BOOKMARKS_ENABLED), anyOrNull())).thenReturn(true)

        bookmarksViewModel.loadBookmarks(episodeUuid, SourceView.PLAYER)

        assertFalse(bookmarksViewModel.uiState.value is BookmarksViewModel.UiState.Upsell)
    }
}
