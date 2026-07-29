package au.com.shiftyjelly.pocketcasts.podcasts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvYourPodcastsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val subscribed = MutableSharedFlow<List<Podcast>>(replay = 1)
    private var homeFolder: List<FolderItem> = emptyList()
    private val podcastManager = mock<PodcastManager> {
        on { findSubscribedFlow() } doReturn subscribed
    }
    private val folderManager = mock<FolderManager> {
        on { observeFolders() } doReturn flowOf(emptyList())
    }

    @Test
    fun `no podcasts or folders map to the empty state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvYourPodcastsUiState.Loading, awaitItem())

            homeFolder = emptyList()
            subscribed.emit(emptyList())

            assertEquals(TvYourPodcastsUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `folders and podcasts are exposed sorted by title`() = runTest {
        val viewModel = createViewModel()
        val zebra = folderItem("Zebra Cast")
        val apple = folderItem("Apple Cast")
        val theBeat = folder("The Beat")
        homeFolder = listOf(zebra, apple, theBeat)

        viewModel.uiState.test {
            assertEquals(TvYourPodcastsUiState.Loading, awaitItem())

            subscribed.emit(listOf(Podcast(uuid = "any")))

            assertEquals(TvYourPodcastsUiState.Loaded(listOf(apple, theBeat, zebra)), awaitItem())
        }
    }

    private suspend fun createViewModel(): TvYourPodcastsViewModel {
        whenever(folderManager.getHomeFolder()).thenAnswer { homeFolder }
        return TvYourPodcastsViewModel(
            podcastManager = podcastManager,
            folderManager = folderManager,
            defaultDispatcher = coroutineRule.testDispatcher,
        )
    }

    private fun folderItem(title: String) = FolderItem.Podcast(Podcast(uuid = title, title = title))

    private fun folder(name: String) = FolderItem.Folder(
        folder = Folder(
            uuid = name,
            name = name,
            color = 0,
            addedDate = Date(0),
            sortPosition = 0,
            podcastsSortType = PodcastsSortType.NAME_A_TO_Z,
            deleted = false,
            syncModified = 0,
        ),
        podcasts = emptyList(),
    )
}
