package au.com.shiftyjelly.pocketcasts.wear.ui.podcasts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem.Folder
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PodcastsViewModel @Inject constructor(
    private val podcastManager: PodcastManager
) : ViewModel() {

    data class UiState(
        val folder: Folder? = null,
        val items: List<FolderItem> = emptyList(),
        val isSignedInAsPlus: Boolean = false
    )

    var uiState by mutableStateOf(UiState())
        private set

    init {
        viewModelScope.launch {
            val podcasts = podcastManager.findPodcastsOrderByTitle().map { FolderItem.Podcast(it) }
            uiState = UiState(folder = null, items = podcasts, isSignedInAsPlus = false)
        }
    }
}
