package au.com.shiftyjelly.pocketcasts.repositories.file

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest

class CloudFilesManager @Inject constructor(
    settings: Settings,
    private val userEpisodeManager: UserEpisodeManager,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val sortedCloudFiles = settings.cloudSortOrder.flow.flatMapLatest { userEpisodeManager.userEpisodesSortedFlow(it) }
}
