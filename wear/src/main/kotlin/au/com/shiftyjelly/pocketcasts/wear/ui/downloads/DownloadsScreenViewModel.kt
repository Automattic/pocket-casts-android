package au.com.shiftyjelly.pocketcasts.wear.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class DownloadsScreenViewModel @Inject constructor(
    episodeManager: EpisodeManager,
    settings: Settings,
) : ViewModel() {

    val stateFlow = episodeManager.observeDownloadEpisodes()
        .asFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val useRssArtwork = settings.useRssArtwork.flow
}
