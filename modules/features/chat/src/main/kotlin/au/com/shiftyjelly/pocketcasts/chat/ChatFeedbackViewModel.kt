package au.com.shiftyjelly.pocketcasts.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.eventhorizon.EpisodeChatFeedbackFormDismissedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackFormShownEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackReasonType
import com.automattic.eventhorizon.EpisodeChatFeedbackSubmittedEvent
import com.automattic.eventhorizon.EpisodeChatFeedbackTriggerType
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
    private val eventHorizon: EventHorizon,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        UiState(
            reason = savedStateHandle.get<ChatFeedbackReason>(REASON_KEY),
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
    private var messageIndex: Long? = null
    private var answerUuid: String? = null

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
        messageIndex: Long?,
        answerUuid: String?,
    ) {
        this.episodeUuid = episodeUuid
        this.podcastUuid = podcastUuid
        this.sourceView = sourceView
        this.trigger = trigger
        this.messageIndex = messageIndex
        this.answerUuid = answerUuid
        if (savedStateHandle.get<Boolean>(IS_SHOWN_TRACKED_KEY) == true) return
        savedStateHandle[IS_SHOWN_TRACKED_KEY] = true
        eventHorizon.track(
            EpisodeChatFeedbackFormShownEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                trigger = trigger,
                messageIndex = messageIndex,
            ),
        )
    }

    fun onReasonSelected(reason: ChatFeedbackReason) {
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
        if (isSubmitted) return
        val details = state.detailsToSend
        isSubmitted = true
        _submitted.trySend(Unit)
        applicationScope.launch {
            val ratedAnswer = answerUuid?.let { findRatedAnswer(it) }
            eventHorizon.track(
                EpisodeChatFeedbackSubmittedEvent(
                    source = sourceView.analyticsValue,
                    episodeUuid = episodeUuid,
                    podcastUuid = podcastUuid,
                    trigger = trigger,
                    reason = reason.analyticsValue,
                    hasDetails = details.isNotEmpty(),
                    messageIndex = messageIndex,
                    details = details.takeIf { it.isNotEmpty() }?.truncatedForAnalytics(),
                    question = ratedAnswer?.question,
                    answer = ratedAnswer?.answer,
                ),
            )
        }
    }

    private suspend fun findRatedAnswer(answerUuid: String): ChatRatedAnswer? {
        return try {
            chatManager.observeMessages(episodeUuid).first().ratedAnswer(answerUuid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogBuffer.e(LOG_TAG, e, "Failed to read the rated episode chat answer")
            null
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
                messageIndex = messageIndex,
            ),
        )
    }

    data class UiState(
        val reason: ChatFeedbackReason? = null,
        val details: String = "",
    ) {
        val canSubmit get() = reason != null
        val showDetails get() = reason == ChatFeedbackReason.Other
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

private val ChatFeedbackReason.analyticsValue
    get() = when (this) {
        ChatFeedbackReason.NotInteresting -> EpisodeChatFeedbackReasonType.NotInteresting
        ChatFeedbackReason.WrongFacts -> EpisodeChatFeedbackReasonType.WrongFacts
        ChatFeedbackReason.OutOfDate -> EpisodeChatFeedbackReasonType.OutOfDate
        ChatFeedbackReason.Offensive -> EpisodeChatFeedbackReasonType.Offensive
        ChatFeedbackReason.Other -> EpisodeChatFeedbackReasonType.Other
    }
