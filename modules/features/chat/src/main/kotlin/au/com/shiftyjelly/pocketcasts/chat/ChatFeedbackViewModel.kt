package au.com.shiftyjelly.pocketcasts.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatFeedback
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatFeedbackManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.eventhorizon.EpisodeChatFeedbackFormDismissedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackFormShownEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackReasonType
import com.automattic.eventhorizon.EpisodeChatFeedbackSubmitFailedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackSubmitTappedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackTriggerType
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatFeedbackViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val chatManager: ChatManager,
    private val feedbackManager: ChatFeedbackManager,
    private val eventHorizon: EventHorizon,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        UiState(
            reason = savedStateHandle.get<ChatFeedback.Reason>(REASON_KEY),
            details = savedStateHandle[DETAILS_KEY] ?: "",
        ),
    )
    val uiState = _uiState.asStateFlow()

    private val _submitted = Channel<Unit>(Channel.CONFLATED)
    val submitted = _submitted.receiveAsFlow()

    private lateinit var episodeUuid: String
    private lateinit var podcastUuid: String
    private lateinit var sourceView: SourceView
    private lateinit var trigger: EpisodeChatFeedbackTriggerType

    private var isSubmitted: Boolean
        get() = savedStateHandle[IS_SUBMITTED_KEY] ?: false
        set(value) {
            savedStateHandle[IS_SUBMITTED_KEY] = value
        }

    init {
        if (isSubmitted) _submitted.trySend(Unit)
    }

    fun onShown(
        episodeUuid: String,
        podcastUuid: String,
        sourceView: SourceView,
        trigger: EpisodeChatFeedbackTriggerType,
    ) {
        this.episodeUuid = episodeUuid
        this.podcastUuid = podcastUuid
        this.sourceView = sourceView
        this.trigger = trigger
        if (savedStateHandle.get<Boolean>(IS_SHOWN_TRACKED_KEY) == true) return
        savedStateHandle[IS_SHOWN_TRACKED_KEY] = true
        eventHorizon.track(
            EpisodeChatFeedbackFormShownEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                trigger = trigger,
            ),
        )
    }

    fun onReasonSelected(reason: ChatFeedback.Reason) {
        savedStateHandle[REASON_KEY] = reason
        _uiState.update { it.copy(reason = reason) }
    }

    fun onDetailsChange(details: String) {
        val limited = details.take(MAX_DETAILS_LENGTH)
        savedStateHandle[DETAILS_KEY] = limited
        _uiState.update { it.copy(details = limited) }
    }

    fun submit() {
        val state = _uiState.value
        val reason = state.reason ?: return
        if (isSubmitted || state.isSubmitting) return
        val details = state.detailsToSend
        eventHorizon.track(
            EpisodeChatFeedbackSubmitTappedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                trigger = trigger,
                reason = reason.analyticsValue,
                hasDetails = details.isNotEmpty(),
            ),
        )
        _uiState.update { it.copy(isSubmitting = true, hasSubmitError = false) }
        val request = applicationScope.async {
            runCatching {
                feedbackManager.submit(
                    ChatFeedback(
                        episodeUuid = episodeUuid,
                        podcastUuid = podcastUuid,
                        reason = reason,
                        details = details,
                        conversation = chatManager.observeMessages(episodeUuid).first(),
                    ),
                )
            }.onFailure { LogBuffer.e(LOG_TAG, it, "Failed to submit episode chat feedback") }
        }
        viewModelScope.launch {
            if (request.await().isSuccess) {
                isSubmitted = true
                _submitted.send(Unit)
            } else {
                eventHorizon.track(
                    EpisodeChatFeedbackSubmitFailedEvent(
                        source = sourceView.analyticsValue,
                        episodeUuid = episodeUuid,
                        podcastUuid = podcastUuid,
                        trigger = trigger,
                    ),
                )
                _uiState.update { it.copy(isSubmitting = false, hasSubmitError = true) }
            }
        }
    }

    fun onDismissed() {
        if (isSubmitted) return
        eventHorizon.track(
            EpisodeChatFeedbackFormDismissedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                trigger = trigger,
            ),
        )
    }

    data class UiState(
        val reason: ChatFeedback.Reason? = null,
        val details: String = "",
        val isSubmitting: Boolean = false,
        val hasSubmitError: Boolean = false,
    ) {
        val canSubmit get() = reason != null && !isSubmitting
        val showDetails get() = reason == ChatFeedback.Reason.Other
        val detailsToSend get() = if (showDetails) details.trim() else ""
    }

    private companion object {
        const val IS_SHOWN_TRACKED_KEY = "is_shown_tracked"
        const val IS_SUBMITTED_KEY = "is_submitted"
        const val REASON_KEY = "reason"
        const val DETAILS_KEY = "details"
        const val MAX_DETAILS_LENGTH = 1000
        const val LOG_TAG = "EpisodeChat"
    }
}

private val ChatFeedback.Reason.analyticsValue
    get() = when (this) {
        ChatFeedback.Reason.NotInteresting -> EpisodeChatFeedbackReasonType.NotInteresting
        ChatFeedback.Reason.WrongFacts -> EpisodeChatFeedbackReasonType.WrongFacts
        ChatFeedback.Reason.OutOfDate -> EpisodeChatFeedbackReasonType.OutOfDate
        ChatFeedback.Reason.Offensive -> EpisodeChatFeedbackReasonType.Offensive
        ChatFeedback.Reason.Other -> EpisodeChatFeedbackReasonType.Other
    }
