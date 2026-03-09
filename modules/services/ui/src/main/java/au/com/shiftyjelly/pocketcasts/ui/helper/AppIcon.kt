package au.com.shiftyjelly.pocketcasts.ui.helper

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AppIconSetting
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import com.automattic.eventhorizon.AppIconType as EventHorizonAppIconType

@Singleton
class AppIcon @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: Settings,
) {

    enum class AppIconType(
        internal val setting: AppIconSetting,
        @StringRes val labelId: Int,
        @DrawableRes val settingsIcon: Int,
        val tier: SubscriptionTier?,
        @DrawableRes val launcherIcon: Int,
        val aliasName: String,
        val eventHorizonValue: EventHorizonAppIconType,
    ) {
        DEFAULT(
            setting = AppIconSetting.DEFAULT,
            labelId = LR.string.settings_app_icon_default,
            settingsIcon = IR.drawable.ic_appicon0,
            tier = null,
            launcherIcon = IR.mipmap.ic_launcher,
            aliasName = ".ui.MainActivity_0",
            eventHorizonValue = EventHorizonAppIconType.Default,
        ),
        DARK(
            setting = AppIconSetting.DARK,
            labelId = LR.string.settings_app_icon_dark,
            settingsIcon = IR.drawable.ic_appicon1,
            tier = null,
            launcherIcon = IR.mipmap.ic_launcher_1,
            aliasName = ".ui.MainActivity_1",
            eventHorizonValue = EventHorizonAppIconType.Dark,
        ),
        ROUND_LIGHT(
            setting = AppIconSetting.ROUND_LIGHT,
            labelId = LR.string.settings_app_icon_round_light,
            settingsIcon = IR.drawable.ic_appicon2,
            tier = null,
            launcherIcon = IR.mipmap.ic_launcher_2,
            aliasName = ".ui.MainActivity_2",
            eventHorizonValue = EventHorizonAppIconType.RoundLight,
        ),
        ROUND_DARK(
            setting = AppIconSetting.ROUND_DARK,
            labelId = LR.string.settings_app_icon_round_dark,
            settingsIcon = IR.drawable.ic_appicon3,
            tier = null,
            launcherIcon = IR.mipmap.ic_launcher_3,
            aliasName = ".ui.MainActivity_3",
            eventHorizonValue = EventHorizonAppIconType.RoundDark,
        ),
        INDIGO(
            setting = AppIconSetting.INDIGO,
            labelId = LR.string.settings_app_icon_indigo,
            settingsIcon = IR.drawable.ic_appicon_indigo,
            tier = null,
            launcherIcon = IR.mipmap.ic_launcher_indigo,
            aliasName = ".ui.MainActivity_9",
            eventHorizonValue = EventHorizonAppIconType.Indigo,
        ),
        ROSE(
            setting = AppIconSetting.ROSE,
            labelId = LR.string.settings_app_icon_rose,
            settingsIcon = IR.drawable.appicon_rose,
            tier = null,
            launcherIcon = IR.mipmap.ic_launcher_rose,
            aliasName = ".ui.MainActivity_12",
            eventHorizonValue = EventHorizonAppIconType.Rose,
        ),
        CAT(
            setting = AppIconSetting.CAT,
            labelId = LR.string.settings_app_icon_pocket_cats,
            settingsIcon = IR.drawable.ic_appicon_pocket_cats,
            tier = null,
            launcherIcon = IR.mipmap.ic_launcher_cat,
            aliasName = ".ui.MainActivity_10",
            eventHorizonValue = EventHorizonAppIconType.PocketCats,
        ),
        REDVELVET(
            setting = AppIconSetting.REDVELVET,
            labelId = LR.string.settings_app_icon_red_velvet,
            settingsIcon = IR.drawable.appicon_red_velvet,
            tier = null,
            launcherIcon = IR.mipmap.ic_launcher_redvelvet,
            aliasName = ".ui.MainActivity_11",
            eventHorizonValue = EventHorizonAppIconType.RedVelvet,
        ),
        PRIDE(
            setting = AppIconSetting.PRIDE,
            labelId = LR.string.settings_app_icon_pride,
            settingsIcon = IR.drawable.appicon_pride,
            tier = null,
            launcherIcon = IR.mipmap.ic_launcher_pride,
            aliasName = ".ui.MainActivity_18",
            eventHorizonValue = EventHorizonAppIconType.Pride2023,
        ),
        PLUS(
            setting = AppIconSetting.PLUS,
            labelId = LR.string.settings_app_icon_plus,
            settingsIcon = IR.drawable.ic_appicon4,
            tier = SubscriptionTier.Plus,
            launcherIcon = IR.mipmap.ic_launcher_4,
            aliasName = ".ui.MainActivity_4",
            eventHorizonValue = EventHorizonAppIconType.Plus,
        ),
        CLASSIC(
            setting = AppIconSetting.CLASSIC,
            labelId = LR.string.settings_app_icon_classic,
            settingsIcon = IR.drawable.ic_appicon5,
            tier = SubscriptionTier.Plus,
            launcherIcon = IR.mipmap.ic_launcher_5,
            aliasName = ".ui.MainActivity_5",
            eventHorizonValue = EventHorizonAppIconType.Classic,
        ),
        ELECTRIC_BLUE(
            setting = AppIconSetting.ELECTRIC_BLUE,
            labelId = LR.string.settings_app_icon_electric_blue,
            settingsIcon = IR.drawable.ic_appicon6,
            tier = SubscriptionTier.Plus,
            launcherIcon = IR.mipmap.ic_launcher_6,
            aliasName = ".ui.MainActivity_6",
            eventHorizonValue = EventHorizonAppIconType.ElectricBlue,
        ),
        ELECTRIC_PINK(
            setting = AppIconSetting.ELECTRIC_PINK,
            labelId = LR.string.settings_app_icon_electric_pink,
            settingsIcon = IR.drawable.ic_appicon7,
            tier = SubscriptionTier.Plus,
            launcherIcon = IR.mipmap.ic_launcher_7,
            aliasName = ".ui.MainActivity_7",
            eventHorizonValue = EventHorizonAppIconType.ElectricPink,
        ),
        RADIOACTIVE(
            setting = AppIconSetting.RADIOACTIVE,
            labelId = LR.string.settings_app_icon_radioactivity,
            settingsIcon = IR.drawable.appicon_radioactive,
            tier = SubscriptionTier.Plus,
            launcherIcon = IR.mipmap.ic_launcher_radioactive,
            aliasName = ".ui.MainActivity_8",
            eventHorizonValue = EventHorizonAppIconType.Radioactive,
        ),
        HALLOWEEN(
            setting = AppIconSetting.HALLOWEEN,
            labelId = LR.string.settings_app_icon_halloween,
            settingsIcon = IR.drawable.appicon_halloween,
            tier = SubscriptionTier.Plus,
            launcherIcon = IR.mipmap.ic_launcher_halloween,
            aliasName = ".ui.MainActivity_13",
            eventHorizonValue = EventHorizonAppIconType.Halloween,
        ),
        PATRON_CHROME(
            setting = AppIconSetting.PATRON_CHROME,
            labelId = LR.string.settings_app_icon_patron_chrome,
            settingsIcon = IR.drawable.appicon_patron_chrome,
            tier = SubscriptionTier.Patron,
            launcherIcon = IR.mipmap.ic_launcher_patron_chrome,
            aliasName = ".ui.MainActivity_14",
            eventHorizonValue = EventHorizonAppIconType.PatronChrome,
        ),
        PATRON_ROUND(
            setting = AppIconSetting.PATRON_ROUND,
            labelId = LR.string.settings_app_icon_patron_round,
            settingsIcon = IR.drawable.appicon_patron_round,
            tier = SubscriptionTier.Patron,
            launcherIcon = IR.mipmap.ic_launcher_patron_round,
            aliasName = ".ui.MainActivity_15",
            eventHorizonValue = EventHorizonAppIconType.PatronRound,
        ),
        PATRON_GLOW(
            setting = AppIconSetting.PATRON_GLOW,
            labelId = LR.string.settings_app_icon_patron_glow,
            settingsIcon = IR.drawable.appicon_patron_glow,
            tier = SubscriptionTier.Patron,
            launcherIcon = IR.mipmap.ic_launcher_patron_glow,
            aliasName = ".ui.MainActivity_16",
            eventHorizonValue = EventHorizonAppIconType.PatronGlow,
        ),
        PATRON_DARK(
            setting = AppIconSetting.PATRON_DARK,
            labelId = LR.string.settings_app_icon_patron_dark,
            settingsIcon = IR.drawable.appicon_patron_dark,
            tier = SubscriptionTier.Patron,
            launcherIcon = IR.mipmap.ic_launcher_patron_dark,
            aliasName = ".ui.MainActivity_17",
            eventHorizonValue = EventHorizonAppIconType.PatronDark,
        ),
        ;

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
                AppIconSetting.PRIDE -> PRIDE
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

    var activeAppIcon: AppIconType = AppIconType.fromSetting(settings.appIcon.value)
        set(value) {
            field = value
            settings.appIcon.set(value.setting, updateModifiedAt = false)
        }

    val allAppIconTypes get() = AppIconType.entries

    fun enableSelectedAlias(selectedIconType: AppIconType) {
        val classPath = "au.com.shiftyjelly.pocketcasts"
        AppIconType.entries.forEach { iconType ->
            val componentName = ComponentName(context.packageName, "$classPath${iconType.aliasName}")
            // If we are using the default icon we just switch every alias off
            val enabledFlag = if (selectedIconType == iconType && selectedIconType != AppIconType.DEFAULT) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            context.packageManager.setComponentEnabledSetting(
                componentName,
                enabledFlag,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
