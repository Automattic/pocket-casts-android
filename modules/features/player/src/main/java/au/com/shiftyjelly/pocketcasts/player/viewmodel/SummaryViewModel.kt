package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val transcriptManager: TranscriptManager,
    private val settings: Settings,
    private val paymentClient: PaymentClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    sealed interface SummaryState {
        data object Loading : SummaryState
        data class Loaded(val text: String) : SummaryState
        data class Upsell(val text: String, val isFreeTrialAvailable: Boolean) : SummaryState
        data object NotAvailable : SummaryState
    }

    private val _state = MutableStateFlow<SummaryState>(SummaryState.Loading)
    val state: StateFlow<SummaryState> = _state.asStateFlow()

    private var currentEpisodeUuid: String? = null
    private var loadedText: String? = null
    private var loadJob: Job? = null
    private var isFreeTrialAvailable = false

    init {
        viewModelScope.launch {
            val plans = paymentClient.loadSubscriptionPlans().getOrNull()
            isFreeTrialAvailable = plans?.findOfferPlan(
                SubscriptionTier.Plus,
                BillingCycle.Monthly,
                SubscriptionOffer.Trial,
            ) != null
            val text = loadedText
            if (text != null) {
                _state.value = resolveState(text)
            }
        }
        viewModelScope.launch {
            settings.cachedSubscription.flow.collect {
                val text = loadedText
                if (text != null) {
                    _state.value = resolveState(text)
                }
            }
        }
    }

    fun clearSummary() {
        currentEpisodeUuid = null
        loadedText = null
        loadJob?.cancel()
        _state.value = SummaryState.NotAvailable
    }

    fun loadSummary(episodeUuid: String) {
        if (currentEpisodeUuid == episodeUuid && _state.value is SummaryState.Loaded) return
        if (currentEpisodeUuid == episodeUuid && _state.value is SummaryState.Upsell) return
        if (currentEpisodeUuid == episodeUuid && loadJob?.isActive == true) return
        currentEpisodeUuid = episodeUuid
        loadedText = null
        loadJob?.cancel()
        _state.value = SummaryState.Loading
        loadJob = viewModelScope.launch(ioDispatcher) {
            val text = transcriptManager.loadSummaryText(episodeUuid)
            loadedText = text
            _state.value = if (text != null) {
                resolveState(text)
            } else {
                SummaryState.NotAvailable
            }
        }
    }

    private fun resolveState(text: String): SummaryState {
        val isPaidUser = settings.cachedSubscription.value != null
        return if (isPaidUser) SummaryState.Loaded(text) else SummaryState.Upsell(text, isFreeTrialAvailable)
    }
}
