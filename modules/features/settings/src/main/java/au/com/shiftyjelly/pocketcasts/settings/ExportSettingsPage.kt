package au.com.shiftyjelly.pocketcasts.settings

import androidx.annotation.AttrRes
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.use
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
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
            PreferenceCategory(title = stringResource(LR.string.settings_import_title))
            PreferenceRow(
                title = stringResource(LR.string.settings_import_file),
                summary = stringResource(LR.string.settings_import_file_summary),
                onClick = onImportFileClick,
            )
            PreferenceRow(
                title = stringResource(LR.string.settings_import_url),
                summary = stringResource(LR.string.settings_import_url_summary),
                onClick = onImportUrlClick,
            )
            PreferenceCategory(
                title = stringResource(LR.string.settings_export_subscriptions),
                hasDividerAbove = true,
            )
            PreferenceRow(
                title = stringResource(LR.string.settings_export_send_email),
                summary = stringResource(LR.string.settings_export_send_email_summary),
                onClick = onExportEmailClick,
            )
            PreferenceRow(
                title = stringResource(LR.string.settings_export_save_file),
                summary = stringResource(LR.string.settings_export_save_file_summary),
                onClick = onExportFileClick,
            )
        }
    }
}

@Composable
private fun PreferenceCategory(
    title: String,
    hasDividerAbove: Boolean = false,
) {
    val context = LocalContext.current
    val divider = remember(context) {
        context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider)).use { it.getDrawable(0) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hasDividerAbove && divider != null) {
                    Modifier.drawBehind {
                        divider.setBounds(0, 0, size.width.toInt(), divider.intrinsicHeight)
                        divider.draw(drawContext.canvas.nativeCanvas)
                    }
                } else {
                    Modifier
                },
            )
            .padding(top = 16.dp)
            .padding(start = 72.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
    ) {
        TextH50(
            text = title,
            color = rememberThemeColor(androidx.appcompat.R.attr.colorAccent),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
        )
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 72.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        TextP40(
            text = title,
        )
        TextP50(
            text = summary,
            color = rememberThemeColor(android.R.attr.textColorSecondary),
            maxLines = 10,
        )
    }
}

@Composable
private fun rememberThemeColor(@AttrRes attr: Int): Color {
    val context = LocalContext.current
    return remember(context, attr) {
        context.obtainStyledAttributes(intArrayOf(attr)).use { Color(it.getColor(0, 0)) }
    }
}
