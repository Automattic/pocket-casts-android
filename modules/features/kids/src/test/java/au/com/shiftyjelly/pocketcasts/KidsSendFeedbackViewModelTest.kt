package au.com.shiftyjelly.pocketcasts

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.kids.viewmodel.KidsSendFeedbackViewModel
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class KidsSendFeedbackViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: KidsSendFeedbackViewModel

    private val tracker: AnalyticsTracker = mock()

    @Before
    fun setUp() {
        viewModel = KidsSendFeedbackViewModel(tracker)
    }

    @Test
    fun `onSendFeedbackClick sets showFeedbackDialog to true and track event`() = runTest {
        viewModel.onSendFeedbackClick()
        assertEquals(true, viewModel.showFeedbackDialog.value)
        verify(tracker).track(AnalyticsEvent.KIDS_PROFILE_SEND_FEEDBACK_TAPPED)
    }

    @Test
    fun `onNoThankYouClick sets showFeedbackDialog to false and track event`() = runTest {
        viewModel.onNoThankYouClick()

        assertEquals(false, viewModel.showFeedbackDialog.value)
        verify(tracker).track(AnalyticsEvent.KIDS_PROFILE_NO_THANK_YOU_TAPPED)
    }

    @Test
    fun `should track event for when feedback was submitted`() {
        viewModel.onSubmitFeedback()
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
