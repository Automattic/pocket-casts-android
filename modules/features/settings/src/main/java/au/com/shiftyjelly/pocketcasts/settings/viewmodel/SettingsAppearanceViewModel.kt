package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.ui.helper.AppIcon
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsAppearanceViewModel @Inject constructor(
    userManager: UserManager,
    private val settings: Settings,
    val userEpisodeManager: UserEpisodeManager,
    val theme: Theme,
    private val appIcon: AppIcon
) : ViewModel() {

    val signInState: LiveData<SignInState> = LiveDataReactiveStreams.fromPublisher(userManager.getSignInState())
    val createAccountState = MutableLiveData<SettingsAppearanceState>().apply { value = SettingsAppearanceState.Empty }
    val showArtworkOnLockScreen = MutableLiveData<Boolean>(settings.showArtworkOnLockScreen())
    val useEmbeddedArtwork = MutableLiveData<Boolean>(settings.getUseEmbeddedArtwork())

    var changeThemeType: Pair<Theme.ThemeType?, Theme.ThemeType?> = Pair(null, null)
    var changeAppIconType: Pair<AppIcon.AppIconType?, AppIcon.AppIconType?> = Pair(null, null)

    fun loadThemesAndIcons() {
        createAccountState.postValue(SettingsAppearanceState.ThemesAndIconsLoading)

        createAccountState.postValue(
            SettingsAppearanceState.ThemesAndIconsLoaded(
                theme.activeTheme, theme.allThemes.toList(),
                appIcon.activeAppIcon, appIcon.allAppIconTypes.toList()
            )
        )
    }

    fun updateGlobalIcon(appIconType: AppIcon.AppIconType) {
        appIcon.activeAppIcon = appIconType
        appIcon.enableSelectedAlias(appIconType)
    }

    fun updateShowArtworkOnLockScreen(value: Boolean) {
        settings.setShowArtworkOnLockScreen(value)
        showArtworkOnLockScreen.value = value
    }

    fun updateUseEmbeddedArtwork(value: Boolean) {
        settings.setUseEmbeddedArtwork(value)
        useEmbeddedArtwork.value = value
    }

    fun updateChangeThemeType(value: Pair<Theme.ThemeType?, Theme.ThemeType?>) {
        changeThemeType = value
    }

    fun updateChangeAppIconType(value: Pair<AppIcon.AppIconType?, AppIcon.AppIconType?>) {
        changeAppIconType = value
    }
}

sealed class SettingsAppearanceState {
    object Empty : SettingsAppearanceState()
    object ThemesAndIconsLoading : SettingsAppearanceState()
    data class ThemesAndIconsLoaded(
        val currentThemeType: Theme.ThemeType,
        val themeList: List<Theme.ThemeType>,
        val currentAppIcon: AppIcon.AppIconType,
        val iconList: List<AppIcon.AppIconType>
    ) : SettingsAppearanceState()
}
