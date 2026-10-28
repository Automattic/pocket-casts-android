package au.com.shiftyjelly.pocketcasts.views.helper

import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.views.dialog.HiltShimActivity
import au.com.shiftyjelly.pocketcasts.views.dialog.HiltShimApplication
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltShimApplication::class)
class CloudDeleteHelperTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val downloadQueue = mock<DownloadQueue>()
    private val playbackManager = mock<PlaybackManager>()
    private val userEpisodeManager = mock<UserEpisodeManager>()

    @Test
    fun `delete state follows download and upload status`() {
        assertEquals(DeleteState.Everywhere, CloudDeleteHelper.getDeleteState(isDownloaded = true, isBoth = true))
        assertEquals(DeleteState.DeviceOnly, CloudDeleteHelper.getDeleteState(isDownloaded = true, isBoth = false))
        assertEquals(DeleteState.Cloud, CloudDeleteHelper.getDeleteState(isDownloaded = false, isBoth = false))
        assertEquals(DeleteState.Everywhere, CloudDeleteHelper.getDeleteState(userEpisode(downloaded = true, uploaded = true)))
        assertEquals(DeleteState.DeviceOnly, CloudDeleteHelper.getDeleteState(userEpisode(downloaded = true, uploaded = false)))
        assertEquals(DeleteState.Cloud, CloudDeleteHelper.getDeleteState(userEpisode(downloaded = false, uploaded = true)))
    }

    @Test
    fun `device only delete of an uploaded file only cancels the download`() {
        val episode = userEpisode(downloaded = true, uploaded = true)

        delete(episode, DeleteState.DeviceOnly)

        verify(downloadQueue).cancel(episode.uuid, SourceView.FILES)
        verifyNoInteractions(userEpisodeManager)
    }

    @Test
    fun `device only delete of a local file deletes it`() {
        val episode = userEpisode(downloaded = true, uploaded = false)

        delete(episode, DeleteState.DeviceOnly)

        verifyBlocking(userEpisodeManager) { delete(episode, playbackManager) }
        verify(userEpisodeManager, never()).removeFromCloud(any())
        verifyNoInteractions(downloadQueue)
    }

    @Test
    fun `cloud delete keeps a downloaded copy`() {
        val episode = userEpisode(downloaded = true, uploaded = true)

        delete(episode, DeleteState.Cloud)

        verify(userEpisodeManager).removeFromCloud(episode)
        verifyBlocking(userEpisodeManager, never()) { delete(any(), any()) }
        verifyNoInteractions(downloadQueue)
    }

    @Test
    fun `cloud delete of a file that is not downloaded removes it entirely`() {
        val episode = userEpisode(downloaded = false, uploaded = true)

        delete(episode, DeleteState.Cloud)

        verify(userEpisodeManager).removeFromCloud(episode)
        verifyBlocking(userEpisodeManager) { delete(episode, playbackManager) }
        verifyNoInteractions(downloadQueue)
    }

    @Test
    fun `everywhere delete removes from cloud and device`() {
        val episode = userEpisode(downloaded = true, uploaded = true)

        delete(episode, DeleteState.Everywhere)

        verify(userEpisodeManager).removeFromCloud(episode)
        verifyBlocking(userEpisodeManager) { delete(episode, playbackManager) }
        verifyNoInteractions(downloadQueue)
    }

    @Test
    fun `everywhere dialog confirms everywhere and offers device only as secondary`() {
        val chosenStates = mutableListOf<DeleteState>()
        val episodes = listOf(userEpisode(downloaded = true, uploaded = true))
        val controller = Robolectric.buildActivity(HiltShimActivity::class.java).setup()
        val fragmentManager = controller.get().supportFragmentManager

        val resources = controller.get().resources

        listOf(LR.string.profile_delete_everywhere, LR.string.profile_delete_from_device_only).forEach { buttonText ->
            val dialog = CloudDeleteHelper.getDeleteDialog(episodes, DeleteState.Everywhere, { _, state -> chosenStates += state }, resources)
            dialog.show(fragmentManager, "delete_warning")
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.onNodeWithText(resources.getString(buttonText)).performClick()
            shadowOf(Looper.getMainLooper()).idle()
        }

        assertEquals(listOf(DeleteState.Everywhere, DeleteState.DeviceOnly), chosenStates)
    }

    @Test
    fun `device only and cloud dialogs confirm their own state without a secondary button`() {
        val episodes = listOf(userEpisode(downloaded = true, uploaded = true))
        val controller = Robolectric.buildActivity(HiltShimActivity::class.java).setup()
        val fragmentManager = controller.get().supportFragmentManager
        val resources = controller.get().resources

        mapOf(
            DeleteState.DeviceOnly to LR.string.profile_delete_from_device,
            DeleteState.Cloud to LR.string.profile_delete_from_cloud,
        ).forEach { (state, buttonText) ->
            val chosenStates = mutableListOf<DeleteState>()
            val dialog = CloudDeleteHelper.getDeleteDialog(episodes, state, { _, chosen -> chosenStates += chosen }, resources)
            dialog.show(fragmentManager, "delete_warning")
            shadowOf(Looper.getMainLooper()).idle()

            composeRule.onNodeWithText(resources.getString(LR.string.profile_delete_from_device_only)).assertDoesNotExist()
            composeRule.onNodeWithText(resources.getString(buttonText)).performClick()
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(listOf(state), chosenStates)
        }
    }

    private fun delete(episode: UserEpisode, state: DeleteState) {
        CloudDeleteHelper.deleteEpisode(
            episode = episode,
            sourceView = SourceView.FILES,
            downloadQueue = downloadQueue,
            playbackManager = playbackManager,
            userEpisodeManager = userEpisodeManager,
            applicationScope = CoroutineScope(Dispatchers.Unconfined),
            deleteState = state,
        )
    }

    private fun userEpisode(downloaded: Boolean, uploaded: Boolean) = UserEpisode(
        uuid = "file",
        publishedDate = Date(),
        downloadStatus = if (downloaded) EpisodeDownloadStatus.Downloaded else EpisodeDownloadStatus.DownloadNotRequested,
        serverStatus = if (uploaded) UserEpisodeServerStatus.UPLOADED else UserEpisodeServerStatus.LOCAL,
    )
}
