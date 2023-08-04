package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import dagger.hilt.android.qualifiers.ApplicationContext
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedSettingsViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Mock
    @ApplicationContext
    private lateinit var context: Context
    private lateinit var viewModel: AdvancedSettingsViewModel

    @Before
    fun setUp() {
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
