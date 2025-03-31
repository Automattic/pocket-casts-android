package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Flowable
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FolderEditViewModelTest {

    lateinit var viewModel: FolderEditViewModel

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        val settings = mock<Settings>()

        val sortType = Mockito.mock<UserSetting<PodcastsSortType>>()
        whenever(sortType.flow).thenReturn(MutableStateFlow(PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST))
        whenever(sortType.value).thenReturn(PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST)
        whenever(settings.podcastsSortType).thenReturn(sortType)

        val gridType = Mockito.mock<UserSetting<PodcastGridLayoutType>>()
        whenever(gridType.flow).thenReturn(MutableStateFlow(PodcastGridLayoutType.LARGE_ARTWORK))
        whenever(gridType.value).thenReturn(PodcastGridLayoutType.LARGE_ARTWORK)
        whenever(settings.podcastGridLayout).thenReturn(gridType)

        val podcastManager = mock<PodcastManager>()
        whenever(podcastManager.podcastsOrderByLatestEpisodeRxFlowable()).thenReturn(Flowable.just(emptyList()))
        whenever(podcastManager.subscribedRxFlowable()).thenReturn(Flowable.just(emptyList()))

        val folderManager = mock<FolderManager>()
        whenever(folderManager.findFoldersFlow()).thenReturn(flowOf(emptyList()))

        whenever(settings.selectPodcastSortTypeObservable).thenReturn(Observable.just(PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST))

        viewModel = FolderEditViewModel(
            podcastManager = podcastManager,
            folderManager = folderManager,
            settings = settings,
            analyticsTracker = mock(),
        )
    }

    @Test
    fun test_nameLength_100() {
        val name = "a".repeat(100)
        viewModel.changeFolderName(name)
        assertEquals(viewModel.folderName.value, name)
    }

    @Test
    fun test_nameLength_101() {
        val expectedName = "a".repeat(100)
        viewModel.changeFolderName(expectedName + "all these characters should be ignored")
        // Drops the characters above 100
        assertEquals(viewModel.folderName.value, expectedName)
    }
}
