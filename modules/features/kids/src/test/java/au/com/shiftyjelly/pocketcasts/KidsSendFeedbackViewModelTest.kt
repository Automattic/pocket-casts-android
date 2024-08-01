package au.com.shiftyjelly.pocketcasts

import au.com.shiftyjelly.pocketcasts.kids.viewmodel.KidsSendFeedbackViewModel
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class KidsSendFeedbackViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: KidsSendFeedbackViewModel

    @Before
    fun setUp() {
        viewModel = KidsSendFeedbackViewModel()
    }

    @Test
    fun `onSendFeedbackClick sets showFeedbackDialog to true`() = runTest {
        viewModel.onSendFeedbackClick()
        assertEquals(true, viewModel.showFeedbackDialog.value)
    }

    @Test
    fun `onNoThankYouClick sets showFeedbackDialog to false`() = runTest {
        viewModel.onSendFeedbackClick()

        viewModel.onNoThankYouClick()

        assertEquals(false, viewModel.showFeedbackDialog.value)
    }
}
