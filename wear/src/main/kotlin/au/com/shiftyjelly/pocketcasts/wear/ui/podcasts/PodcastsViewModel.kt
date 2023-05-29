package au.com.shiftyjelly.pocketcasts.wear.ui.podcasts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem.Folder
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PodcastsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val folderManager: FolderManager,
) : ViewModel() {

    private val folderUuid: String = savedStateHandle[PodcastsScreen.argumentFolderUuid] ?: ""

    data class UiState(
        val folder: Folder? = null,
        val items: List<FolderItem> = emptyList(),
        val isSignedInAsPlus: Boolean = false
    )

    var uiState by mutableStateOf(UiState())
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val folder: FolderItem.Folder?
            val items: List<FolderItem>
            if (folderUuid.isEmpty()) {
                items = folderManager.getHomeFolder()
                folder = null
            } else {
                val podcasts = folderManager.findFolderPodcastsSorted(folderUuid)
                items = podcasts.map { FolderItem.Podcast(it) }
                folder = folderManager.findByUuid(folderUuid)?.let { FolderItem.Folder(folder = it, podcasts = podcasts) }
            }
            uiState = UiState(folder = folder, items = items, isSignedInAsPlus = false)
        }
    }
}
