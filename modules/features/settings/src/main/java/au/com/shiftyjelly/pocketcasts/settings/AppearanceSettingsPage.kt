package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.use
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.CloseButton
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

internal data class AppearanceSwitches(
    val useSystemTheme: Boolean,
    val showArtworkOnLockScreen: Boolean,
    val useEpisodeArtwork: Boolean,
    val useDarkUpNext: Boolean,
    val useDynamicColorsForWidget: Boolean,
)

internal interface AppearanceListener {
    fun onNavigationClick()
    fun onUseSystemThemeChange(checked: Boolean)
    fun onShowArtworkOnLockScreenChange(checked: Boolean)
    fun onUseEpisodeArtworkChange(checked: Boolean)
    fun onUseDarkUpNextChange(checked: Boolean)
    fun onUseDynamicColorsForWidgetChange(checked: Boolean)
    fun onEpisodeArtworkConfigurationClick()
    fun onRefreshArtworkClick()
    fun onUpgradeClick()
    fun onCloseUpgradeClick()
}

@Composable
internal fun AppearanceSettingsPage(
    switches: AppearanceSwitches,
    showWidgetSection: Boolean,
    showUpgradeBanner: Boolean,
    bottomInset: Dp,
    listener: AppearanceListener,
    themePicker: @Composable () -> Unit,
    appIconPicker: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    toolbarModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Column(modifier = modifier.fillMaxSize()) {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_appearance),
            navigationButton = NavigationButton.Back,
            onNavigationClick = listener::onNavigationClick,
            iconColor = colors.secondaryIcon01,
            textColor = colors.secondaryText01,
            backgroundColor = colors.secondaryUi01,
            modifier = toolbarModifier,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(colors.primaryUi04)
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset),
        ) {
            SettingSection(heading = stringResource(LR.string.settings_theme)) {
                themePicker()
                SwitchRow(
                    title = stringResource(LR.string.settings_use_android_light_dark_mode),
                    checked = switches.useSystemTheme,
                    onCheckedChange = listener::onUseSystemThemeChange,
                )
            }
            SettingSection(heading = stringResource(LR.string.settings_app_icon)) {
                appIconPicker()
            }
            SettingSection(heading = stringResource(LR.string.settings_podcast_artwork)) {
                SwitchRow(
                    title = stringResource(LR.string.settings_show_artwork),
                    details = stringResource(LR.string.settings_show_artwork_details),
                    checked = switches.showArtworkOnLockScreen,
                    onCheckedChange = listener::onShowArtworkOnLockScreenChange,
                )
                SwitchRow(
                    title = stringResource(LR.string.settings_use_episode_artwork),
                    details = stringResource(LR.string.settings_use_episode_artwork_details),
                    checked = switches.useEpisodeArtwork,
                    onCheckedChange = listener::onUseEpisodeArtworkChange,
                )
                SettingRow(
                    primaryText = stringResource(LR.string.settings_use_episode_artwork_advanced),
                    modifier = Modifier.clickable(onClick = listener::onEpisodeArtworkConfigurationClick),
                )
                SettingRow(
                    primaryText = stringResource(LR.string.settings_refresh_all_podcast_artwork),
                    modifier = Modifier.clickable(onClick = listener::onRefreshArtworkClick),
                )
            }
            SettingSection(heading = stringResource(LR.string.up_next)) {
                SwitchRow(
                    title = stringResource(LR.string.settings_use_dark_up_next),
                    details = stringResource(LR.string.settings_use_dark_up_next_details),
                    checked = switches.useDarkUpNext,
                    onCheckedChange = listener::onUseDarkUpNextChange,
                )
            }
            if (showWidgetSection) {
                SettingSection(heading = stringResource(LR.string.widget)) {
                    SwitchRow(
                        title = stringResource(LR.string.settings_use_dynamic_colors_widget),
                        details = stringResource(LR.string.settings_use_dynamic_colors_widget_details),
                        checked = switches.useDynamicColorsForWidget,
                        onCheckedChange = listener::onUseDynamicColorsForWidgetChange,
                    )
                }
            }
            if (showUpgradeBanner) {
                UpgradeBanner(listener)
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    details: String? = null,
) {
    SettingRow(
        primaryText = title,
        secondaryText = details,
        toggle = SettingRowToggle.Switch(checked = checked),
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    )
}

@Composable
private fun UpgradeBanner(listener: AppearanceListener) {
    val colors = MaterialTheme.theme.colors
    val context = LocalContext.current
    val logo = remember(context) {
        context.obtainStyledAttributes(intArrayOf(UR.attr.plus_logo)).use { it.getResourceId(0, 0) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = listener::onUpgradeClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(logo),
                contentDescription = stringResource(LR.string.pocket_casts_plus),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(width = 232.dp, height = 32.dp),
            )
            TextH40(
                text = stringResource(LR.string.settings_get_plus_and_more),
                color = colors.primaryText02,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 64.dp, top = 16.dp, end = 64.dp)
                    .fillMaxWidth(),
            )
            TextH50(
                text = stringResource(LR.string.settings_learn_more),
                color = colors.primaryInteractive01,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 64.dp, top = 16.dp, end = 64.dp)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(64.dp))
        }
        CloseButton(
            onClick = listener::onCloseUpgradeClick,
            tintColor = colors.primaryText02,
            contentDescription = stringResource(LR.string.settings_close_upgrade_offer),
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}
