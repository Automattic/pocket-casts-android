package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.ui.helper.AppIcon
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsAppearanceViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var userEpisodeManager: UserEpisodeManager

    @Mock
    private lateinit var theme: Theme

    @Mock
    private lateinit var appIcon: AppIcon

    @Mock
    private lateinit var analyticsTracker: AnalyticsTracker

    @Mock
    private lateinit var notificationManager: NotificationManager

    private lateinit var viewModel: SettingsAppearanceViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(userManager.getSignInState()).thenReturn(
            Flowable.just(
                SignInState.SignedIn(
                    email = "",
                    subscription = null,
                ),
            ),
        )

        val showArtworkOnLockScreen: UserSetting<Boolean> = mock()
        whenever(showArtworkOnLockScreen.flow).thenReturn(MutableStateFlow(false))
        whenever(settings.showArtworkOnLockScreen).thenReturn(showArtworkOnLockScreen)

        val artworkConfiguration: UserSetting<ArtworkConfiguration> = mock()
        whenever(artworkConfiguration.flow).thenReturn(MutableStateFlow(ArtworkConfiguration(false)))
        whenever(settings.artworkConfiguration).thenReturn(artworkConfiguration)

        viewModel = SettingsAppearanceViewModel(
            userManager = userManager,
            settings = settings,
            userEpisodeManager = userEpisodeManager,
            theme = theme,
            appIcon = appIcon,
            analyticsTracker = analyticsTracker,
            notificationManager = notificationManager,
        )
    }

    @Test
    fun `when theme is updated, should track notification interaction`() = runTest {
        viewModel.onThemeChanged(ThemeType.DARK)

        verify(notificationManager).updateUserFeatureInteraction(OnboardingNotificationType.Themes)
    }
}
