package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor(
    settings: Settings,
    userEpisodeManager: UserEpisodeManager,
) : ViewModel() {

    val userEpisodes = userEpisodeManager
        .observeUserEpisodesSorted(settings.getCloudSortOrder())
        .asFlow()
}
