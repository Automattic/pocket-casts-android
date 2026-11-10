package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSectionHeader
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ExportSettingsPage(
    bottomInset: Dp,
    onNavigationClick: () -> Unit,
    onImportFileClick: () -> Unit,
    onImportUrlClick: () -> Unit,
    onExportEmailClick: () -> Unit,
    onExportFileClick: () -> Unit,
    modifier: Modifier = Modifier,
    toolbarModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = bottomInset),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_import_export),
            navigationButton = NavigationButton.Back,
            onNavigationClick = onNavigationClick,
            iconColor = MaterialTheme.theme.colors.secondaryIcon01,
            textColor = MaterialTheme.theme.colors.secondaryText01,
            backgroundColor = MaterialTheme.theme.colors.secondaryUi01,
            modifier = toolbarModifier,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.theme.colors.primaryUi02)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingSection {
                SectionHeading(stringResource(LR.string.settings_import_title))
                ExportRow(
                    title = stringResource(LR.string.settings_import_file),
                    summary = stringResource(LR.string.settings_import_file_summary),
                    onClick = onImportFileClick,
                )
                ExportRow(
                    title = stringResource(LR.string.settings_import_url),
                    summary = stringResource(LR.string.settings_import_url_summary),
                    onClick = onImportUrlClick,
                )
            }
            SettingSection(showDivider = false) {
                SectionHeading(stringResource(LR.string.settings_export_subscriptions))
                ExportRow(
                    title = stringResource(LR.string.settings_export_send_email),
                    summary = stringResource(LR.string.settings_export_send_email_summary),
                    onClick = onExportEmailClick,
                )
                ExportRow(
                    title = stringResource(LR.string.settings_export_save_file),
                    summary = stringResource(LR.string.settings_export_save_file_summary),
                    onClick = onExportFileClick,
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    SettingSectionHeader(
        text = text,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun ExportRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    SettingRow(
        primaryText = title,
        secondaryText = summary,
        modifier = Modifier.clickable(onClick = onClick),
    )
}
