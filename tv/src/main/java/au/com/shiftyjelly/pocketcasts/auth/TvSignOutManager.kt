package au.com.shiftyjelly.pocketcasts.auth

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.Lazy
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class TvSignOutManager @Inject constructor(
    private val userManager: UserManager,
    private val playbackManager: Lazy<PlaybackManager>,
    private val upNextQueue: Lazy<UpNextQueue>,
    private val folderManager: Lazy<FolderManager>,
    private val searchHistoryManager: Lazy<SearchHistoryManager>,
    private val episodeManager: Lazy<EpisodeManager>,
    private val fileStorage: FileStorage,
    private val settings: Settings,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun signOutAndWipeData() {
        applicationScope.launch(ioDispatcher) {
            val signOutJob = runWipeStep("sign out and clear data") {
                userManager.signOutAndClearData(
                    playbackManager = playbackManager.get(),
                    upNextQueue = upNextQueue.get(),
                    folderManager = folderManager.get(),
                    searchHistoryManager = searchHistoryManager.get(),
                    episodeManager = episodeManager.get(),
                    wasInitiatedByUser = true,
                )
            }
            if (signOutJob != null && withTimeoutOrNull(SIGN_OUT_TIMEOUT) { signOutJob.join() } == null) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "TV sign out did not finish before the wipe timeout")
            }
            runWipeStep("delete downloaded files") { deleteDownloadedFiles() }
            runWipeStep("clear user preferences") { settings.clearUserPreferences() }
        }
    }

    private fun deleteDownloadedFiles() {
        val dirs = listOfNotNull(
            fileStorage.getOrCreateEpisodesDir(),
            fileStorage.getOrCreateCloudDir(),
            fileStorage.getOrCreateNetworkImagesDir(),
            fileStorage.getOrCreateEpisodesOldTempDir(),
            fileStorage.getOrCreateEpisodesTempDir(),
        )
        dirs.forEach { dir ->
            dir.listFiles()?.forEach { file ->
                if (!file.name.equals(".nomedia", ignoreCase = true)) {
                    file.deleteRecursively()
                }
            }
        }
    }

    private inline fun <T> runWipeStep(name: String, block: () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "TV sign out step failed: $name")
        null
    }

    private companion object {
        val SIGN_OUT_TIMEOUT = 10.seconds
    }
}
