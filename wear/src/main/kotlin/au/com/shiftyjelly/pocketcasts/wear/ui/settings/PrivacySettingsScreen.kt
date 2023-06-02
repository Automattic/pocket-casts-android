package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
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
                onCheckedChanged = viewModel::onAnalyticsChanged,
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
                onCheckedChanged = viewModel::onCrashReportingChanged,
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
                onCheckedChanged = viewModel::onLinkCrashReportsToUserChanged,
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
