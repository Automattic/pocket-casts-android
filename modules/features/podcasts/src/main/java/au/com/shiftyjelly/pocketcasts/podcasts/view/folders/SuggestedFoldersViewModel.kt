package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.repositories.notification.NewFeaturesAndTipsNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SuggestedFoldersManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel(assistedFactory = SuggestedFoldersViewModel.Factory::class)
class SuggestedFoldersViewModel @AssistedInject constructor(
    @Assisted private val source: SuggestedFoldersFragment.Source,
    private val folderManager: FolderManager,
    private val suggestedFoldersManager: SuggestedFoldersManager,
    private val suggestedFoldersPopupPolicy: SuggestedFoldersPopupPolicy,
    private val podcastManager: PodcastManager,
    private val userManager: UserManager,
    private val analyticsTracker: AnalyticsTracker,
    private val notificationManager: NotificationManager,
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
                    value.copy(signInState = signInState)
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

    fun trackPageShown() {
        analyticsTracker.track(
            AnalyticsEvent.SUGGESTED_FOLDERS_PAGE_SHOWN,
            mapOf(
                "source" to source.analyticsValue,
            ),
        )
    }

    fun trackPageDismissed() {
        analyticsTracker.track(
            AnalyticsEvent.SUGGESTED_FOLDERS_PAGE_DISMISSED,
            mapOf(
                "source" to source.analyticsValue,
                "user_type" to state.value.userTypeAnalyticsValue,
            ),
        )
    }

    fun trackUseSuggestedFoldersTapped() {
        analyticsTracker.track(
            AnalyticsEvent.SUGGESTED_FOLDERS_USE_SUGGESTED_FOLDERS_TAPPED,
            mapOf(
                "source" to source.analyticsValue,
                "user_type" to state.value.userTypeAnalyticsValue,
            ),
        )
    }

    fun trackCreateCustomFolderTapped() {
        analyticsTracker.track(
            AnalyticsEvent.SUGGESTED_FOLDERS_CREATE_CUSTOM_FOLDER_TAPPED,
            mapOf(
                "source" to source.analyticsValue,
                "user_type" to state.value.userTypeAnalyticsValue,
            ),
        )
    }

    fun trackReplaceFolderTapped() {
        analyticsTracker.track(
            AnalyticsEvent.SUGGESTED_FOLDERS_REPLACE_FOLDERS_TAPPED,
            mapOf(
                "source" to source.analyticsValue,
            ),
        )
    }

    fun trackReplaceFoldersConfirmationTapped() {
        analyticsTracker.track(
            AnalyticsEvent.SUGGESTED_FOLDERS_REPLACE_FOLDERS_CONFIRM_TAPPED,
            mapOf(
                "source" to source.analyticsValue,
            ),
        )
    }

    fun trackPreviewFolderTapped(folder: SuggestedFolder) {
        analyticsTracker.track(
            AnalyticsEvent.SUGGESTED_FOLDERS_PREVIEW_FOLDER_TAPPED,
            mapOf(
                "source" to source.analyticsValue,
                "folder_name" to folder.name,
                "podcast_count" to folder.podcastIds.size,
            ),
        )
    }

    fun registerFeatureInteraction() = viewModelScope.launch {
        notificationManager.updateUserFeatureInteraction(NewFeaturesAndTipsNotificationType.SmartFolders)
    }

    data class State(
        val signInState: SignInState,
        val existingFoldersCount: Int?,
        val suggestedFolders: List<SuggestedFolder>,
        val useFoldersState: UseFoldersState,
    ) {
        val action
            get() = if (signInState.isSignedInAsPlusOrPatron) {
                when (existingFoldersCount) {
                    null -> null
                    0 -> SuggestedAction.UseFolders
                    else -> SuggestedAction.ReplaceFolders
                }
            } else {
                SuggestedAction.UseFolders
            }

        val userTypeAnalyticsValue
            get() = when (signInState) {
                is SignInState.SignedOut -> "unsigned"
                is SignInState.SignedIn -> if (signInState.subscription == null) "free" else "paid"
            }

        companion object {
            val Empty = State(
                signInState = SignInState.SignedOut,
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

    @AssistedFactory
    interface Factory {
        fun crate(source: SuggestedFoldersFragment.Source): SuggestedFoldersViewModel
    }
}
