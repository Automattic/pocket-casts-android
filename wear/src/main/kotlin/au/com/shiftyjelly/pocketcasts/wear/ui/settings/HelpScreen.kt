package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object HelpScreen {
    const val route = "help_screen"
}

@Composable
fun HelpScreen(columnState: ScalingLazyColumnState) {

    val viewModel = hiltViewModel<HelpScreenViewModel>()
    val state = viewModel.state.collectAsState().value
    val context = LocalContext.current

    ScalingLazyColumn(columnState = columnState) {
        item {
            ScreenHeaderChip(text = LR.string.settings_title_help)
        }

        if (state == null) {
            return@ScalingLazyColumn
        } else if (state.isPhoneAvailable) {
            phoneAvailableContent(
                onEmailLogsToSupport = { viewModel.emailLogsToSupport(context) },
                onSendLogsToPhone = { viewModel.sendLogsToPhone(context) },
            )
        } else {
            noPhoneAvailableContent()
        }
    }
}

private fun ScalingLazyListScope.phoneAvailableContent(
    onEmailLogsToSupport: () -> Unit,
    onSendLogsToPhone: () -> Unit,
) {
    item {
        Text(
            text = stringResource(id = LR.string.settings_help_contact_support_wear_requires_nearby_phone),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption3,
            color = MaterialTheme.colors.onSecondary,
        )
    }

    item {
        Spacer(Modifier.height(8.dp))
    }

    item {
        WatchListChip(
            title = stringResource(LR.string.settings_help_contact_support),
            onClick = onEmailLogsToSupport,
        )
    }

    item {
        WatchListChip(
            title = stringResource(LR.string.settings_help_send_logs_to_phone),
            onClick = onSendLogsToPhone,
        )
    }
}

private fun ScalingLazyListScope.noPhoneAvailableContent() {
    item {
        Text(
            text = stringResource(id = LR.string.settings_help_contact_support_no_phone_connection),
            textAlign = TextAlign.Center,
        )
    }

    item {
        // Make sure that the bottom of the text can be scrolled onto the screen of a circular watch
        Spacer(Modifier.height(24.dp))
    }
}
