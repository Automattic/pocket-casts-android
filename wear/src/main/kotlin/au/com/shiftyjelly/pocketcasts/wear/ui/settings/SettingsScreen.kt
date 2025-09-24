package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChipDefaults
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.SectionHeaderChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberColumnState
import com.google.android.horologist.compose.material.ToggleChip
import com.google.android.horologist.compose.material.ToggleChipToggleControl
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.settings.R as SR

object SettingsScreen {
    const val ROUTE = "settings_screen"
}

@Composable
fun SettingsScreen(
    signInClick: () -> Unit,
    navigateToPrivacySettings: () -> Unit,
    navigateToAbout: () -> Unit,
    navigateToHelp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberColumnState()

    ScreenScaffold(
        scrollState = scrollState,
        modifier = modifier,
    ) {
        Content(
            scrollState = scrollState,
            state = state,
            onWarnOnMeteredChange = { viewModel.setWarnOnMeteredNetwork(it) },
            onRefreshInBackgroundChange = { viewModel.setRefreshPodcastsInBackground(it) },
            signInClick = signInClick,
            onSignOutClick = viewModel::signOut,
            onRefreshClick = viewModel::refresh,
            onPrivacyClick = navigateToPrivacySettings,
            onAboutClick = navigateToAbout,
            onHelpClick = navigateToHelp,
        )
    }
}

@Composable
private fun Content(
    scrollState: ScalingLazyColumnState,
    state: SettingsViewModel.State,
    onWarnOnMeteredChange: (Boolean) -> Unit,
    onRefreshInBackgroundChange: (Boolean) -> Unit,
    signInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onAboutClick: () -> Unit,
    onHelpClick: () -> Unit,
) {
    ScalingLazyColumn(columnState = scrollState) {
        item {
            ScreenHeaderChip(LR.string.settings)
        }

        item {
            ToggleChip(
                label = stringResource(LR.string.settings_metered_data_warning),
                checked = state.showDataWarning,
                onToggle = onWarnOnMeteredChange,
            )
        }

        val backgroundRefreshStringRes = if (state.refreshInBackground) {
            LR.string.settings_storage_background_refresh_on_watch
        } else {
            LR.string.settings_storage_background_refresh_off_watch
        }

        item {
            ToggleChip(
                label = stringResource(LR.string.settings_storage_background_refresh),
                checked = state.refreshInBackground,
                onToggle = onRefreshInBackgroundChange,
            )
        }

        item {
            Text(
                text = stringResource(backgroundRefreshStringRes),
                style = MaterialTheme.typography.caption3,
                color = MaterialTheme.colors.onSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp),
            )
        }

        item {
            SectionHeaderChip(LR.string.account)
        }

        item {
            val title = stringResource(LR.string.profile_refresh_now)
            val rotation = rotationAnimation(
                state = state.refreshState,
                durationMillis = 800,
            )
            WatchListChip(
                title = title,
                icon = {
                    Icon(
                        painter = painterResource(IR.drawable.ic_retry),
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = rotation.value
                        },
                    )
                },
                onClick = onRefreshClick,
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.settings_privacy_analytics),
                iconRes = SR.drawable.whatsnew_privacy,
                onClick = onPrivacyClick,
            )
        }

        item {
            val signInState = state.signInState
            when (signInState) {
                is SignInState.SignedIn -> {
                    WatchListChip(
                        title = stringResource(LR.string.log_out),
                        secondaryLabel = signInState.email,
                        iconRes = IR.drawable.ic_signout,
                        onClick = onSignOutClick,
                    )
                }

                is SignInState.SignedOut -> {
                    WatchListChip(
                        title = stringResource(LR.string.log_in),
                        iconRes = IR.drawable.signin,
                        onClick = signInClick,
                    )
                }
            }
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.settings_title_help),
                iconRes = IR.drawable.ic_help,
                onClick = onHelpClick,
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.settings_title_about),
                iconRes = SR.drawable.settings_about,
                onClick = onAboutClick,
            )
        }
    }
}

@Composable
private fun rotationAnimation(state: RefreshState?, durationMillis: Int): Animatable<Float, AnimationVector1D> {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(state, anim.isRunning) {
        if (anim.value == 360f) {
            anim.snapTo(0f)
        }

        if (anim.value == 0f && state == RefreshState.Refreshing) {
            // We're at 0 and we're refreshing, so start animating

            anim.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis, easing = LinearEasing),
            )
        } else if (anim.value != 0f && state != RefreshState.Refreshing) {
            // No longer refreshing but we're not at 0 so continue animating until
            // we're back at 0 to keep things smooth (i.e., if a second refresh is
            // later initiated, we won't have a "jump" back to 0)

            val degreesLeft = 360 - anim.value
            val percentLeft = degreesLeft / 360f
            val timeLeft = (percentLeft * durationMillis).toInt()

            anim.animateTo(
                targetValue = 360f,
                animationSpec = tween(timeLeft, easing = LinearEasing),
            )
        }
    }
    return anim
}

@Composable
fun ToggleChip(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colors.error
    ToggleChip(
        checked = checked,
        onCheckedChanged = { onToggle(it) },
        label = label,
        toggleControl = ToggleChipToggleControl.Switch,
        colors = ToggleChipDefaults.toggleChipColors(
            checkedEndBackgroundColor = color.copy(alpha = 0.32f),
            checkedToggleControlColor = color,
        ),
        modifier = modifier,
    )
}

@Preview(
    widthDp = 200,
    heightDp = 200,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
)
@Composable
private fun SettingsScreenPreview_unchecked() {
    WearAppTheme {
        Content(
            scrollState = ScalingLazyColumnState(),
            state = SettingsViewModel.State(
                signInState = SignInState.SignedIn(
                    email = "matt@pocketcasts.com",
                    subscription = null,
                ),
                showDataWarning = false,
                refreshInBackground = false,
                refreshState = null,
            ),
            signInClick = {},
            onWarnOnMeteredChange = {},
            onRefreshInBackgroundChange = {},
            onSignOutClick = {},
            onRefreshClick = {},
            onPrivacyClick = {},
            onAboutClick = {},
            onHelpClick = {},
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
    WearAppTheme {
        Content(
            scrollState = ScalingLazyColumnState(),
            state = SettingsViewModel.State(
                signInState = SignInState.SignedIn(
                    email = "matt@pocketcasts.com",
                    subscription = null,
                ),
                showDataWarning = true,
                refreshInBackground = true,
                refreshState = null,
            ),
            signInClick = {},
            onWarnOnMeteredChange = {},
            onRefreshInBackgroundChange = {},
            onSignOutClick = {},
            onRefreshClick = {},
            onPrivacyClick = {},
            onAboutClick = {},
            onHelpClick = {},
        )
    }
}
