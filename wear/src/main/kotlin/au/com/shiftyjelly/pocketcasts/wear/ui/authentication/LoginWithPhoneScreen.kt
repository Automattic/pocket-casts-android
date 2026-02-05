package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearColors
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.material.Chip
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun LoginWithPhoneScreen(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    syncState: WatchSyncState? = null,
    onRetry: () -> Unit = {},
) {
    when (syncState) {
        null -> LoginWithPhoneInstructionsContent(
            onLoginClick = onLoginClick,
            modifier = modifier,
        )

        WatchSyncState.Syncing -> SyncingContent(modifier = modifier)

        WatchSyncState.Success -> SuccessContent(modifier = modifier)

        is WatchSyncState.Failed -> ErrorContent(
            error = syncState.error,
            onRetry = onRetry,
            modifier = modifier,
        )
    }
}

@Composable
private fun LoginWithPhoneInstructionsContent(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnState = rememberResponsiveColumnState()

    ScreenScaffold(
        scrollState = columnState,
        modifier = modifier,
    ) {
        ScalingLazyColumn(
            columnState = columnState,
            modifier = Modifier.clickable(onClick = onLoginClick),
        ) {
            item {
                Text(
                    text = stringResource(LR.string.log_in_on_phone),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.title2,
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
            }

            item {
                Text(
                    text = "1. ${stringResource(LR.string.log_in_watch_from_phone_instructions_1)}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
            }

            item {
                Text(
                    text = "2. ${stringResource(LR.string.log_in_watch_from_phone_instructions_2)}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }

            item {
                Text(
                    text = stringResource(LR.string.log_in_watch_from_phone_instructions_3, BuildConfig.VERSION_NAME),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun SyncingContent(modifier: Modifier = Modifier) {
    ScreenScaffold(
        timeText = null,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            CircularProgressIndicator(
                indicatorColor = MaterialTheme.colors.onPrimary,
                trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                strokeWidth = 3.dp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(LR.string.watch_sync_syncing),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.onPrimary,
            )
        }
    }
}

@Composable
private fun SuccessContent(modifier: Modifier = Modifier) {
    ScreenScaffold(
        timeText = null,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                tint = WearColors.success,
                contentDescription = stringResource(LR.string.watch_sync_success),
                modifier = Modifier.size(52.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(LR.string.watch_sync_success),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.onPrimary,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: WatchSyncError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (errorMessage, helpText) = when (error) {
        WatchSyncError.Timeout -> stringResource(LR.string.watch_sync_error_timeout) to
            stringResource(LR.string.watch_sync_error_timeout_help)

        WatchSyncError.NoPhoneConnection -> stringResource(LR.string.watch_sync_error_no_connection) to
            stringResource(LR.string.watch_sync_error_no_connection_help)

        is WatchSyncError.LoginFailed -> stringResource(LR.string.watch_sync_error_login_failed) to
            stringResource(LR.string.watch_sync_error_login_failed_help)

        is WatchSyncError.Unknown -> stringResource(LR.string.watch_sync_error_unknown) to
            stringResource(LR.string.watch_sync_error_unknown_help)
    }

    val columnState = rememberResponsiveColumnState()

    ScreenScaffold(
        scrollState = columnState,
        modifier = modifier,
    ) {
        ScalingLazyColumn(
            columnState = columnState,
        ) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        tint = MaterialTheme.colors.error,
                        contentDescription = stringResource(LR.string.error),
                        modifier = Modifier.size(52.dp),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(
                    text = errorMessage,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.onPrimary,
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(
                    text = helpText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onPrimary.copy(alpha = 0.8f),
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Chip(
                    label = stringResource(LR.string.watch_sync_retry),
                    onClick = onRetry,
                )
            }
        }
    }
}

@Preview
@Composable
private fun LoginWithPhoneInstructionsPreview() {
    WearAppTheme {
        LoginWithPhoneInstructionsContent(
            onLoginClick = {},
        )
    }
}

@Preview
@Composable
private fun SyncingContentPreview() {
    WearAppTheme {
        SyncingContent()
    }
}

@Preview
@Composable
private fun SuccessContentPreview() {
    WearAppTheme {
        SuccessContent()
    }
}

@Preview
@Composable
private fun ErrorContentTimeoutPreview() {
    WearAppTheme {
        ErrorContent(
            error = WatchSyncError.Timeout,
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun ErrorContentNoConnectionPreview() {
    WearAppTheme {
        ErrorContent(
            error = WatchSyncError.NoPhoneConnection,
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun ErrorContentLoginFailedPreview() {
    WearAppTheme {
        ErrorContent(
            error = WatchSyncError.LoginFailed("Invalid credentials"),
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun ErrorContentUnknownPreview() {
    WearAppTheme {
        ErrorContent(
            error = WatchSyncError.Unknown("Network error"),
            onRetry = {},
        )
    }
}
