package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    private val settings: Settings,
    private val feature: FeatureWrapper
) : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state = _state.asStateFlow()

    private val _navigationState: MutableSharedFlow<NavigationState> = MutableSharedFlow()
    val navigationState = _navigationState.asSharedFlow()

    init {
        val isBookmarksEnabled = FeatureFlag.isEnabled(feature.bookmarksFeature)

        _state.value = UiState.Loaded(
            feature = if (isBookmarksEnabled) bookmarksFeature() else WhatsNewFeature.AutoPlay,
            tier = if (isBookmarksEnabled) bookmarksTier() else UserTier.Free,
        )
    }

    private fun bookmarksFeature(): WhatsNewFeature.Bookmarks {
        val isBookmarksCurrentlyExclusiveToPatron = feature.bookmarksFeature.isCurrentlyExclusiveToPatron()
        val isUserEntitledForBookmarks = feature.isUserEntitled(feature.bookmarksFeature, settings.userTier)
        val showJoinBeta = isBookmarksCurrentlyExclusiveToPatron && !isUserEntitledForBookmarks

        return WhatsNewFeature.Bookmarks(
            title = if (showJoinBeta) {
                LR.string.whats_new_boomarks_join_beta_testing_title
            } else {
                LR.string.whats_new_bookmarks_title
            },
            message = LR.string.whats_new_bookmarks_body,
        )
    }

    private fun bookmarksTier() = settings.userTier

    fun onConfirm() {
        viewModelScope.launch {
            val currentState = state.value as? UiState.Loaded ?: return@launch
            val target = when (currentState.feature) {
                is WhatsNewFeature.AutoPlay -> NavigationState.PlaybackSettings
                is WhatsNewFeature.Bookmarks -> NavigationState.HeadphoneControlsSettings
            }
            _navigationState.emit(target)
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Loaded(
            val feature: WhatsNewFeature,
            val tier: UserTier,
        ) : UiState()
    }

    sealed class WhatsNewFeature(
        @StringRes open val title: Int,
        @StringRes open val message: Int,
        @StringRes val confirmButtonTitle: Int,
        @StringRes val closeButtonTitle: Int? = null,
    ) {
        object AutoPlay : WhatsNewFeature(
            title = LR.string.whats_new_autoplay_title,
            message = LR.string.whats_new_autoplay_body,
            confirmButtonTitle = LR.string.whats_new_autoplay_enable_button,
            closeButtonTitle = LR.string.whats_new_autoplay_maybe_later_button,
        )

        data class Bookmarks(
            @StringRes override val title: Int,
            @StringRes override val message: Int,
        ) : WhatsNewFeature(
            title = title,
            message = message,
            confirmButtonTitle = LR.string.whats_new_bookmarks_try_now_button,
        )
    }

    sealed class NavigationState {
        object PlaybackSettings : NavigationState()
        object HeadphoneControlsSettings : NavigationState()
    }
}
