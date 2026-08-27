package au.com.shiftyjelly.pocketcasts.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.automattic.eventhorizon.AccountDetailsShowPrivacyPolicyEvent
import com.automattic.eventhorizon.AccountDetailsShowTosEvent
import com.automattic.eventhorizon.AccountDetailsSubscriptionEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsGeneralShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.rx2.asFlow

@HiltViewModel
class TvSettingsViewModel @Inject constructor(
    private val settings: Settings,
    private val syncManager: SyncManager,
    private val eventHorizon: EventHorizon,
) : ViewModel() {
    val uiState: StateFlow<TvSettingsUiState> = combine(
        syncManager.isLoggedInObservable.asFlow(),
        settings.artworkConfiguration.flow,
        settings.cachedSubscription.flow,
    ) { isSignedIn, artwork, subscription ->
        TvSettingsUiState(
            isSignedIn = isSignedIn,
            useEpisodeArtwork = artwork.useEpisodeArtwork,
            subscription = subscription,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TvSettingsUiState(
            isSignedIn = syncManager.isLoggedIn(),
            useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
            subscription = settings.cachedSubscription.value,
        ),
    )

    fun setUseEpisodeArtwork(value: Boolean) {
        val configuration = settings.artworkConfiguration.value
        settings.artworkConfiguration.set(configuration.copy(useEpisodeArtwork = value), updateModifiedAt = true)
    }

    fun trackSettingsShown() {
        eventHorizon.track(SettingsGeneralShownEvent)
    }

    fun trackSubscriptionShown() {
        eventHorizon.track(AccountDetailsSubscriptionEvent)
    }

    fun trackPrivacyPolicyShown() {
        eventHorizon.track(AccountDetailsShowPrivacyPolicyEvent)
    }

    fun trackTermsOfUseShown() {
        eventHorizon.track(AccountDetailsShowTosEvent)
    }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L

data class TvSettingsUiState(
    val isSignedIn: Boolean = false,
    val useEpisodeArtwork: Boolean = false,
    val subscription: Subscription? = null,
)
