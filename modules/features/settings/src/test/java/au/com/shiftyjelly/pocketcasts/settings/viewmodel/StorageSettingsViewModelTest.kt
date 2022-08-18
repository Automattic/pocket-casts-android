package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.file.FolderLocation
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Flowable
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

private const val CUSTOM_FOLDER_LABEL = "CustomFolder"
private const val CUSTOM_FOLDER_NEW_PATH = "custom_folder_new_path"
private const val CUSTOM_FOLDER_NEW_LABEL = "CustomFolderNew"

@RunWith(MockitoJUnitRunner::class)
class StorageSettingsViewModelTest {
    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var fileStorage: FileStorage

    @Mock
    private lateinit var fileUtil: FileUtilWrapper

    @Mock
    private lateinit var settings: Settings

    @Mock
    @ApplicationContext
    private lateinit var context: Context
    private lateinit var viewModel: StorageSettingsViewModel

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(context.getString(any())).thenReturn("")
        whenever(context.getString(any(), any())).thenReturn("")
        whenever(settings.getStorageChoiceName()).thenReturn("")
        whenever(episodeManager.observeDownloadedEpisodes()).thenReturn(Flowable.empty())
        viewModel = StorageSettingsViewModel(
            podcastManager,
            episodeManager,
            fileStorage,
            fileUtil,
            settings,
            context
        )
    }

    @Test
    fun `given sdk version less than 29, when fragment resumed, then custom folder choice is added`() {
        startViewModelAndResumeFragment(folderLocations = emptyList(), sdkVersion = 28)

        val (_, folderPaths) = viewModel.state.value.storageChoiceState.choices

        assertEquals(folderPaths.last(), Settings.STORAGE_ON_CUSTOM_FOLDER)
    }

    @Test
    fun `given sdk version at least 29, when fragment resumed, then custom folder choice is not added`() {
        startViewModelAndResumeFragment(folderLocations = emptyList(), sdkVersion = 29)

        val (_, folderPaths) = viewModel.state.value.storageChoiceState.choices

        assertTrue(folderPaths.isEmpty())
    }

    @Test
    fun `given sdk version less than 29, when storage choice is custom folder, then custom folder path is shown`() {
        val folderLocation = FolderLocation(Settings.STORAGE_ON_CUSTOM_FOLDER, CUSTOM_FOLDER_LABEL)
        startViewModelAndResumeFragment(folderLocations = listOf(folderLocation), sdkVersion = 28)
        val file = File(folderLocation.filePath)
        whenever(fileStorage.baseStorageDirectory).thenReturn(file)
        whenever(settings.usingCustomFolderStorage()).thenReturn(true)

        viewModel.state.value.storageChoiceState.onStateChange(folderLocation.filePath)

        verify(settings).setStorageCustomFolder(file.absolutePath)
        assertTrue(viewModel.state.value.storageFolderState.isVisible)
    }

    @Test
    fun `given sdk version less than 29, when storage choice is not custom folder, then custom folder path is not shown`() {
        val folderLocation = FolderLocation("/path", "Phone")
        startViewModelAndResumeFragment(folderLocations = listOf(folderLocation), sdkVersion = 28)
        whenever(settings.usingCustomFolderStorage()).thenReturn(false)
        whenever(settings.getStorageChoice()).thenReturn(folderLocation.filePath)

        viewModel.state.value.storageChoiceState.onStateChange(folderLocation.filePath)

        verify(settings).setStorageChoice(folderLocation.filePath, folderLocation.label)
        assertFalse(viewModel.state.value.storageFolderState.isVisible)
    }

    @Test
    fun `given disk write permission granted, when custom folder is changed, then move podcast alert shown`() {
        val alertDialogResult = mutableListOf<StorageSettingsViewModel.AlertDialogState>()
        testCustomFolderChangeMovePodcastAlert(
            alertDialogResult,
            permissionGranted = true
        ) {
            viewModel.state.value.storageFolderState.onStateChange(CUSTOM_FOLDER_NEW_PATH)

            assertEquals(alertDialogResult.size, 1)
        }
    }

    @Test
    fun `given disk write permission not granted, when custom folder is changed, then move podcast alert not shown`() {
        val alertDialogResult = mutableListOf<StorageSettingsViewModel.AlertDialogState>()
        testCustomFolderChangeMovePodcastAlert(
            alertDialogResult,
            permissionGranted = false
        ) {
            viewModel.state.value.storageFolderState.onStateChange(CUSTOM_FOLDER_NEW_PATH)

            assertEquals(alertDialogResult.size, 0)
        }
    }

    private fun startViewModelAndResumeFragment(
        folderLocations: List<FolderLocation> = emptyList(),
        sdkVersion: Int,
        permissionGranted: Boolean = true,
    ) {
        viewModel.start(
            folderLocations = { folderLocations },
            permissionGranted = { permissionGranted },
            sdkVersion = sdkVersion
        )
        viewModel.onFragmentResume()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun testCustomFolderChangeMovePodcastAlert(
        alertDialogResult: MutableList<StorageSettingsViewModel.AlertDialogState>,
        permissionGranted: Boolean,
        testBody: () -> Unit
    ) = runTest {
        val folderLocation = FolderLocation(Settings.STORAGE_ON_CUSTOM_FOLDER, CUSTOM_FOLDER_LABEL)
        startViewModelAndResumeFragment(
            folderLocations = listOf(folderLocation),
            sdkVersion = 28,
            permissionGranted = permissionGranted
        )
        val oldFile = File(folderLocation.filePath)
        whenever(fileStorage.baseStorageDirectory).thenReturn(oldFile)
        whenever(settings.usingCustomFolderStorage()).thenReturn(true)
        createNewTemporaryCustomFolder()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.alertDialog.toList(alertDialogResult)
        }

        testBody()

        collectJob.cancel()
        temporaryFolder.delete()
    }

    private fun createNewTemporaryCustomFolder() {
        File(temporaryFolder.newFolder(CUSTOM_FOLDER_NEW_PATH), CUSTOM_FOLDER_NEW_LABEL).createNewFile()
    }

    @After
    fun tearDown() {
        temporaryFolder.delete()
    }
}
