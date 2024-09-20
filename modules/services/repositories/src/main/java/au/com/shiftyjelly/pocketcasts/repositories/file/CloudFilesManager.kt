package au.com.shiftyjelly.pocketcasts.repositories.file

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.reactive.asFlow

class CloudFilesManager @Inject constructor(
    settings: Settings,
    private val userEpisodeManager: UserEpisodeManager,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val sortedCloudFiles = settings.cloudSortOrder.flow.flatMapLatest { userEpisodeManager.observeUserEpisodesSorted(it).asFlow() }
}
