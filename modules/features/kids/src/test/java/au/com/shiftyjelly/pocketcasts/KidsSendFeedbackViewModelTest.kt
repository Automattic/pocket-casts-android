package au.com.shiftyjelly.pocketcasts

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.kids.feedback.FeedbackManager
import au.com.shiftyjelly.pocketcasts.kids.feedback.FeedbackResult
import au.com.shiftyjelly.pocketcasts.kids.viewmodel.KidsSendFeedbackViewModel
import au.com.shiftyjelly.pocketcasts.kids.viewmodel.SendFeedbackState
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class KidsSendFeedbackViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: KidsSendFeedbackViewModel

    @Mock
    private lateinit var feedbackManager: FeedbackManager

    @Mock
    private lateinit var tracker: AnalyticsTracker

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = KidsSendFeedbackViewModel(feedbackManager, tracker)
    }

    @Test
    fun `onSendFeedbackClick sets showFeedbackDialog to true`() = runTest {
        viewModel.onSendFeedbackClick()
        assertEquals(true, viewModel.showFeedbackDialog.value)
        verify(tracker).track(AnalyticsEvent.KIDS_PROFILE_SEND_FEEDBACK_TAPPED)
    }

    @Test
    fun `onNoThankYouClick sets showFeedbackDialog to false`() = runTest {
        viewModel.onSendFeedbackClick()

        viewModel.onNoThankYouClick()

        assertEquals(false, viewModel.showFeedbackDialog.value)
        verify(tracker).track(AnalyticsEvent.KIDS_PROFILE_NO_THANK_YOU_TAPPED)
    }

    @Test
    fun `should send successful feedback`() = runTest {
        val feedback = "Great app!"
        val feedbackResult = FeedbackResult.Success

        whenever(feedbackManager.sendAnonymousFeedback(feedback)).thenReturn(feedbackResult)

        viewModel.submitFeedback(feedback)

        assertEquals(SendFeedbackState.Success, viewModel.sendFeedbackState.value)
        verify(tracker).track(AnalyticsEvent.KIDS_PROFILE_FEEDBACK_SENT)
    }

    @Test
    fun `should get an error when sending feedback`() = runTest {
        val feedback = "Great app!"
        val feedbackResult = FeedbackResult.Error

        whenever(feedbackManager.sendAnonymousFeedback(feedback)).thenReturn(feedbackResult)

        viewModel.submitFeedback(feedback)

        assertEquals(SendFeedbackState.Error, viewModel.sendFeedbackState.value)
        verify(tracker).track(AnalyticsEvent.KIDS_PROFILE_FEEDBACK_SENT)
    }

    @Test
    fun `should track event for when thank you for your interest screen is seen`() {
        viewModel.onThankYouForYourInterestSeen()
        verify(tracker).track(AnalyticsEvent.KIDS_PROFILE_THANK_YOU_FOR_YOUR_INTEREST_SEEN)
    }

    @Test
    fun `should track event for when feedback form screen is seen`() {
        viewModel.onFeedbackFormSeen()
        verify(tracker).track(AnalyticsEvent.KIDS_PROFILE_FEEDBACK_FORM_SEEN)
    }
}
