package au.com.shiftyjelly.pocketcasts.wear.ui.starred

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.StarredSyncWorker
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.wear.ui.component.EpisodeListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StarredScreenViewModel @Inject constructor(
    episodeManager: EpisodeManager,
    settings: Settings,
    val syncManager: SyncManager,
) : ViewModel() {

    val stateFlow: StateFlow<EpisodeListUiState> = episodeManager.findStarredEpisodesFlow().map { episodes ->
        if (episodes.isEmpty()) {
            EpisodeListUiState.Empty
        } else {
            EpisodeListUiState.Loaded(episodes)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = EpisodeListUiState.Loading,
    )

    val artworkConfiguration = settings.artworkConfiguration.flow

    fun refresh(context: Context) {
        StarredSyncWorker.enqueue(
            syncManager = syncManager,
            context = context,
        )
    }
}
