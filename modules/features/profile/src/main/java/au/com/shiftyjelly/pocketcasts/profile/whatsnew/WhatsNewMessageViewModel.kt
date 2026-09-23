package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.whatsnew.WhatsNewManager
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewAction
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewContent
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewImage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewResearch
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.WhatsNewActionTappedEvent
import com.automattic.eventhorizon.WhatsNewMessageShownEvent
import com.automattic.eventhorizon.WhatsNewPollResponseSubmittedEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel(assistedFactory = WhatsNewMessageViewModel.Factory::class)
class WhatsNewMessageViewModel @AssistedInject constructor(
    @Assisted private val messageId: String,
    private val manager: WhatsNewManager,
    private val eventHorizon: EventHorizon,
    settings: Settings,
) : ViewModel() {
    private val isMissing = MutableStateFlow(false)

    private val selectedOptionId = MutableStateFlow<String?>(null)

    private var shownMessage: WhatsNewMessage? = null

    internal val uiState: StateFlow<UiState> = combine(
        manager.feedMessages
            .map { messages -> messages.firstOrNull { it.id == messageId }?.also(::onMessageFound) ?: shownMessage }
            .distinctUntilChanged(),
        isMissing,
        selectedOptionId,
        manager.readState,
    ) { message, isMissing, selectedOptionId, readState ->
        when {
            message != null -> UiState.Loaded(
                message = message,
                pages = pagesOf(message),
                poll = message.research?.let { research ->
                    PollState(
                        research = research,
                        selectedOptionId = selectedOptionId,
                        hasResponded = readState.hasRespondedTo(research.poll.pollId),
                    )
                },
            )

            isMissing -> UiState.Missing

            else -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val bottomInset: StateFlow<Int> = settings.bottomInset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private fun onMessageFound(message: WhatsNewMessage) {
        if (shownMessage == null) {
            eventHorizon.track(WhatsNewMessageShownEvent(messageUuid = message.id, messageType = message.type.analyticsValue))
        }
        shownMessage = message
    }

    init {
        viewModelScope.launch {
            manager.refreshIfNeeded()
            if (manager.feedMessages.first().none { it.id == messageId }) {
                isMissing.value = true
            }
        }
    }

    internal fun onActionClick(event: WhatsNewActionEvent) {
        val message = shownMessage ?: return
        eventHorizon.track(
            WhatsNewActionTappedEvent(
                messageUuid = message.id,
                messageType = message.type.analyticsValue,
                action = event.analyticsValue,
            ),
        )
    }

    fun onOptionClick(optionId: String) {
        val poll = shownMessage?.research?.poll ?: return
        if (manager.readState.value.hasRespondedTo(poll.pollId)) return
        selectedOptionId.value = optionId
    }

    fun onSubmitClick() {
        val message = shownMessage ?: return
        val poll = message.research?.poll ?: return
        val option = poll.options.firstOrNull { it.id == selectedOptionId.value } ?: return
        if (manager.readState.value.hasRespondedTo(poll.pollId)) return
        manager.markAsResponded(poll.pollId)
        eventHorizon.track(
            WhatsNewPollResponseSubmittedEvent(
                messageUuid = message.id,
                messageType = message.type.analyticsValue,
                pollUuid = poll.pollId,
                pollKey = poll.pollKey,
                optionUuid = option.id,
                pollOptionKey = option.pollOptionKey,
            ),
        )
    }

    internal sealed interface UiState {
        data object Loading : UiState

        data object Missing : UiState

        data class Loaded(
            val message: WhatsNewMessage,
            val pages: List<Page>,
            val poll: PollState?,
        ) : UiState
    }

    internal data class PollState(
        val research: WhatsNewResearch,
        val selectedOptionId: String?,
        val hasResponded: Boolean,
    ) {
        val canSubmit get() = !hasResponded && research.poll.options.any { it.id == selectedOptionId }
    }

    internal data class Page(
        val image: WhatsNewImage?,
        val heading: String,
        val description: String,
        val action: Action?,
    )

    internal data class Action(
        val label: String,
        val event: WhatsNewActionEvent,
    )

    private fun pagesOf(message: WhatsNewMessage) = when (val content = message.content) {
        is WhatsNewContent.Pages -> content.pages.map { page ->
            Page(
                image = page.image,
                heading = page.heading,
                description = page.description,
                action = page.action?.let(::actionOf),
            )
        }

        is WhatsNewContent.Research -> emptyList()
    }

    private fun actionOf(action: WhatsNewAction): Action? {
        val event = WhatsNewActionEvent.fromKey(action.event)
        if (event == null) {
            Timber.i("What's New: dropping an action this build doesn't support: ${action.event}")
        }
        return event?.let { Action(label = action.label, event = it) }
    }

    private val WhatsNewMessage.research get() = (content as? WhatsNewContent.Research)?.research

    @AssistedFactory
    interface Factory {
        fun create(messageId: String): WhatsNewMessageViewModel
    }
}
