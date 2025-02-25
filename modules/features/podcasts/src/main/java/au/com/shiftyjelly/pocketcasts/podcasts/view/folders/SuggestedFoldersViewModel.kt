package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolderDetails
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
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
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
    private val uuidProvider: UUIDProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<FoldersState>(FoldersState.Idle)
    val state: StateFlow<FoldersState> = _state

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_MODAL_SHOWN)
    }

    fun onDismissed() {
        analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_MODAL_DISMISSED)
    }

    fun onUseTheseFolders(folders: List<Folder>) {
        analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_MODAL_USE_THESE_FOLDERS_TAPPED)
        viewModelScope.launch {
            val currentFoldersCount = folderManager.countFolders()
            if (currentFoldersCount > 0) {
                _state.value = FoldersState.ShowConfirmationDialog
            } else {
                overrideFoldersWithSuggested(folders)
            }
        }
    }

    fun onCreateCustomFolders() {
        analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_MODAL_CREATE_CUSTOM_FOLDERS_TAPPED)
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

            folderManager.overrideFoldersWithSuggested(newFolders)
            suggestedFoldersManager.deleteSuggestedFolders(folders.toSuggestedFolders())
            _state.value = FoldersState.Created
        }
    }

    sealed class FoldersState {
        data object Idle : FoldersState()
        data object Creating : FoldersState()
        data object Created : FoldersState()
        data object ShowConfirmationDialog : FoldersState()
    }
}
