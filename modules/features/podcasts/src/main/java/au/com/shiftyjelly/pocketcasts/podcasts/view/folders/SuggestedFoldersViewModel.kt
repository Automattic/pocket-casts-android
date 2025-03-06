package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SuggestedFoldersManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder as DbSuggestedFolder

@HiltViewModel
class SuggestedFoldersViewModel @Inject constructor(
    private val folderManager: FolderManager,
    private val suggestedFoldersManager: SuggestedFoldersManager,
    private val suggestedFoldersPopupPolicy: SuggestedFoldersPopupPolicy,
    private val podcastManager: PodcastManager,
    private val userManager: UserManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _state = MutableStateFlow(State.Empty)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val folderCount = folderManager.countFolders()
            val suggestedFolders = getSuggestedFolders()
            _state.update { value ->
                value.copy(
                    existingFoldersCount = folderCount,
                    suggestedFolders = suggestedFolders,
                )
            }
        }
        viewModelScope.launch {
            userManager.getSignInState().asFlow().collect { signInState ->
                _state.update { value ->
                    value.copy(isUserPlusOrPatreon = signInState.isSignedInAsPlusOrPatron)
                }
            }
        }
    }

    private suspend fun getSuggestedFolders(): List<SuggestedFolder> {
        val dbFolders = suggestedFoldersManager.observeSuggestedFolders().first()
        return withContext(Dispatchers.Default) {
            val grouped = dbFolders.groupBy(DbSuggestedFolder::name).toList().sortedBy { it.first }
            grouped.mapIndexed { index, (name, folders) ->
                SuggestedFolder(
                    name = name,
                    colorIndex = index % 12,
                    podcastIds = folders.map { it.podcastUuid },
                )
            }
        }
    }

    private var suggestedFoldersJob: Job? = null

    fun useSuggestedFolders() {
        if (suggestedFoldersJob?.isActive == true) {
            return
        }
        viewModelScope.launch(NonCancellable) {
            _state.update { value ->
                value.copy(useFoldersState = UseFoldersState.Applying)
            }
            val suggestedFolders = withContext(Dispatchers.Default) {
                state.value.suggestedFolders.flatMap { folder ->
                    folder.podcastIds.map { podcastId ->
                        DbSuggestedFolder(folder.name, podcastId)
                    }
                }
            }
            suggestedFoldersManager.useSuggestedFolders(suggestedFolders)
            _state.update { value ->
                value.copy(useFoldersState = UseFoldersState.Applied)
            }
            podcastManager.refreshPodcasts("suggested-folders")
        }
    }

    fun markPopupAsDismissed() {
        suggestedFoldersPopupPolicy.markPolicyUsed()
    }

    data class State(
        val isUserPlusOrPatreon: Boolean,
        val existingFoldersCount: Int?,
        val suggestedFolders: List<SuggestedFolder>,
        val useFoldersState: UseFoldersState,
    ) {
        val action
            get() = if (isUserPlusOrPatreon) {
                when (existingFoldersCount) {
                    null -> null
                    0 -> SuggestedAction.UseFolders
                    else -> SuggestedAction.ReplaceFolders
                }
            } else {
                SuggestedAction.UseFolders
            }

        companion object {
            val Empty = State(
                isUserPlusOrPatreon = false,
                existingFoldersCount = null,
                suggestedFolders = emptyList(),
                useFoldersState = UseFoldersState.Idle,
            )
        }
    }

    enum class UseFoldersState {
        Idle,
        Applying,
        Applied,
    }
}
