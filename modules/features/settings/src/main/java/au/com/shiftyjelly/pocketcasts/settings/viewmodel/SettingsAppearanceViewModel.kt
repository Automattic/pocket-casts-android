package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.repositories.widget.WidgetManager
import au.com.shiftyjelly.pocketcasts.ui.helper.AppIcon
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsAppearanceViewModel @Inject constructor(
    userManager: UserManager,
    private val settings: Settings,
    private val playbackManager: PlaybackManager,
    val userEpisodeManager: UserEpisodeManager,
    private val widgetManager: WidgetManager,
    val theme: Theme,
    private val appIcon: AppIcon,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {

    val signInState: LiveData<SignInState> = userManager.getSignInState().toLiveData()
    val createAccountState = MutableLiveData<SettingsAppearanceState>().apply { value = SettingsAppearanceState.Empty }
    val showArtworkOnLockScreen = settings.showArtworkOnLockScreen.flow
    val useEmbeddedArtwork = settings.useEmbeddedArtwork.flow

    var changeThemeType: Pair<Theme.ThemeType?, Theme.ThemeType?> = Pair(null, null)
    var changeAppIconType: Pair<AppIcon.AppIconType?, AppIcon.AppIconType?> = Pair(null, null)

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_APPEARANCE_SHOWN)
    }

    fun onRefreshArtwork() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_APPEARANCE_REFRESH_ALL_ARTWORK_TAPPED)
    }

    fun onThemeChanged(theme: Theme.ThemeType) {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_APPEARANCE_THEME_CHANGED,
            mapOf(
                "value" to when (theme) {
                    Theme.ThemeType.LIGHT -> "default_light"
                    Theme.ThemeType.DARK -> "default_dark"
                    Theme.ThemeType.ROSE -> "rose"
                    Theme.ThemeType.INDIGO -> "indigo"
                    Theme.ThemeType.EXTRA_DARK -> "extra_dark"
                    Theme.ThemeType.DARK_CONTRAST -> "dark_contrast"
                    Theme.ThemeType.LIGHT_CONTRAST -> "light_contrast"
                    Theme.ThemeType.ELECTRIC -> "electric"
                    Theme.ThemeType.CLASSIC_LIGHT -> "classic"
                    Theme.ThemeType.RADIOACTIVE -> "radioactive"
                },
            ),
        )
    }

    fun loadThemesAndIcons() {
        createAccountState.postValue(SettingsAppearanceState.ThemesAndIconsLoading)
        val appIcons = appIcon.allAppIconTypes.toList()

        createAccountState.postValue(
            SettingsAppearanceState.ThemesAndIconsLoaded(
                theme.activeTheme,
                theme.allThemes.toList(),
                appIcon.activeAppIcon,
                appIcons,
            ),
        )
    }

    fun updateGlobalIcon(appIconType: AppIcon.AppIconType) {
        appIcon.activeAppIcon = appIconType
        appIcon.enableSelectedAlias(appIconType)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_APPEARANCE_APP_ICON_CHANGED,
            mapOf(
                "value" to when (appIconType) {
                    AppIcon.AppIconType.DEFAULT -> "default"
                    AppIcon.AppIconType.DARK -> "dark"
                    AppIcon.AppIconType.ROUND_LIGHT -> "round_light"
                    AppIcon.AppIconType.ROUND_DARK -> "round_dark"
                    AppIcon.AppIconType.INDIGO -> "indigo"
                    AppIcon.AppIconType.ROSE -> "rose"
                    AppIcon.AppIconType.CAT -> "pocket_cats"
                    AppIcon.AppIconType.REDVELVET -> "red_velvet"
                    AppIcon.AppIconType.PLUS -> "plus"
                    AppIcon.AppIconType.CLASSIC -> "classic"
                    AppIcon.AppIconType.ELECTRIC_BLUE -> "electric_blue"
                    AppIcon.AppIconType.ELECTRIC_PINK -> "electric_pink"
                    AppIcon.AppIconType.RADIOACTIVE -> "radioactive"
                    AppIcon.AppIconType.HALLOWEEN -> "halloween"
                    AppIcon.AppIconType.PATRON_CHROME -> "patron_chrome"
                    AppIcon.AppIconType.PATRON_ROUND -> "patron_round"
                    AppIcon.AppIconType.PATRON_GLOW -> "patron_glow"
                    AppIcon.AppIconType.PATRON_DARK -> "patron_dark"
                    AppIcon.AppIconType.PRIDE_2023 -> "pride_2023"
                },
            ),
        )
    }

    fun updateUpNextDarkTheme(value: Boolean) {
        settings.useDarkUpNextTheme.set(value, needsSync = false)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_APPEARANCE_USE_DARK_UP_NEXT_TOGGLED,
            mapOf("enabled" to value),
        )
    }

    fun updateWidgetForDynamicColors(value: Boolean) {
        settings.useDynamicColorsForWidget.set(value, needsSync = false)
        viewModelScope.launch(Dispatchers.IO) {
            widgetManager.updateWidgetFromSettings(playbackManager)
        }
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_APPEARANCE_USE_DYNAMIC_COLORS_WIDGET_TOGGLED,
            mapOf("enabled" to value),
        )
    }

    fun updateShowArtworkOnLockScreen(value: Boolean) {
        settings.showArtworkOnLockScreen.set(value, needsSync = false)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_APPEARANCE_SHOW_ARTWORK_ON_LOCK_SCREEN_TOGGLED,
            mapOf("enabled" to value),
        )
    }

    fun updateUseEmbeddedArtwork(value: Boolean) {
        settings.useEmbeddedArtwork.set(value, needsSync = false)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_APPEARANCE_USE_EMBEDDED_ARTWORK_TOGGLED,
            mapOf("enabled" to value),
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
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_APPEARANCE_FOLLOW_SYSTEM_THEME_TOGGLED,
            mapOf("enabled" to use),
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
