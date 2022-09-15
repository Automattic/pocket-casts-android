package au.com.shiftyjelly.pocketcasts.wear.ui.podcast

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PodcastViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podcastManager: PodcastManager
) : ViewModel() {

    private val podcastUuid: String = savedStateHandle[PodcastScreen.argument] ?: ""

    data class UiState(
        val podcast: Podcast? = null
    )

    var uiState by mutableStateOf(UiState())
        private set

    init {
        viewModelScope.launch {
            val podcast = podcastManager.findPodcastByUuidSuspend(podcastUuid)
            uiState = UiState(podcast = podcast)
        }
    }
}
