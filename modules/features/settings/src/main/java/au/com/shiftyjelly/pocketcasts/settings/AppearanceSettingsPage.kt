package au.com.shiftyjelly.pocketcasts.settings

import android.annotation.SuppressLint
import android.view.View
import android.widget.Switch
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
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.use
import au.com.shiftyjelly.pocketcasts.compose.bars.AppCompatTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR
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
        AppCompatTopAppBar(
            title = stringResource(LR.string.settings_title_appearance),
            navigationButton = NavigationButton.Drawable(IR.drawable.ic_arrow_back, LR.string.back),
            onNavigationClick = listener::onNavigationClick,
            iconColor = colors.secondaryIcon01,
            titleColor = colors.secondaryText01,
            backgroundColor = colors.secondaryUi01,
            bottomShadow = false,
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
            SectionHeader(LR.string.settings_theme)
            Box(Modifier.padding(top = 20.dp)) { themePicker() }
            SwitchRow(
                title = stringResource(LR.string.settings_use_android_light_dark_mode),
                checked = switches.useSystemTheme,
                onCheckedChange = listener::onUseSystemThemeChange,
                titlePaddingBottom = 16.dp,
                titleEndMargin = 0.dp,
                titlePaddingTop = 16.dp,
                switchCentered = true,
                switchMarginTop = 4.dp,
            )
            Divider(top = 16.dp)
            SectionHeader(LR.string.settings_app_icon)
            Box(Modifier.padding(top = 20.dp)) { appIconPicker() }
            Divider(top = 0.dp)
            SectionHeader(LR.string.settings_podcast_artwork, paddingBottom = 8.dp)
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
            TextRow(LR.string.settings_use_episode_artwork_advanced, listener::onEpisodeArtworkConfigurationClick)
            TextRow(LR.string.settings_refresh_all_podcast_artwork, listener::onRefreshArtworkClick)
            Divider(top = 16.dp)
            SectionHeader(LR.string.up_next)
            SwitchRow(
                title = stringResource(LR.string.settings_use_dark_up_next),
                details = stringResource(LR.string.settings_use_dark_up_next_details),
                checked = switches.useDarkUpNext,
                onCheckedChange = listener::onUseDarkUpNextChange,
                topMargin = 8.dp,
                titlePaddingTop = 16.dp,
                switchCentered = true,
                switchMarginTop = 12.dp,
            )
            if (showWidgetSection) {
                Divider(top = 16.dp)
                SectionHeader(LR.string.widget)
                SwitchRow(
                    title = stringResource(LR.string.settings_use_dynamic_colors_widget),
                    details = stringResource(LR.string.settings_use_dynamic_colors_widget_details),
                    checked = switches.useDynamicColorsForWidget,
                    onCheckedChange = listener::onUseDynamicColorsForWidgetChange,
                    topMargin = 8.dp,
                    titlePaddingTop = 16.dp,
                    switchCentered = true,
                    switchMarginTop = 12.dp,
                )
            }
            Divider(top = 16.dp)
            if (showUpgradeBanner) {
                UpgradeBanner(listener)
            }
        }
    }
}

@Composable
private fun SectionHeader(titleRes: Int, paddingBottom: Dp = 0.dp) {
    TextViewText(
        text = stringResource(titleRes),
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.theme.colors.primaryInteractive01,
        modifier = Modifier.padding(start = 72.dp, top = 16.dp, bottom = paddingBottom),
    )
}

@Composable
private fun Divider(top: Dp) {
    Box(
        modifier = Modifier
            .padding(top = top)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.theme.colors.primaryUi05),
    )
}

@Composable
private fun TextRow(titleRes: Int, onClick: () -> Unit) {
    TextViewText(
        text = stringResource(titleRes),
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.theme.colors.primaryText01,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 72.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
    )
}

@SuppressLint("UseSwitchCompatOrMaterialCode")
@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    details: String? = null,
    topMargin: Dp = 0.dp,
    titlePaddingTop: Dp = 8.dp,
    titlePaddingBottom: Dp = 0.dp,
    titleEndMargin: Dp = 16.dp,
    switchMarginTop: Dp = 4.dp,
    switchCentered: Boolean = false,
) {
    val colors = MaterialTheme.theme.colors
    Layout(
        content = {
            TextViewText(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.primaryText01,
                modifier = Modifier.padding(top = titlePaddingTop, bottom = titlePaddingBottom),
            )
            if (details != null) {
                TextViewText(
                    text = details,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.primaryText02,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else {
                Box(Modifier)
            }
            AndroidView(
                factory = { context ->
                    Switch(context).apply {
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    }
                },
                update = { switch ->
                    switch.setOnCheckedChangeListener(null)
                    switch.isChecked = checked
                    switch.setOnCheckedChangeListener { _, isChecked -> onCheckedChange(isChecked) }
                },
            )
        },
        modifier = Modifier
            .padding(top = topMargin)
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val start = 72.dp.roundToPx()
        val switch = measurables[2].measure(Constraints())
        val switchStart = width - 24.dp.roundToPx() - switch.width
        val titleWidth = switchStart - titleEndMargin.roundToPx() - start
        val title = measurables[0].measure(Constraints.fixedWidth(titleWidth))
        val detailsWidth = switchStart - 16.dp.roundToPx() - start
        val detailsPlaceable = measurables[1].measure(Constraints(maxWidth = detailsWidth))
        val detailsTop = title.height + if (details != null) 8.dp.roundToPx() else 0
        val height = if (details != null) detailsTop + detailsPlaceable.height else title.height
        val marginTop = switchMarginTop.roundToPx()
        val switchY = if (switchCentered) marginTop + ((title.height - marginTop - switch.height) * 0.5f).roundToInt() else marginTop
        layout(width, height) {
            title.place(start, 0)
            detailsPlaceable.place(start, detailsTop)
            switch.place(switchStart, switchY)
        }
    }
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
            DrawableImage(
                drawableId = logo,
                contentDescription = stringResource(LR.string.pocket_casts_plus),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(width = 232.dp, height = 32.dp),
            )
            TextViewText(
                text = stringResource(LR.string.settings_get_plus_and_more),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.primaryText02,
                lineSpacingMultiplier = 1.1f,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 64.dp, top = 16.dp, end = 64.dp)
                    .fillMaxWidth(),
            )
            TextViewText(
                text = stringResource(LR.string.settings_learn_more),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.primaryInteractive01,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 64.dp, top = 16.dp, end = 64.dp)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(64.dp))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp)
                .size(32.dp)
                .clickable(interactionSource = null, indication = ripple(bounded = false), role = Role.Button, onClick = listener::onCloseUpgradeClick),
        ) {
            DrawableImage(
                drawableId = IR.drawable.ic_close,
                contentDescription = stringResource(LR.string.settings_close_upgrade_offer),
                tint = colors.primaryText02,
            )
        }
    }
}
