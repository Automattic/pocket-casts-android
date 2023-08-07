package au.com.shiftyjelly.pocketcasts.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.Window
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ThemeSetting
import au.com.shiftyjelly.pocketcasts.ui.BuildConfig
import au.com.shiftyjelly.pocketcasts.ui.R
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.utils.Util
import javax.inject.Inject
import javax.inject.Singleton
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
class Theme @Inject constructor(private val settings: Settings) {
    companion object {
        fun isDark(context: Context?): Boolean {
            val typedValue = TypedValue()
            context?.theme?.resolveAttribute(R.attr.isDark, typedValue, true)
            return typedValue.data != 0
        }

        fun isImageTintEnabled(context: Context): Boolean {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.isImageTintEnabled, typedValue, true)
            return typedValue.data != 0
        }

        fun imageTintThemeTag(context: Context): String {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.imageTintThemeCacheTag, typedValue, true)
            return (typedValue.string as? String) ?: ""
        }

        val folderColors = listOf(
            R.attr.filter_01,
            R.attr.filter_02,
            R.attr.filter_03,
            R.attr.filter_04,
            R.attr.filter_05,
            R.attr.filter_06,
            R.attr.filter_07,
            R.attr.filter_08,
            R.attr.filter_09,
            R.attr.filter_10,
            R.attr.filter_11,
            R.attr.filter_12
        )
    }

    enum class ThemeType(
        internal val themeSetting: ThemeSetting,
        @StringRes val labelId: Int,
        @StyleRes val resourceId: Int,
        @DrawableRes val iconResourceId: Int,
        val defaultLightIcons: Boolean,
        val darkTheme: Boolean,
        val isPlus: Boolean,
    ) {
        LIGHT(
            themeSetting = ThemeSetting.LIGHT,
            labelId = LR.string.settings_theme_light,
            resourceId = R.style.ThemeLight,
            iconResourceId = IR.drawable.ic_apptheme0,
            defaultLightIcons = false,
            darkTheme = false,
            isPlus = false
        ),
        DARK(
            themeSetting = ThemeSetting.DARK,
            labelId = LR.string.settings_theme_dark,
            resourceId = R.style.ThemeDark,
            iconResourceId = IR.drawable.ic_apptheme1,
            defaultLightIcons = true,
            darkTheme = true,
            isPlus = false
        ),
        ROSE(
            themeSetting = ThemeSetting.ROSE,
            labelId = LR.string.settings_theme_rose,
            resourceId = R.style.Rose,
            iconResourceId = IR.drawable.ic_theme_rose,
            defaultLightIcons = false,
            darkTheme = false,
            isPlus = false
        ),
        INDIGO(
            themeSetting = ThemeSetting.INDIGO,
            labelId = LR.string.settings_theme_indigo,
            resourceId = R.style.Indigo,
            iconResourceId = IR.drawable.ic_indigo,
            defaultLightIcons = true,
            darkTheme = false,
            isPlus = false
        ),
        EXTRA_DARK(
            themeSetting = ThemeSetting.EXTRA_DARK,
            labelId = LR.string.settings_theme_extra_dark,
            resourceId = R.style.ExtraThemeDark,
            iconResourceId = IR.drawable.ic_apptheme2,
            defaultLightIcons = true,
            darkTheme = true,
            isPlus = false
        ),
        DARK_CONTRAST(
            themeSetting = ThemeSetting.DARK_CONTRAST,
            labelId = LR.string.settings_theme_dark_contrast,
            resourceId = R.style.DarkContrast,
            iconResourceId = IR.drawable.ic_apptheme6,
            defaultLightIcons = true,
            darkTheme = true,
            isPlus = false
        ),
        LIGHT_CONTRAST(
            themeSetting = ThemeSetting.LIGHT_CONTRAST,
            labelId = LR.string.settings_theme_light_contrast,
            resourceId = R.style.LightContrast,
            iconResourceId = IR.drawable.ic_apptheme7,
            defaultLightIcons = false,
            darkTheme = false,
            isPlus = false
        ),
        ELECTRIC(
            themeSetting = ThemeSetting.ELECTRIC,
            labelId = LR.string.settings_theme_electricity,
            resourceId = R.style.Electric,
            iconResourceId = IR.drawable.ic_apptheme5,
            defaultLightIcons = true,
            darkTheme = true,
            isPlus = true
        ),
        CLASSIC_LIGHT(
            themeSetting = ThemeSetting.CLASSIC_LIGHT,
            labelId = LR.string.settings_theme_classic, // "Classic Light"
            resourceId = R.style.ClassicLight,
            iconResourceId = IR.drawable.ic_apptheme3,
            defaultLightIcons = true,
            darkTheme = false,
            isPlus = true
        ),
        RADIOACTIVE(
            themeSetting = ThemeSetting.RADIOACTIVE,
            labelId = LR.string.settings_theme_radioactivity,
            resourceId = R.style.Radioactive,
            iconResourceId = IR.drawable.ic_theme_radioactive,
            defaultLightIcons = true,
            darkTheme = true,
            isPlus = true
        );

        companion object {
            fun fromThemeSetting(themeSetting: ThemeSetting): ThemeType = when (themeSetting) {
                ThemeSetting.LIGHT -> Theme.ThemeType.LIGHT
                ThemeSetting.DARK -> Theme.ThemeType.DARK
                ThemeSetting.ROSE -> Theme.ThemeType.ROSE
                ThemeSetting.INDIGO -> Theme.ThemeType.INDIGO
                ThemeSetting.EXTRA_DARK -> Theme.ThemeType.EXTRA_DARK
                ThemeSetting.DARK_CONTRAST -> Theme.ThemeType.DARK_CONTRAST
                ThemeSetting.LIGHT_CONTRAST -> Theme.ThemeType.LIGHT_CONTRAST
                ThemeSetting.ELECTRIC -> Theme.ThemeType.ELECTRIC
                ThemeSetting.CLASSIC_LIGHT -> Theme.ThemeType.CLASSIC_LIGHT
                ThemeSetting.RADIOACTIVE -> Theme.ThemeType.RADIOACTIVE
            }
        }
    }

    var activeTheme: ThemeType = getThemeFromPreferences()
        set(value) {
            field = value
            setThemeToPreferences(value)

            if (value.darkTheme) {
                setPreferredDarkThemeToPreferences(value)
            } else {
                setPreferredLightThemeToPreferences(value)
            }
        }

    val allThemes = ThemeType.values()

    val isDarkTheme: Boolean
        get() = activeTheme.darkTheme

    val isLightTheme: Boolean
        get() = !isDarkTheme

    val folderLockedImageName: Int
        get() {
            return when (activeTheme) {
                ThemeType.DARK, ThemeType.EXTRA_DARK, ThemeType.ELECTRIC, ThemeType.DARK_CONTRAST -> IR.drawable.folder_lock_dark
                ThemeType.LIGHT, ThemeType.CLASSIC_LIGHT, ThemeType.LIGHT_CONTRAST, ThemeType.INDIGO -> IR.drawable.folder_lock_light
                ThemeType.RADIOACTIVE -> IR.drawable.folder_lock_radioactive
                ThemeType.ROSE -> IR.drawable.folder_lock_rose
            }
        }

    fun updateTheme(activity: AppCompatActivity, theme: Theme.ThemeType, configuration: Configuration = activity.resources.configuration) {
        activity.setTheme(theme.resourceId)
        if (theme == activeTheme) return
        activeTheme = theme

        when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> { // Night mode is not active, we're using the light theme
                if (activeTheme.darkTheme) {
                    setUseSystemTheme(false, null)
                }
            }
            Configuration.UI_MODE_NIGHT_YES -> { // Night mode is active, we're using dark theme
                if (!activeTheme.darkTheme) {
                    setUseSystemTheme(false, null)
                }
            }
            else -> getPreferredDarkThemeFromPreferences()
        }

        activity.recreate()
    }

    fun toggleDarkLightThemeActivity(activity: AppCompatActivity) {
        val newTheme = toggledThemed()
        updateTheme(activity, newTheme)
    }

    fun setupThemeForConfig(activity: AppCompatActivity, configuration: Configuration) {
        if (getUseSystemTheme() && !Util.isAutomotive(activity)) {
            val theme = when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> {
                    getPreferredLightFromPreferences()
                } // Night mode is not active, we're using the light theme
                Configuration.UI_MODE_NIGHT_YES -> {
                    getPreferredDarkThemeFromPreferences()
                } // Night mode is active, we're using dark theme
                else -> getPreferredDarkThemeFromPreferences()
            }

            updateTheme(activity, theme)
        } else {
            updateTheme(activity, activeTheme)
        }
    }

    private fun toggledThemed(): Theme.ThemeType {
        return if (BuildConfig.DEBUG) {
            val allThemes = Theme.ThemeType.values()
            val index = allThemes.indexOf(activeTheme)
            allThemes[(index + 1) % allThemes.size]
        } else {
            if (isLightTheme) getPreferredDarkThemeFromPreferences() else getPreferredLightFromPreferences()
        }
    }

    private fun getThemeFromPreferences(): ThemeType =
        ThemeType.fromThemeSetting(settings.theme.flow.value)

    private fun setThemeToPreferences(theme: ThemeType) {
        settings.theme.set(theme.themeSetting)
    }

    private fun getPreferredDarkThemeFromPreferences(): ThemeType =
        ThemeType.fromThemeSetting(settings.darkThemePreference.flow.value)

    private fun getPreferredLightFromPreferences(): ThemeType =
        ThemeType.fromThemeSetting(settings.lightThemePreference.flow.value)

    private fun setPreferredDarkThemeToPreferences(theme: ThemeType) {
        settings.darkThemePreference.set(theme.themeSetting)
    }

    private fun setPreferredLightThemeToPreferences(theme: ThemeType) {
        settings.lightThemePreference.set(theme.themeSetting)
    }

    fun setUseSystemTheme(value: Boolean, activity: AppCompatActivity?) {
        settings.useSystemTheme.set(value, commit = true)

        if (value && activity != null) {
            setupThemeForConfig(activity, activity.resources.configuration)
        }
    }

    fun getUseSystemTheme(): Boolean = settings.useSystemTheme.flow.value

    fun getPodcastTintColor(podcast: Podcast): Int {
        return podcast.getTintColor(isDarkTheme)
    }

    fun playerBackgroundColor(podcast: Podcast?): Int {
        val podcastBgColor = podcast?.backgroundColor ?: 0xFF3D3D3D.toInt()

        return ThemeColor.playerBackground01(activeTheme, podcastBgColor)
    }

    fun playerBackground2Color(podcast: Podcast?): Int {
        val podcastBgColor = podcast?.backgroundColor ?: 0xFF5C6164.toInt()

        return ThemeColor.playerBackground02(activeTheme, podcastBgColor)
    }

    fun playerHighlightColor(podcast: Podcast?): Int {
        val tintColor = podcast?.getTintColor(true) ?: Color.WHITE

        return ThemeColor.playerHighlight01(activeTheme, tintColor)
    }

    fun playerHighlight7Color(podcast: Podcast?): Int {
        val tintColor = podcast?.getTintColor(true) ?: Color.WHITE

        return ThemeColor.playerHighlight07(activeTheme, tintColor)
    }

    fun getUserFilePlayerHighlightColor(): Int {
        return Color.WHITE
    }

    @ColorInt fun getColorForeground(context: Context): Int {
        return context.getThemeColor(R.attr.secondary_ui_01)
    }

    fun getColorIconPrimary(context: Context): Int {
        return context.getThemeColor(R.attr.primary_icon_01)
    }

    fun getColorIconSecondary(context: Context): Int {
        return context.getThemeColor(R.attr.secondary_icon_01)
    }

    @ColorInt fun getToolbarIconColor(context: Context): Int {
        return context.getThemeColor(R.attr.secondary_icon_01)
    }

    @ColorInt fun getToolbarTextColor(context: Context): Int {
        return context.getThemeColor(R.attr.secondary_text_01)
    }

    @ColorInt fun getToolbarBackgroundColor(context: Context): Int {
        return context.getThemeColor(R.attr.secondary_ui_01)
    }

    @ColorInt fun getStatusBarColor(context: Context): Int {
        return getColorForeground(context)
    }

    /**
     * Colour the status bar and let the window know if the icons should be black or white.
     *
     * Passing the status bar means the following:
     * StatusBarColor.Light means the background is light and the icons are black
     * StatusBarColor.Dark means the background is dark and the icons are white
     */
    fun updateWindowStatusBar(window: Window?, statusBarColor: StatusBarColor? = null, context: Context) {
        window ?: return

        // Fixes the issue where the window internal DecorView is null and causes a crash. https://console.firebase.google.com/project/singular-vector-91401/crashlytics/app/android:au.com.shiftyjelly.pocketcasts/issues/d44d873d36442ac43b59f56fe95e311b
        if (window.peekDecorView() == null) {
            return
        }

        val color = statusBarColor ?: (if (isDarkTheme) StatusBarColor.Dark else StatusBarColor.Light)
        when {
            color is StatusBarColor.Custom -> {
                if (color.isWhiteIcons) {
                    useLightStatusBarIcons(window)
                } else {
                    useDarkStatusBarIcons(window)
                }
                window.statusBarColor = color.color
            }
            isDarkTheme -> {
                useLightStatusBarIcons(window)
                window.statusBarColor = context.getThemeColor(R.attr.secondary_ui_01)
            }
            color is StatusBarColor.Dark -> {
                useLightStatusBarIcons(window)
                window.statusBarColor = ContextCompat.getColor(context, R.color.colorStatusBarDarkInLight)
            }
            color is StatusBarColor.Light -> {
                if (activeTheme.defaultLightIcons) {
                    useLightStatusBarIcons(window)
                } else {
                    useDarkStatusBarIcons(window)
                }
                window.statusBarColor = getStatusBarColor(context)
            }
        }
    }

    private fun useDarkStatusBarIcons(window: Window) {
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }

    private fun useLightStatusBarIcons(window: Window) {
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
    }

    fun setNavigationBarColor(window: Window, isDark: Boolean, color: Int? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        window.navigationBarColor = color ?: (if (isDark) Color.BLACK else Color.WHITE)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = !isDark
    }

    fun verticalPlusLogo(context: Context?): Drawable? {
        return context?.let {
            val resId = verticalPlusLogoRes()
            AppCompatResources.getDrawable(context, resId)
        }
    }

    @DrawableRes
    fun verticalPlusLogoRes() =
        if (isDarkTheme) IR.drawable.plus_logo_vertical_dark else IR.drawable.plus_logo_vertical_white
}
