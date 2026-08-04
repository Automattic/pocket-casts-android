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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TvYourPodcastsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val subscribed = MutableSharedFlow<List<Podcast>>(replay = 1)
    private val folders = MutableSharedFlow<List<Folder>>(replay = 1)
    private var homeFolder: List<FolderItem> = emptyList()
    private var folderPodcasts: Map<String, List<Podcast>> = emptyMap()
    private val podcastManager = mock<PodcastManager> {
        on { findSubscribedFlow() } doReturn subscribed
    }
    private val folderManager = mock<FolderManager> {
        on { observeFolders() } doReturn folders
        on { getHomeFolder() } doAnswer { homeFolder }
        on { findFolderPodcastsSorted(any()) } doAnswer { invocation ->
            folderPodcasts[invocation.getArgument<String>(0)].orEmpty()
        }
    }

    @Test
    fun `no podcasts or folders map to the empty state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvYourPodcastsUiState.Loading, awaitItem())

            folders.emit(emptyList())
            subscribed.emit(emptyList())

            assertEquals(TvYourPodcastsUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `folders and podcasts are exposed sorted by title`() = runTest {
        val viewModel = createViewModel()
        val zebra = podcastItem("Zebra Cast")
        val apple = podcastItem("Apple Cast")
        val theBeat = folderItem("The Beat")
        homeFolder = listOf(zebra, apple, theBeat)

        viewModel.uiState.test {
            assertEquals(TvYourPodcastsUiState.Loading, awaitItem())

            folders.emit(emptyList())
            subscribed.emit(listOf(Podcast(uuid = "any")))

            assertEquals(TvYourPodcastsUiState.Loaded(listOf(apple, theBeat, zebra)), awaitItem())
        }
    }

    @Test
    fun `the grid is re-queried when the folders change`() = runTest {
        val viewModel = createViewModel()
        val apple = podcastItem("Apple Cast")
        homeFolder = listOf(apple)

        viewModel.uiState.test {
            assertEquals(TvYourPodcastsUiState.Loading, awaitItem())

            folders.emit(emptyList())
            subscribed.emit(emptyList())
            assertEquals(TvYourPodcastsUiState.Loaded(listOf(apple)), awaitItem())

            val beat = podcastItem("Beat Cast")
            homeFolder = listOf(apple, beat)
            folders.emit(listOf(folderEntity("New Folder")))

            assertEquals(TvYourPodcastsUiState.Loaded(listOf(apple, beat)), awaitItem())
        }
    }

    @Test
    fun `folder items are enriched with their cover podcasts`() = runTest {
        val viewModel = createViewModel()
        val tech = folderItem("Tech")
        homeFolder = listOf(tech)
        folderPodcasts = mapOf("Tech" to listOf(Podcast(uuid = "p1"), Podcast(uuid = "p2")))

        viewModel.uiState.test {
            assertEquals(TvYourPodcastsUiState.Loading, awaitItem())

            folders.emit(emptyList())
            subscribed.emit(emptyList())

            val loaded = awaitItem() as TvYourPodcastsUiState.Loaded
            val folder = loaded.items.single() as FolderItem.Folder
            assertEquals(listOf("p1", "p2"), folder.podcasts.map(Podcast::uuid))
        }
    }

    private fun createViewModel() = TvYourPodcastsViewModel(
        podcastManager = podcastManager,
        folderManager = folderManager,
        defaultDispatcher = coroutineRule.testDispatcher,
    )

    private fun podcastItem(title: String) = FolderItem.Podcast(Podcast(uuid = title, title = title))

    private fun folderItem(name: String) = FolderItem.Folder(
        folder = folderEntity(name),
        podcasts = emptyList(),
    )

    private fun folderEntity(name: String) = Folder(
        uuid = name,
        name = name,
        color = 0,
        addedDate = Date(0),
        sortPosition = 0,
        podcastsSortType = PodcastsSortType.NAME_A_TO_Z,
        deleted = false,
        syncModified = 0,
    )
}
