package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@RunWith(MockitoJUnitRunner::class)
class AdvancedSettingsViewModelTest {

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Mock
    @ApplicationContext
    private lateinit var context: Context
    private lateinit var viewModel: AdvancedSettingsViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(settings.syncOnMeteredNetwork()).thenReturn(false)
        viewModel = AdvancedSettingsViewModel(
            settings,
            analyticsTracker,
            context
        )
    }

    @Test
    fun `verify settings methods initialize the viewModel state correctly`() {
        TestCase.assertEquals(viewModel.state.value.backgroundSyncOnMeteredState.isChecked, false)
    }
}
