package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.annotation.SuppressLint
import android.widget.Switch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.GradientIcon
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.profile.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun CloudSettingsPage(
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CloudSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CloudSettingsContent(
        uiState = uiState,
        bottomInset = bottomInset,
        onBackPress = onBackPress,
        onUpgradeClick = onUpgradeClick,
        onAddToUpNextChange = viewModel::setAddToUpNext,
        onDeleteLocalFileChange = viewModel::setDeleteLocalFileAfterPlaying,
        onDeleteCloudFileChange = viewModel::setDeleteCloudFileAfterPlaying,
        onCloudAutoUploadChange = viewModel::setCloudAutoUpload,
        onCloudAutoDownloadChange = viewModel::setCloudAutoDownload,
        onCloudOnlyOnWifiChange = viewModel::setCloudOnlyWifi,
        onUpgradeBannerDismiss = { viewModel.onUpgradeBannerDismissed(SourceView.FILES_SETTINGS) },
        modifier = modifier,
    )
}

@Composable
private fun CloudSettingsContent(
    uiState: CloudSettingsViewModel.UiState,
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onUpgradeClick: () -> Unit,
    onAddToUpNextChange: (Boolean) -> Unit,
    onDeleteLocalFileChange: (Boolean) -> Unit,
    onDeleteCloudFileChange: (Boolean) -> Unit,
    onCloudAutoUploadChange: (Boolean) -> Unit,
    onCloudAutoDownloadChange: (Boolean) -> Unit,
    onCloudOnlyOnWifiChange: (Boolean) -> Unit,
    onUpgradeBannerDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ThemedTopAppBar(
            title = stringResource(LR.string.profile_cloud_settings_title),
            onNavigationClick = onBackPress,
            windowInsets = WindowInsets.statusBars,
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset),
        ) {
            FileSettingsSection(
                uiState = uiState,
                onAddToUpNextChange = onAddToUpNextChange,
                onDeleteLocalFileChange = onDeleteLocalFileChange,
                onDeleteCloudFileChange = onDeleteCloudFileChange,
            )
            PlusFeaturesSection(
                uiState = uiState,
                onCloudAutoUploadChange = onCloudAutoUploadChange,
                onCloudAutoDownloadChange = onCloudAutoDownloadChange,
                onCloudOnlyOnWifiChange = onCloudOnlyOnWifiChange,
                onUpgradeClick = onUpgradeClick,
            )
            if (uiState.isUpgradeBannerVisible) {
                UpgradeBanner(
                    onUpgradeClick = onUpgradeClick,
                    onDismiss = onUpgradeBannerDismiss,
                )
            }
        }
    }
}

