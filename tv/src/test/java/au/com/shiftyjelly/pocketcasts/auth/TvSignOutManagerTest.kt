package au.com.shiftyjelly.pocketcasts.auth

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.ui.images.CoilManager
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvSignOutManagerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val userManager = mock<UserManager>()
    private val playbackManager = mock<PlaybackManager>()
    private val upNextQueue = mock<UpNextQueue>()
    private val folderManager = mock<FolderManager>()
    private val searchHistoryManager = mock<SearchHistoryManager>()
    private val episodeManager = mock<EpisodeManager>()
    private val coilManager = mock<CoilManager>()
    private val settings = mock<Settings>()

    @Test
    fun `sign out clears the account data and caches in order`() = runTest {
        val manager = createManager(fileStorage = mock<FileStorage>())

        manager.signOutAndWipeData()
        advanceUntilIdle()

        inOrder(userManager, coilManager, settings) {
            verify(userManager).signOutAndClearData(
                playbackManager = eq(playbackManager),
                upNextQueue = eq(upNextQueue),
                folderManager = eq(folderManager),
                searchHistoryManager = eq(searchHistoryManager),
                episodeManager = eq(episodeManager),
                wasInitiatedByUser = eq(true),
            )
            verify(coilManager).clearAll()
            verify(settings).clearUserPreferences()
        }
    }

    @Test
    fun `sign out deletes the downloaded files`() = runTest {
        val episodesDir = temporaryFolder.newFolder("episodes")
        val cloudDir = temporaryFolder.newFolder("cloud_files")
        val downloadedEpisode = File(episodesDir, "episode.mp3").apply { writeText("audio") }
        val cloudFile = File(cloudDir, "file.mp3").apply { writeText("audio") }
        val fileStorage = mock<FileStorage> {
            on { getOrCreateEpisodesDir() } doReturn episodesDir
            on { getOrCreateCloudDir() } doReturn cloudDir
        }
        val manager = createManager(fileStorage = fileStorage)

        manager.signOutAndWipeData()
        advanceUntilIdle()

        assertFalse(downloadedEpisode.exists())
        assertFalse(cloudFile.exists())
        assertTrue(episodesDir.exists())
    }

    @Test
    fun `wipe waits for the pending sign out before clearing caches`() = runTest {
        val signOutJob = Job()
        whenever(
            userManager.signOutAndClearData(any(), any(), any(), any(), any(), any()),
        ).thenReturn(signOutJob)
        val manager = createManager(fileStorage = mock<FileStorage>())

        manager.signOutAndWipeData()
        runCurrent()

        verify(coilManager, never()).clearAll()
        verify(settings, never()).clearUserPreferences()

        signOutJob.complete()
        runCurrent()

        verify(coilManager).clearAll()
        verify(settings).clearUserPreferences()
    }

    @Test
    fun `sign out ignores unavailable storage directories`() = runTest {
        val manager = createManager(fileStorage = mock<FileStorage>())

        manager.signOutAndWipeData()
        advanceUntilIdle()

        verify(settings).clearUserPreferences()
    }

    private fun kotlinx.coroutines.test.TestScope.createManager(fileStorage: FileStorage) = TvSignOutManager(
        userManager = userManager,
        playbackManager = { playbackManager },
        upNextQueue = { upNextQueue },
        folderManager = { folderManager },
        searchHistoryManager = { searchHistoryManager },
        episodeManager = { episodeManager },
        fileStorage = fileStorage,
        coilManager = coilManager,
        settings = settings,
        applicationScope = this,
        ioDispatcher = coroutineRule.testDispatcher,
    )
}
