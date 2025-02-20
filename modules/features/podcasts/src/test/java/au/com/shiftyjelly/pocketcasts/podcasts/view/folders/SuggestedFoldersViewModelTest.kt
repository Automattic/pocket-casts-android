package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolderDetails
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SuggestedFoldersManager
import au.com.shiftyjelly.pocketcasts.utils.UUIDProvider
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SuggestedFoldersViewModelTest {

    @Mock
    lateinit var folderManager: FolderManager

    @Mock
    lateinit var settings: Settings

    @Mock
    lateinit var uuidProvider: UUIDProvider

    @Mock
    lateinit var analyticsTracker: AnalyticsTracker

    @Mock
    lateinit var suggestedFoldersManager: SuggestedFoldersManager

    lateinit var viewModel: SuggestedFoldersViewModel

    private var mockedUUID = UUID.randomUUID()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val mockedPodcastsSortType = mock<UserSetting<PodcastsSortType>>()
        whenever(mockedPodcastsSortType.value).thenReturn(PodcastsSortType.NAME_A_TO_Z)
        whenever(settings.podcastsSortType).thenReturn(mockedPodcastsSortType)
        whenever(uuidProvider.generateUUID()).thenReturn(mockedUUID)

        viewModel = SuggestedFoldersViewModel(folderManager, suggestedFoldersManager, settings, analyticsTracker, uuidProvider)
    }

    @Ignore("This test is flaky and needs to be fixed")
    @Test
    fun shouldCreateFolders() = runBlocking {
        val folders = listOf(
            Folder("Tech Podcasts", listOf("podcastuuid1"), 1),
        )

        viewModel.onUseTheseFolders(folders)

        val newFolders: List<SuggestedFolderDetails> = folders.map {
            SuggestedFolderDetails(
                uuid = mockedUUID.toString(),
                name = it.name,
                color = it.color,
                podcastsSortType = settings.podcastsSortType.value,
                podcasts = it.podcasts,
            )
        }

        verify(folderManager).createFolders(newFolders)
        verify(suggestedFoldersManager).deleteSuggestedFolders(any())
    }
}
