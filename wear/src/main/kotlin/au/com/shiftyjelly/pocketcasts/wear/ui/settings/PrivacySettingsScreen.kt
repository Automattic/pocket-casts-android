package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object PrivacySettingsScreen {
    const val route = "analytics_settings_screen"
}

@Composable
fun PrivacySettingsScreen(
    scrollState: ScalingLazyColumnState,
) {
    val viewModel = hiltViewModel<PrivacySettingsViewModel>()
    val state by viewModel.state.collectAsState()
    Content(
        scrollState = scrollState,
        state = state,
        onAnalyticsChanged = viewModel::onAnalyticsChanged,
        onCrashReportingChanged = viewModel::onCrashReportingChanged,
        onLinkCrashReportsToUserChanged = viewModel::onLinkCrashReportsToUserChanged,
    )
}

@Composable
private fun Content(
    scrollState: ScalingLazyColumnState,
    state: PrivacySettingsViewModel.State,
    onAnalyticsChanged: (Boolean) -> Unit,
    onCrashReportingChanged: (Boolean) -> Unit,
    onLinkCrashReportsToUserChanged: (Boolean) -> Unit,
) {
    ScalingLazyColumn(columnState = scrollState) {

        item {
            ScreenHeaderChip(LR.string.settings_privacy_analytics)
        }

        item {
            DescriptionText(LR.string.settings_privacy_summary)
        }

        item {
            val analyticsLabel = stringResource(LR.string.settings_privacy_analytics)
            ToggleChip(
                label = analyticsLabel,
                checked = state.sendAnalytics,
                onCheckedChanged = onAnalyticsChanged,
            )
        }

        item {
            DescriptionText(LR.string.settings_privacy_analytics_summary)
        }

        item {
            val analyticsLabel = stringResource(LR.string.settings_privacy_crash)
            ToggleChip(
                label = analyticsLabel,
                checked = state.sendCrashReports,
                onCheckedChanged = onCrashReportingChanged,
            )
        }

        item {
            DescriptionText(LR.string.settings_privacy_crash_summary)
        }

        item {
            val analyticsLabel = stringResource(LR.string.settings_privacy_crash_link_short)
            ToggleChip(
                label = analyticsLabel,
                checked = state.linkCrashReportsToUser,
                onCheckedChanged = onLinkCrashReportsToUserChanged,
            )
        }

        item {
            DescriptionText(LR.string.settings_privacy_crash_link_summary)
        }
    }
}

@Composable
private fun DescriptionText(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.caption3,
        color = MaterialTheme.colors.onSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
private fun Preview() {
    WearAppTheme {
        Content(
            scrollState = ScalingLazyColumnState(),
            state = PrivacySettingsViewModel.State(
                sendAnalytics = true,
                sendCrashReports = true,
                linkCrashReportsToUser = false,
            ),
            onAnalyticsChanged = {},
            onCrashReportingChanged = {},
            onLinkCrashReportsToUserChanged = {},
        )
    }
}
