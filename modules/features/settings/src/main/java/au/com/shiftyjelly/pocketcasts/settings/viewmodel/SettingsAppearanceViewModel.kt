package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.ui.helper.AppIcon
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsAppearanceAppIconChangedEvent
import com.automattic.eventhorizon.SettingsAppearanceFollowSystemThemeToggledEvent
import com.automattic.eventhorizon.SettingsAppearanceRefreshAllArtworkTappedEvent
import com.automattic.eventhorizon.SettingsAppearanceShowArtworkOnLockScreenToggledEvent
import com.automattic.eventhorizon.SettingsAppearanceShownEvent
import com.automattic.eventhorizon.SettingsAppearanceThemeChangedEvent
import com.automattic.eventhorizon.SettingsAppearanceUseDarkUpNextToggledEvent
import com.automattic.eventhorizon.SettingsAppearanceUseDynamicColorsWidgetToggledEvent
import com.automattic.eventhorizon.SettingsAppearanceUseEpisodeArtworkToggledEvent
import com.automattic.eventhorizon.UpgradeBannerDismissedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsAppearanceViewModel @Inject constructor(
    userManager: UserManager,
    private val settings: Settings,
    val userEpisodeManager: UserEpisodeManager,
    val theme: Theme,
    private val appIcon: AppIcon,
    private val eventHorizon: EventHorizon,
    private val notificationManager: NotificationManager,
) : ViewModel() {

    val signInState: LiveData<SignInState> = userManager.getSignInState().toLiveData()
    val createAccountState = MutableLiveData<SettingsAppearanceState>().apply { value = SettingsAppearanceState.Empty }
    val showArtworkOnLockScreen = settings.showArtworkOnLockScreen.flow
    val artworkConfiguration = settings.artworkConfiguration.flow

    var changeThemeType: Pair<Theme.ThemeType?, Theme.ThemeType?> = Pair(null, null)
    var changeAppIconType: Pair<AppIcon.AppIconType?, AppIcon.AppIconType?> = Pair(null, null)

    fun onShown() {
        eventHorizon.track(SettingsAppearanceShownEvent)
    }

    fun onRefreshArtwork() {
        eventHorizon.track(SettingsAppearanceRefreshAllArtworkTappedEvent)
    }

    fun onThemeChanged(theme: Theme.ThemeType) {
        eventHorizon.track(
            SettingsAppearanceThemeChangedEvent(
                value = theme.eventHorizonValue,
            ),
        )
        viewModelScope.launch {
            notificationManager.updateUserFeatureInteraction(OnboardingNotificationType.Themes)
        }
    }

    fun loadThemesAndIcons() {
        createAccountState.postValue(SettingsAppearanceState.ThemesAndIconsLoading)

        createAccountState.postValue(
            SettingsAppearanceState.ThemesAndIconsLoaded(
                theme.activeTheme,
                theme.allThemes.toList(),
                appIcon.activeAppIcon,
                appIcon.allAppIconTypes,
            ),
        )
    }

    fun updateGlobalIcon(appIconType: AppIcon.AppIconType) {
        appIcon.activeAppIcon = appIconType
        appIcon.enableSelectedAlias(appIconType)
        eventHorizon.track(
            SettingsAppearanceAppIconChangedEvent(
                value = appIconType.eventHorizonValue,
            ),
        )
    }

    fun updateUpNextDarkTheme(value: Boolean) {
        settings.useDarkUpNextTheme.set(value, updateModifiedAt = true)
        eventHorizon.track(
            SettingsAppearanceUseDarkUpNextToggledEvent(
                enabled = value,
            ),
        )
    }

    fun updateWidgetForDynamicColors(value: Boolean) {
        settings.useDynamicColorsForWidget.set(value, updateModifiedAt = true)
        eventHorizon.track(
            SettingsAppearanceUseDynamicColorsWidgetToggledEvent(
                enabled = value,
            ),
        )
    }

    fun updateShowArtworkOnLockScreen(value: Boolean) {
        settings.showArtworkOnLockScreen.set(value, updateModifiedAt = true)
        eventHorizon.track(
            SettingsAppearanceShowArtworkOnLockScreenToggledEvent(
                enabled = value,
            ),
        )
    }

    fun updateUseEpisodeArtwork(value: Boolean) {
        val currentConfiguration = settings.artworkConfiguration.value
        settings.artworkConfiguration.set(currentConfiguration.copy(useEpisodeArtwork = value), updateModifiedAt = true)
        eventHorizon.track(
            SettingsAppearanceUseEpisodeArtworkToggledEvent(
                enabled = value,
            ),
        )
    }

    fun updateChangeThemeType(value: Pair<Theme.ThemeType?, Theme.ThemeType?>) {
        changeThemeType = value
    }

    fun updateChangeAppIconType(value: Pair<AppIcon.AppIconType?, AppIcon.AppIconType?>) {
        changeAppIconType = value
    }

    fun useAndroidLightDarkMode(use: Boolean, activity: AppCompatActivity?) {
        theme.setUseSystemTheme(use, activity)
        eventHorizon.track(
            SettingsAppearanceFollowSystemThemeToggledEvent(
                enabled = use,
            ),
        )
    }

    fun onUpgradeBannerDismissed(sourceView: SourceView) {
        eventHorizon.track(
            UpgradeBannerDismissedEvent(
                source = sourceView.eventHorizonValue,
            ),
        )
    }
}

sealed class SettingsAppearanceState {
    object Empty : SettingsAppearanceState()
    object ThemesAndIconsLoading : SettingsAppearanceState()
    data class ThemesAndIconsLoaded(
        val currentThemeType: Theme.ThemeType,
        val themeList: List<Theme.ThemeType>,
        val currentAppIcon: AppIcon.AppIconType,
        val iconList: List<AppIcon.AppIconType>,
    ) : SettingsAppearanceState()
}
