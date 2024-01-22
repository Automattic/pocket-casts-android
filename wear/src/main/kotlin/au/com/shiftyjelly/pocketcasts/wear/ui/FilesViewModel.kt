package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class FilesViewModel @Inject constructor(
    settings: Settings,
    userEpisodeManager: UserEpisodeManager,
) : ViewModel() {

    val userEpisodes = userEpisodeManager
        .observeUserEpisodesSorted(settings.getCloudSortOrder())
        .asFlow()
}
