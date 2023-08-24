package au.com.shiftyjelly.pocketcasts.views.review

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.util.Date

@RunWith(AndroidJUnit4::class)
class InAppReviewHelperTest {

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

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

        verify(reviewManager).requestReviewFlow()
        verifyNoMoreInteractions(reviewManager)
    }

    private fun launchReviewDialog() = runTest() {
        inAppReviewHelper.launchReviewDialog(
            activity = mock(),
            delayInMs = 100,
            sourceView = SourceView.UNKNOWN
        )
    }

    private fun initInAppReviewHelper() {
        inAppReviewHelper = InAppReviewHelper(
            settings = settings,
            analyticsTracker = analyticsTracker,
            reviewManager = reviewManager
        )
    }
}
