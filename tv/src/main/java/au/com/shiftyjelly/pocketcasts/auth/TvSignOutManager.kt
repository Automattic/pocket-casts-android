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
import au.com.shiftyjelly.pocketcasts.ui.images.CoilManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import dagger.Lazy
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
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
    private val coilManager: CoilManager,
    private val settings: Settings,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun signOutAndWipeData() {
        applicationScope.launch(ioDispatcher) {
            val signOutJob = userManager.signOutAndClearData(
                playbackManager = playbackManager.get(),
                upNextQueue = upNextQueue.get(),
                folderManager = folderManager.get(),
                searchHistoryManager = searchHistoryManager.get(),
                episodeManager = episodeManager.get(),
                wasInitiatedByUser = true,
            )
            withTimeoutOrNull(SIGN_OUT_TIMEOUT) { signOutJob?.join() }
            deleteDownloadedFiles()
            coilManager.clearAll()
            settings.clearUserPreferences()
        }
    }

    private fun deleteDownloadedFiles() {
        val dirs = listOfNotNull(
            fileStorage.getOrCreateEpisodesDir(),
            fileStorage.getOrCreateCloudDir(),
            fileStorage.getOrCreateNetworkImagesDir(),
            fileStorage.getOrCreateEpisodesTempDir(),
        )
        dirs.forEach { dir -> FileUtil.deleteDirContents(dir.absolutePath) }
    }

    private companion object {
        val SIGN_OUT_TIMEOUT = 10.seconds
    }
}
