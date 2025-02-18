package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolderDetails
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.utils.UUIDProvider
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
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

    lateinit var viewModel: SuggestedFoldersViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val mockedPodcastsSortType = mock<UserSetting<PodcastsSortType>>()
        whenever(mockedPodcastsSortType.value).thenReturn(PodcastsSortType.NAME_A_TO_Z)
        whenever(settings.podcastsSortType).thenReturn(mockedPodcastsSortType)
        whenever(uuidProvider.generateUUID()).thenReturn("uuid")

        viewModel = SuggestedFoldersViewModel(folderManager, settings, analyticsTracker, uuidProvider)
    }

    @Test
    fun shouldCreateFolders() = runBlocking {
        val suggestedFolders = listOf(
            Folder("Tech Podcasts", listOf("podcastuuid1"), 1),
        )

        viewModel.onUseTheseFolders(suggestedFolders)

        val newFolders = suggestedFolders.map {
            SuggestedFolderDetails(
                uuid = uuidProvider.generateUUID(),
                name = it.name,
                color = it.color,
                podcastsSortType = settings.podcastsSortType.value,
                podcasts = it.podcasts,
            )
        }

        verify(folderManager).createFolders(newFolders)
    }
}
