package au.com.shiftyjelly.pocketcasts.views.review

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.sharedtest.FakeCrashLogging
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InAppReviewHelperTest {

    @Mock
    private lateinit var settings: Settings

    private lateinit var inAppReviewHelper: InAppReviewHelper

    private lateinit var reviewManager: ReviewManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        reviewManager = spy(FakeReviewManager(appContext))
    }

    @Test
    fun testInAppReviewFlowRequestedOnlyOnce() = runTest {
        whenever(settings.getReviewRequestedDates())
            .thenReturn(emptyList())
            .thenReturn(listOf(Date().toString()))
        initInAppReviewHelper()

        launchReviewDialog()
        launchReviewDialog()

        verify(reviewManager, times(1)).requestReviewFlow()
    }

    private fun launchReviewDialog() = runTest {
        inAppReviewHelper.launchReviewDialog(
            activity = mock(),
            delayInMs = 100,
            sourceView = SourceView.UNKNOWN,
        )
    }

    private fun initInAppReviewHelper() {
        inAppReviewHelper = InAppReviewHelper(
            settings = settings,
            analyticsTracker = AnalyticsTracker.test(),
            reviewManager = reviewManager,
            crashLogging = FakeCrashLogging(),
        )
    }
}
