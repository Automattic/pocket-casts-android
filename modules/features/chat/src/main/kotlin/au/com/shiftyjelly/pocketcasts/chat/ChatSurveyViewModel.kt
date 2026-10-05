package au.com.shiftyjelly.pocketcasts.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import com.automattic.eventhorizon.EpisodeChatSentimentType
import com.automattic.eventhorizon.EpisodeChatSurveyDismissedEvent
import com.automattic.eventhorizon.EpisodeChatSurveyResponseSubmittedEvent
import com.automattic.eventhorizon.EpisodeChatSurveyShownEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatSurveyViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val eventHorizon: EventHorizon,
) : ViewModel() {
    private lateinit var episodeUuid: String
    private lateinit var podcastUuid: String
    private lateinit var sourceView: SourceView

    private var isAnswered: Boolean
        get() = savedStateHandle[IS_ANSWERED_KEY] ?: false
        set(value) {
            savedStateHandle[IS_ANSWERED_KEY] = value
        }

    fun onShown(
        episodeUuid: String,
        podcastUuid: String,
        sourceView: SourceView,
    ) {
        this.episodeUuid = episodeUuid
        this.podcastUuid = podcastUuid
        this.sourceView = sourceView
        if (savedStateHandle.get<Boolean>(IS_SHOWN_TRACKED_KEY) == true) return
        savedStateHandle[IS_SHOWN_TRACKED_KEY] = true
        eventHorizon.track(
            EpisodeChatSurveyShownEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        )
    }

    fun onAnswer(isPositive: Boolean) {
        if (isAnswered) return
        isAnswered = true
        eventHorizon.track(
            EpisodeChatSurveyResponseSubmittedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                response = if (isPositive) EpisodeChatSentimentType.Positive else EpisodeChatSentimentType.Negative,
            ),
        )
    }

    fun onDismissed() {
        if (isAnswered) return
        eventHorizon.track(
            EpisodeChatSurveyDismissedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        )
    }

    private companion object {
        const val IS_SHOWN_TRACKED_KEY = "is_shown_tracked"
        const val IS_ANSWERED_KEY = "is_answered"
    }
}
