package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeDefault
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.BookmarkFeatureControl
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import java.util.UUID
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BookmarksViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Mock
    private lateinit var bookmarkFeature: BookmarkFeatureControl

    private lateinit var bookmarksViewModel: BookmarksViewModel
    private val episodeUuid = UUID.randomUUID().toString()

    @Before
    fun setUp() = runTest {
        val userSetting = mock<UserSetting<SubscriptionStatus?>>()
        whenever(userSetting.flow).thenReturn(MutableStateFlow(cachedSubscriptionStatus))
        whenever(settings.cachedSubscriptionStatus).thenReturn(userSetting)
        whenever(episodeManager.findEpisodeByUuid(episodeUuid)).thenReturn(episode)
        whenever(bookmarkFeature.isAvailable(anyOrNull())).thenReturn(true)
        whenever(bookmarkManager.findEpisodeBookmarksFlow(episode, BookmarksSortTypeDefault.TIMESTAMP)).thenReturn(flowOf(emptyList()))
        val playerBookmarksSortType = mock<UserSetting<BookmarksSortTypeDefault>> {
            on { flow } doReturn MutableStateFlow(BookmarksSortTypeDefault.TIMESTAMP)
        }
        whenever(settings.playerBookmarksSortType).thenReturn(playerBookmarksSortType)
        whenever(multiSelectHelper.isMultiSelectingLive)
            .thenReturn(MutableLiveData<Boolean>().apply { value = false })
        whenever(multiSelectHelper.selectedListLive)
            .thenReturn(MutableLiveData<List<Bookmark>>().apply { value = emptyList() })

        bookmarksViewModel = BookmarksViewModel(
            analyticsTracker = analyticsTracker,
            bookmarkManager = bookmarkManager,
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            multiSelectHelper = multiSelectHelper,
            settings = settings,
            playbackManager = playbackManager,
            theme = theme,
            bookmarkFeature = bookmarkFeature,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `given feature not available, when bookmarks loaded, then Upsell state shown`() = runTest {
        whenever(bookmarkFeature.isAvailable(any())).thenReturn(false)

        bookmarksViewModel.loadBookmarks(episodeUuid, SourceView.PLAYER)

        assertTrue(bookmarksViewModel.uiState.value is BookmarksViewModel.UiState.Upsell)
    }

    @Test
    fun `given feature available, when bookmarks loaded, then Upsell state not shown`() = runTest {
        whenever(bookmarkFeature.isAvailable(any())).thenReturn(true)

        bookmarksViewModel.loadBookmarks(episodeUuid, SourceView.PLAYER)

        assertFalse(bookmarksViewModel.uiState.value is BookmarksViewModel.UiState.Upsell)
    }
}
