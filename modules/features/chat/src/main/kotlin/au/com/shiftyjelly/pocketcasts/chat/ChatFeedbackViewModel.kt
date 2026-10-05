package au.com.shiftyjelly.pocketcasts.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatFeedback
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatFeedbackManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import com.automattic.eventhorizon.EpisodeChatFeedbackFormDismissedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackFormShownEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackReasonType
import com.automattic.eventhorizon.EpisodeChatFeedbackSubmitTappedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackTriggerType
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    private lateinit var episodeUuid: String
    private lateinit var podcastUuid: String
    private lateinit var sourceView: SourceView
    private lateinit var trigger: EpisodeChatFeedbackTriggerType

    private var isSubmitted: Boolean
        get() = savedStateHandle[IS_SUBMITTED_KEY] ?: false
        set(value) {
            savedStateHandle[IS_SUBMITTED_KEY] = value
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
        savedStateHandle[DETAILS_KEY] = details
        _uiState.update { it.copy(details = details) }
    }

    fun submit(): Boolean {
        val state = _uiState.value
        val reason = state.reason ?: return false
        if (isSubmitted) return false
        isSubmitted = true
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
        applicationScope.launch {
            feedbackManager.submit(
                ChatFeedback(
                    episodeUuid = episodeUuid,
                    podcastUuid = podcastUuid,
                    reason = reason,
                    details = details,
                    conversation = chatManager.observeMessages(episodeUuid).first(),
                ),
            )
        }
        return true
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
    ) {
        val canSubmit get() = reason != null
        val showDetails get() = reason == ChatFeedback.Reason.Other
        val detailsToSend get() = if (showDetails) details.trim() else ""
    }

    private companion object {
        const val IS_SHOWN_TRACKED_KEY = "is_shown_tracked"
        const val IS_SUBMITTED_KEY = "is_submitted"
        const val REASON_KEY = "reason"
        const val DETAILS_KEY = "details"
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
