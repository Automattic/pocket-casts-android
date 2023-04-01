package au.com.shiftyjelly.pocketcasts.wear.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@HiltViewModel
class DownloadsScreenViewModel @Inject constructor(
    episodeManager: EpisodeManager,
) : ViewModel() {

    val stateFlow = episodeManager.observeDownloadEpisodes()
        .asFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
}
