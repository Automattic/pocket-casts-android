package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolderDetails
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SuggestedFoldersManager
import au.com.shiftyjelly.pocketcasts.utils.UUIDProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SuggestedFoldersViewModel @Inject constructor(
    private val folderManager: FolderManager,
    private val suggestedFoldersManager: SuggestedFoldersManager,
    private val suggestedFoldersPopupPolicy: SuggestedFoldersPopupPolicy,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
    private val uuidProvider: UUIDProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<FoldersState>(FoldersState.Idle)
    val state: StateFlow<FoldersState> = _state

    fun onReplaceExistingFoldersShown() {
        _state.value = FoldersState.Idle
    }

    fun onUseTheseFolders(folders: List<Folder>) {
        viewModelScope.launch {
            val currentFoldersCount = folderManager.countFolders()
            if (currentFoldersCount > 0) {
                _state.value = FoldersState.ShowConfirmationDialog
            } else {
                overrideFoldersWithSuggested(folders)
            }
        }
    }

    fun overrideFoldersWithSuggested(folders: List<Folder>) {
        _state.value = FoldersState.Creating
        viewModelScope.launch {
            val newFolders = folders.map {
                SuggestedFolderDetails(
                    uuid = uuidProvider.generateUUID().toString(),
                    name = it.name,
                    color = it.color,
                    podcastsSortType = settings.podcastsSortType.value,
                    podcasts = it.podcasts,
                )
            }
            settings.podcastsSortType.set(PodcastsSortType.NAME_A_TO_Z, updateModifiedAt = true)
            folderManager.overrideFoldersWithSuggested(newFolders)
            podcastManager.refreshPodcasts("suggested-folders")
            suggestedFoldersManager.replaceSuggestedFolders(folders.toSuggestedFolders())
            _state.value = FoldersState.Created
        }
    }

    fun markPopupAsDismissed() {
        suggestedFoldersPopupPolicy.markPolicyUsed()
    }

    sealed class FoldersState {
        data object Idle : FoldersState()
        data object Creating : FoldersState()
        data object Created : FoldersState()
        data object ShowConfirmationDialog : FoldersState()
    }
}