@Composable
private fun FileSettingsSection(
    uiState: CloudSettingsViewModel.UiState,
    onAddToUpNextChange: (Boolean) -> Unit,
    onDeleteLocalFileChange: (Boolean) -> Unit,
    onDeleteCloudFileChange: (Boolean) -> Unit,
) {
    Column {
        SwitchRow(
            primaryText = stringResource(LR.string.profile_cloud_auto_add_to_up_next),
            secondaryText = stringResource(LR.string.profile_cloud_all_files_added),
            checked = uiState.cloudAddToUpNext,
            onCheckedChange = onAddToUpNextChange,
            modifier = Modifier.padding(top = 32.dp),
        )
        SettingsText(
            text = stringResource(LR.string.profile_cloud_after_playing),
            color = MaterialTheme.theme.colors.primaryInteractive01,
            modifier = Modifier.padding(start = 72.dp, top = 32.dp),
        )
        SwitchRow(
            primaryText = stringResource(LR.string.profile_cloud_delete_local_file),
            checked = uiState.deleteLocalFileAfterPlaying,
            onCheckedChange = onDeleteLocalFileChange,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (uiState.isSignedInAsPlusOrPatron) {
            SwitchRow(
                primaryText = stringResource(LR.string.profile_cloud_delete_cloud_file),
                checked = uiState.deleteCloudFileAfterPlaying,
                onCheckedChange = onDeleteCloudFileChange,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        Divider(
            color = MaterialTheme.theme.colors.primaryUi05,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun PlusFeaturesSection(
    uiState: CloudSettingsViewModel.UiState,
    onCloudAutoUploadChange: (Boolean) -> Unit,
    onCloudAutoDownloadChange: (Boolean) -> Unit,
    onCloudOnlyOnWifiChange: (Boolean) -> Unit,
    onUpgradeClick: () -> Unit,
) {
    val isPlus = uiState.isSignedInAsPlusOrPatron
    Box {
        if (!isPlus) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(interactionSource = null, indication = null, onClick = onUpgradeClick),
            )
        }
        Column(
            modifier = Modifier.alpha(if (isPlus) 1f else 0.5f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 72.dp, top = 16.dp),
            ) {
                SettingsText(
                    text = stringResource(LR.string.profile_cloud_plus_features),
                    color = MaterialTheme.theme.colors.primaryInteractive01,
                )
                if (!isPlus) {
                    GradientIcon(
                        painter = painterResource(R.drawable.ic_lock),
                        colors = listOf(
                            colorResource(UR.color.plus_gold_dark),
                            colorResource(UR.color.plus_gold_light),
                        ),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(width = 12.dp, height = 16.dp),
                    )
                }
            }
            SwitchRow(
                primaryText = stringResource(LR.string.profile_cloud_auto_upload_to_cloud),
                secondaryText = stringResource(LR.string.profile_cloud_files_added_uploaded),
                checked = uiState.cloudAutoUpload,
                enabled = isPlus,
                onCheckedChange = onCloudAutoUploadChange,
                modifier = Modifier.padding(top = 16.dp),
            )
            SwitchRow(
                primaryText = stringResource(LR.string.profile_cloud_auto_download_from_cloud),
                secondaryText = stringResource(LR.string.profile_cloud_files_added_downloaded),
                checked = uiState.cloudAutoDownload,
                enabled = isPlus,
                onCheckedChange = onCloudAutoDownloadChange,
                modifier = Modifier.padding(top = 16.dp),
            )
            SwitchRow(
                primaryText = stringResource(LR.string.profile_cloud_only_on_wifi),
                checked = uiState.cloudDownloadOnlyOnWifi,
                enabled = isPlus,
                onCheckedChange = onCloudOnlyOnWifiChange,
                includeSwitchHeight = true,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun UpgradeBanner(
    onUpgradeClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Divider(color = MaterialTheme.theme.colors.secondaryUi02)
            val logoResource = if (MaterialTheme.theme.isLight) {
                IR.drawable.plus_logo_horizontal_light
            } else {
                IR.drawable.plus_logo_horizontal_dark
            }
            Image(
                painter = painterResource(logoResource),
                contentDescription = stringResource(LR.string.pocket_casts_plus),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(width = 232.dp, height = 32.dp)
                    .clickable(interactionSource = null, indication = null, onClick = onUpgradeClick),
            )
            SettingsText(
                text = stringResource(LR.string.profile_get_plus),
                color = MaterialTheme.theme.colors.primaryText02,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 64.dp, end = 64.dp, top = 16.dp)
                    .fillMaxWidth()
                    .clickable(interactionSource = null, indication = null, onClick = onUpgradeClick),
            )
            SettingsText(
                text = stringResource(LR.string.plus_learn_more_button),
                color = MaterialTheme.theme.colors.primaryInteractive01,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 64.dp, end = 64.dp, top = 16.dp)
                    .fillMaxWidth()
                    .clickable(interactionSource = null, indication = null, onClick = onUpgradeClick),
            )
            Spacer(Modifier.height(64.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(57.dp),
        ) {
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(248.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_close),
                    contentDescription = stringResource(LR.string.close),
                    tint = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    primaryText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    enabled: Boolean = true,
    includeSwitchHeight: Boolean = false,
) {
    Layout(
        content = {
            Column {
                SettingsText(
                    text = primaryText,
                    color = MaterialTheme.theme.colors.primaryText01,
                )
                if (secondaryText != null) {
                    SettingsText(
                        text = secondaryText,
                        color = MaterialTheme.theme.colors.primaryText02,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            LegacySwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val startPadding = 72.dp.roundToPx()
        val textEndPadding = 16.dp.roundToPx()
        val switchEndPadding = 32.dp.roundToPx()
        val switch = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val textWidth = (constraints.maxWidth - startPadding - textEndPadding - switch.width - switchEndPadding).coerceAtLeast(0)
        val text = measurables[0].measure(constraints.copy(minWidth = textWidth, maxWidth = textWidth, minHeight = 0))
        val height = if (includeSwitchHeight) maxOf(text.height, switch.height) else text.height
        layout(constraints.maxWidth, height) {
            text.place(startPadding, 0)
            switch.place(constraints.maxWidth - switchEndPadding - switch.width, 0)
        }
    }
}

@SuppressLint("UseSwitchCompatOrMaterialCode")
@Composable
private fun LegacySwitch(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    AndroidView(
        factory = { context -> Switch(context) },
        update = { switch ->
            switch.setOnCheckedChangeListener(null)
            switch.isChecked = checked
            switch.isEnabled = enabled
            switch.setOnCheckedChangeListener { _, isChecked -> onCheckedChange(isChecked) }
        },
    )
}

@Composable
private fun SettingsText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        textAlign = textAlign,
        style = LocalTextStyle.current.copy(
            lineHeight = TextUnit.Unspecified,
            lineBreak = LineBreak.Paragraph,
            platformStyle = PlatformTextStyle(includeFontPadding = true),
        ),
        modifier = modifier,
    )
}

@Composable
private fun Divider(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

@PreviewRegularDevice
@Composable
private fun CloudSettingsPagePlusPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        CloudSettingsContent(
            uiState = CloudSettingsViewModel.UiState(
                cloudAddToUpNext = true,
                deleteLocalFileAfterPlaying = true,
                deleteCloudFileAfterPlaying = false,
                cloudAutoUpload = true,
                cloudAutoDownload = false,
                cloudDownloadOnlyOnWifi = true,
                isSignedInAsPlusOrPatron = true,
                isUpgradeBannerVisible = false,
            ),
            bottomInset = 0.dp,
            onBackPress = {},
            onUpgradeClick = {},
            onAddToUpNextChange = {},
            onDeleteLocalFileChange = {},
            onDeleteCloudFileChange = {},
            onCloudAutoUploadChange = {},
            onCloudAutoDownloadChange = {},
            onCloudOnlyOnWifiChange = {},
            onUpgradeBannerDismiss = {},
        )
    }
}

@PreviewRegularDevice
@Composable
private fun CloudSettingsPageFreePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        CloudSettingsContent(
            uiState = CloudSettingsViewModel.UiState(
                cloudAddToUpNext = true,
                deleteLocalFileAfterPlaying = false,
                deleteCloudFileAfterPlaying = false,
                cloudAutoUpload = false,
                cloudAutoDownload = false,
                cloudDownloadOnlyOnWifi = false,
                isSignedInAsPlusOrPatron = false,
                isUpgradeBannerVisible = true,
            ),
            bottomInset = 0.dp,
            onBackPress = {},
            onUpgradeClick = {},
            onAddToUpNextChange = {},
            onDeleteLocalFileChange = {},
            onDeleteCloudFileChange = {},
            onCloudAutoUploadChange = {},
            onCloudAutoDownloadChange = {},
            onCloudOnlyOnWifiChange = {},
            onUpgradeBannerDismiss = {},
        )
    }
}
