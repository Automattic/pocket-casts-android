package au.com.shiftyjelly.pocketcasts.kids.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.kids.feedback.FeedbackManager
import au.com.shiftyjelly.pocketcasts.kids.feedback.FeedbackResult
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class KidsSendFeedbackViewModel @Inject constructor(
    private val feedbackManager: FeedbackManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog: StateFlow<Boolean> = _showFeedbackDialog

    private val _feedbackSent = MutableStateFlow(false)
    val feedbackSent: StateFlow<Boolean> = _feedbackSent

    fun onThankYouForYourInterestSeen() {
        analyticsTracker.track(AnalyticsEvent.KIDS_PROFILE_THANK_YOU_FOR_YOUR_INTEREST_SEEN)
    }

    fun onFeedbackFormSeen() {
        analyticsTracker.track(AnalyticsEvent.KIDS_PROFILE_FEEDBACK_FORM_SEEN)
    }

    fun onSendFeedbackClick() {
        analyticsTracker.track(AnalyticsEvent.KIDS_PROFILE_SEND_FEEDBACK_TAPPED)
        viewModelScope.launch {
            _showFeedbackDialog.value = true
        }
    }

    fun onNoThankYouClick() {
        analyticsTracker.track(AnalyticsEvent.KIDS_PROFILE_NO_THANK_YOU_TAPPED)
        viewModelScope.launch {
            _showFeedbackDialog.value = false
        }
    }

    fun sendFeedback(feedback: String) {
        analyticsTracker.track(AnalyticsEvent.KIDS_PROFILE_FEEDBACK_SENT)

        viewModelScope.launch {
            val result: FeedbackResult = feedbackManager.sendAnonymousFeedback(feedback)
            _feedbackSent.value = result is FeedbackResult.Success
        }
    }
}
