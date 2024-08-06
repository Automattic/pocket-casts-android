package au.com.shiftyjelly.pocketcasts.kids.feedback

import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import io.reactivex.Single
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Response

@RunWith(MockitoJUnitRunner::class)
class FeedbackManagerTest {

    @Mock
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `should return success when send support feedback`() = runTest {
        val subject = FeedbackManager.SUBJECT
        val inbox = FeedbackManager.INBOX
        val message = "Message"

        val response: Response<Void> = Response.success(null)

        `when`(syncManager.sendAnonymousFeedback(subject, inbox, message)).thenReturn(Single.just(response))

        val feedbackManager = FeedbackManager(syncManager)

        val result = feedbackManager.sendAnonymousFeedback(message)

        assertEquals(FeedbackResult.Success, result)
    }

    @Test
    fun `should return error when throws an exception`() = runTest {
        val subject = FeedbackManager.SUBJECT
        val inbox = FeedbackManager.INBOX
        val message = "Message"

        `when`(syncManager.sendAnonymousFeedback(subject, inbox, message)).thenReturn(Single.error(Exception()))

        val feedbackManager = FeedbackManager(syncManager)

        val result = feedbackManager.sendAnonymousFeedback(message)

        assertEquals(FeedbackResult.Error, result)
    }

    @Test
    fun `should return error when response is not successful`() = runTest {
        val subject = FeedbackManager.SUBJECT
        val inbox = FeedbackManager.INBOX
        val message = "Message"
        val responseError: Response<Void> = Response.error(400, "".toResponseBody(null))

        `when`(syncManager.sendAnonymousFeedback(subject, inbox, message)).thenReturn(Single.just(responseError))

        val feedbackManager = FeedbackManager(syncManager)

        val result = feedbackManager.sendAnonymousFeedback(message)

        assertEquals(FeedbackResult.Error, result)
    }
}
