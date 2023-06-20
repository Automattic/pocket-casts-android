package au.com.shiftyjelly.pocketcasts.wear.ui.podcasts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PodcastsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val folderManager: FolderManager,
) : ViewModel() {

    private val folderUuid: String = savedStateHandle[PodcastsScreen.argumentFolderUuid] ?: ""

    sealed class UiState {
        object Empty : UiState()
        object Loading : UiState()
        data class Loaded(
            val folder: Folder? = null,
            val items: List<FolderItem> = emptyList()
        ) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val folder: Folder?
            val items: List<FolderItem>
            if (folderUuid.isEmpty()) {
                items = folderManager.getHomeFolder()
                folder = null
            } else {
                val podcasts = folderManager.findFolderPodcastsSorted(folderUuid)
                items = podcasts.map { FolderItem.Podcast(it) }
                folder = folderManager.findByUuid(folderUuid)
            }
            _uiState.value = if (items.isNotEmpty()) {
                UiState.Loaded(folder = folder, items = items)
            } else {
                UiState.Empty
            }
        }
    }
}
