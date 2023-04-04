package au.com.shiftyjelly.pocketcasts.wear.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.theme.theme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ChipScreenHeader
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ChipSectionHeader
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object SettingsScreen {
    const val route = "settings_screen"
}

@Composable
fun SettingsScreen(
    scrollState: ScalingLazyColumnState,
    signInClick: () -> Unit,
) {

    val viewModel = hiltViewModel<SettingsViewModel>()
    val state by viewModel.state.collectAsState()

    Content(
        scrollState = scrollState,
        state = state,
        onWarnOnMeteredChanged = { viewModel.setWarnOnMeteredNetwork(it) },
        signInClick = signInClick,
        onSignOutClicked = viewModel::signOut,
    )
}

@Composable
private fun Content(
    scrollState: ScalingLazyColumnState,
    state: SettingsViewModel.State,
    onWarnOnMeteredChanged: (Boolean) -> Unit,
    signInClick: () -> Unit,
    onSignOutClicked: () -> Unit,
) {
    ScalingLazyColumn(columnState = scrollState) {

        item {
            ChipScreenHeader(LR.string.settings)
        }

        item {
            ToggleChip(
                label = stringResource(LR.string.settings_metered_data_warning),
                checked = state.showDataWarning,
                onCheckedChanged = onWarnOnMeteredChanged,
            )
        }

        item {
            ChipSectionHeader(LR.string.account)
        }

        item {
            val signInState = state.signInState
            when (signInState) {
                is SignInState.SignedIn -> {
                    WatchListChip(
                        titleRes = LR.string.log_out,
                        secondaryLabel = signInState.email,
                        iconRes = IR.drawable.ic_signout,
                        onClick = onSignOutClicked,
                    )
                }
                is SignInState.SignedOut -> {
                    WatchListChip(
                        titleRes = LR.string.log_in,
                        iconRes = IR.drawable.signin,
                        onClick = signInClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
) {
    val color = MaterialTheme.theme.colors.support05
    ToggleChip(
        checked = checked,
        onCheckedChange = { onCheckedChanged(it) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.button,
            )
        },
        toggleControl = {
            Icon(
                imageVector = ToggleChipDefaults.switchIcon(checked),
                contentDescription = stringResource(if (checked) LR.string.on else LR.string.off),
                modifier = Modifier
            )
        },
        colors = ToggleChipDefaults.toggleChipColors(
            checkedEndBackgroundColor = color.copy(alpha = 0.32f),
            checkedToggleControlColor = color,
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(
    widthDp = 200,
    heightDp = 200,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
)
@Composable
private fun SettingsScreenPreview_unchecked() {
    WearAppTheme(Theme.ThemeType.DARK) {
        Content(
            scrollState = ScalingLazyColumnState(),
            state = SettingsViewModel.State(
                signInState = SignInState.SignedIn(
                    email = "matt@pocketcasts.com",
                    subscriptionStatus = SubscriptionStatus.Free(),
                ),
                showDataWarning = false,
            ),
            signInClick = {},
            onWarnOnMeteredChanged = {},
            onSignOutClicked = {}

        )
    }
}

@Preview(
    widthDp = 200,
    heightDp = 200,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
)
@Composable
private fun SettingsScreenPreview_checked() {
    WearAppTheme(Theme.ThemeType.DARK) {
        Content(
            scrollState = ScalingLazyColumnState(),
            state = SettingsViewModel.State(
                signInState = SignInState.SignedIn(
                    email = "matt@pocketcasts.com",
                    subscriptionStatus = SubscriptionStatus.Free(),
                ),
                showDataWarning = true,
            ),
            signInClick = {},
            onWarnOnMeteredChanged = {},
            onSignOutClicked = {}

        )
    }
}
