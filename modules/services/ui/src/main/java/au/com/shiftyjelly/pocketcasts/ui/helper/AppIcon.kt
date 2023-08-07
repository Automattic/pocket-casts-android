package au.com.shiftyjelly.pocketcasts.ui.helper

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AppIconSetting
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val PREFERENCE_APPICON = "pocketCastsAppIcon"

@Singleton
class AppIcon @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: Settings,
) {

    enum class AppIconType(
        internal val setting: AppIconSetting,
        @StringRes val labelId: Int,
        @DrawableRes val settingsIcon: Int,
        val tier: SubscriptionTier,
        @DrawableRes val launcherIcon: Int,
        val aliasName: String,
    ) {
        DEFAULT(
            setting = AppIconSetting.DEFAULT,
            labelId = LR.string.settings_app_icon_default,
            settingsIcon = IR.drawable.ic_appicon0,
            tier = SubscriptionTier.NONE,
            launcherIcon = IR.mipmap.ic_launcher,
            aliasName = ".ui.MainActivity_0"
        ),
        DARK(
            setting = AppIconSetting.DARK,
            labelId = LR.string.settings_app_icon_dark,
            settingsIcon = IR.drawable.ic_appicon1,
            tier = SubscriptionTier.NONE,
            launcherIcon = IR.mipmap.ic_launcher_1,
            aliasName = ".ui.MainActivity_1"
        ),
        ROUND_LIGHT(
            setting = AppIconSetting.ROUND_LIGHT,
            labelId = LR.string.settings_app_icon_round_light,
            settingsIcon = IR.drawable.ic_appicon2,
            tier = SubscriptionTier.NONE,
            launcherIcon = IR.mipmap.ic_launcher_2,
            aliasName = ".ui.MainActivity_2"
        ),
        ROUND_DARK(
            setting = AppIconSetting.ROUND_DARK,
            labelId = LR.string.settings_app_icon_round_dark,
            settingsIcon = IR.drawable.ic_appicon3,
            tier = SubscriptionTier.NONE,
            launcherIcon = IR.mipmap.ic_launcher_3,
            aliasName = ".ui.MainActivity_3"
        ),
        INDIGO(
            setting = AppIconSetting.INDIGO,
            labelId = LR.string.settings_app_icon_indigo,
            settingsIcon = IR.drawable.ic_appicon_indigo,
            tier = SubscriptionTier.NONE,
            launcherIcon = IR.mipmap.ic_launcher_indigo,
            aliasName = ".ui.MainActivity_9"
        ),
        ROSE(
            setting = AppIconSetting.ROSE,
            labelId = LR.string.settings_app_icon_rose,
            settingsIcon = IR.drawable.appicon_rose,
            tier = SubscriptionTier.NONE,
            launcherIcon = IR.mipmap.ic_launcher_rose,
            aliasName = ".ui.MainActivity_12"
        ),
        CAT(
            setting = AppIconSetting.CAT,
            labelId = LR.string.settings_app_icon_pocket_cats,
            settingsIcon = IR.drawable.ic_appicon_pocket_cats,
            tier = SubscriptionTier.NONE,
            launcherIcon = IR.mipmap.ic_launcher_cat,
            aliasName = ".ui.MainActivity_10"
        ),
        REDVELVET(
            setting = AppIconSetting.REDVELVET,
            labelId = LR.string.settings_app_icon_red_velvet,
            settingsIcon = IR.drawable.appicon_red_velvet,
            tier = SubscriptionTier.NONE,
            launcherIcon = IR.mipmap.ic_launcher_redvelvet,
            aliasName = ".ui.MainActivity_11"
        ),
        PRIDE_2023(
            setting = AppIconSetting.PRIDE_2023,
            labelId = LR.string.settings_app_icon_pride_2023,
            settingsIcon = IR.drawable.appicon_pride_2023,
            tier = SubscriptionTier.NONE,
            launcherIcon = IR.mipmap.ic_launcher_pride_2023,
            aliasName = ".ui.MainActivity_18"
        ),
        PLUS(
            setting = AppIconSetting.PLUS,
            labelId = LR.string.settings_app_icon_plus,
            settingsIcon = IR.drawable.ic_appicon4,
            tier = SubscriptionTier.PLUS,
            launcherIcon = IR.mipmap.ic_launcher_4,
            aliasName = ".ui.MainActivity_4"
        ),
        CLASSIC(
            setting = AppIconSetting.CLASSIC,
            labelId = LR.string.settings_app_icon_classic,
            settingsIcon = IR.drawable.ic_appicon5,
            tier = SubscriptionTier.PLUS,
            launcherIcon = IR.mipmap.ic_launcher_5,
            aliasName = ".ui.MainActivity_5"
        ),
        ELECTRIC_BLUE(
            setting = AppIconSetting.ELECTRIC_BLUE,
            labelId = LR.string.settings_app_icon_electric_blue,
            settingsIcon = IR.drawable.ic_appicon6,
            tier = SubscriptionTier.PLUS,
            launcherIcon = IR.mipmap.ic_launcher_6,
            aliasName = ".ui.MainActivity_6"
        ),
        ELECTRIC_PINK(
            setting = AppIconSetting.ELECTRIC_PINK,
            labelId = LR.string.settings_app_icon_electric_pink,
            settingsIcon = IR.drawable.ic_appicon7,
            tier = SubscriptionTier.PLUS,
            launcherIcon = IR.mipmap.ic_launcher_7,
            aliasName = ".ui.MainActivity_7"
        ),
        RADIOACTIVE(
            setting = AppIconSetting.RADIOACTIVE,
            labelId = LR.string.settings_app_icon_radioactivity,
            settingsIcon = IR.drawable.appicon_radioactive,
            tier = SubscriptionTier.PLUS,
            launcherIcon = IR.mipmap.ic_launcher_radioactive,
            aliasName = ".ui.MainActivity_8"
        ),
        HALLOWEEN(
            setting = AppIconSetting.HALLOWEEN,
            labelId = LR.string.settings_app_icon_halloween,
            settingsIcon = IR.drawable.appicon_halloween,
            tier = SubscriptionTier.PLUS,
            launcherIcon = IR.mipmap.ic_launcher_halloween,
            aliasName = ".ui.MainActivity_13"
        ),
        PATRON_CHROME(
            setting = AppIconSetting.PATRON_CHROME,
            labelId = LR.string.settings_app_icon_patron_chrome,
            settingsIcon = IR.drawable.appicon_patron_chrome,
            tier = SubscriptionTier.PATRON,
            launcherIcon = IR.mipmap.ic_launcher_patron_chrome,
            aliasName = ".ui.MainActivity_14"
        ),
        PATRON_ROUND(
            setting = AppIconSetting.PATRON_ROUND,
            labelId = LR.string.settings_app_icon_patron_round,
            settingsIcon = IR.drawable.appicon_patron_round,
            tier = SubscriptionTier.PATRON,
            launcherIcon = IR.mipmap.ic_launcher_patron_round,
            aliasName = ".ui.MainActivity_15"
        ),
        PATRON_GLOW(
            setting = AppIconSetting.PATRON_GLOW,
            labelId = LR.string.settings_app_icon_patron_glow,
            settingsIcon = IR.drawable.appicon_patron_glow,
            tier = SubscriptionTier.PATRON,
            launcherIcon = IR.mipmap.ic_launcher_patron_glow,
            aliasName = ".ui.MainActivity_16"
        ),
        PATRON_DARK(
            setting = AppIconSetting.PATRON_DARK,
            labelId = LR.string.settings_app_icon_patron_dark,
            settingsIcon = IR.drawable.appicon_patron_dark,
            tier = SubscriptionTier.PATRON,
            launcherIcon = IR.mipmap.ic_launcher_patron_dark,
            aliasName = ".ui.MainActivity_17"
        );

        companion object {
            fun fromSetting(setting: AppIconSetting) = when (setting) {
                AppIconSetting.DEFAULT -> DEFAULT
                AppIconSetting.DARK -> DARK
                AppIconSetting.ROUND_LIGHT -> ROUND_LIGHT
                AppIconSetting.ROUND_DARK -> ROUND_DARK
                AppIconSetting.INDIGO -> INDIGO
                AppIconSetting.ROSE -> ROSE
                AppIconSetting.CAT -> CAT
                AppIconSetting.REDVELVET -> REDVELVET
                AppIconSetting.PRIDE_2023 -> PRIDE_2023
                AppIconSetting.PLUS -> PLUS
                AppIconSetting.CLASSIC -> CLASSIC
                AppIconSetting.ELECTRIC_BLUE -> ELECTRIC_BLUE
                AppIconSetting.ELECTRIC_PINK -> ELECTRIC_PINK
                AppIconSetting.RADIOACTIVE -> RADIOACTIVE
                AppIconSetting.HALLOWEEN -> HALLOWEEN
                AppIconSetting.PATRON_CHROME -> PATRON_CHROME
                AppIconSetting.PATRON_ROUND -> PATRON_ROUND
                AppIconSetting.PATRON_GLOW -> PATRON_GLOW
                AppIconSetting.PATRON_DARK -> PATRON_DARK
            }
        }
    }

    var activeAppIcon: AppIconType = AppIconType.fromSetting(settings.appIcon.flow.value)
        set(value) {
            field = value
            settings.appIcon.set(value.setting)
        }

    val allAppIconTypes = AppIconType.values()

    fun enableSelectedAlias(selectedIconType: AppIconType) {
        val componentPackage =
            if (BuildConfig.DEBUG) "au.com.shiftyjelly.pocketcasts.debug" else "au.com.shiftyjelly.pocketcasts"
        val classPath = "au.com.shiftyjelly.pocketcasts"
        AppIconType.values().forEach { iconType ->
            val componentName = ComponentName(componentPackage, "$classPath${iconType.aliasName}")
            // If we are using the default icon we just switch every alias off
            val enabledFlag =
                if (selectedIconType == iconType && selectedIconType != AppIconType.DEFAULT) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            context.packageManager.setComponentEnabledSetting(
                componentName,
                enabledFlag,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
