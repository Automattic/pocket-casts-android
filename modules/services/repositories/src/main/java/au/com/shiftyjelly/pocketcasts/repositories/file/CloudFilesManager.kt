package au.com.shiftyjelly.pocketcasts.repositories.file

import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.BackpressureStrategy
import javax.inject.Inject

class CloudFilesManager @Inject constructor(
    private val settings: Settings,
    private val userEpisodeManager: UserEpisodeManager,
) {
    val sortOrderRelay = BehaviorRelay.create<Settings.CloudSortOrder>().apply { accept(settings.getCloudSortOrder()) }
    val sortedCloudFiles = sortOrderRelay.toFlowable(BackpressureStrategy.LATEST).switchMap { userEpisodeManager.observeUserEpisodesSorted(it) }
    val cloudFilesList = sortedCloudFiles.toLiveData()
}
