package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import au.com.shiftyjelly.pocketcasts.qr.rememberQrPainter
import au.com.shiftyjelly.pocketcasts.repositories.sync.DeviceAuthState
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.LoadingSpinner
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.openUrlOnPhone
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun LoginWithCodeScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginWithCodeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The user needs time to find their phone and approve the code, so don't let the watch sleep while it's showing.
    val isShowingCode = state is DeviceAuthState.Ready
    val view = LocalView.current
    DisposableEffect(view, isShowingCode) {
        view.keepScreenOn = isShowingCode
        onDispose { view.keepScreenOn = false }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    LoginWithCodeContent(
        state = state,
        onOpenOnPhone = { url -> coroutineScope.launch { openUrlOnPhone(url, context) } },
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun LoginWithCodeContent(
    state: DeviceAuthState,
    onOpenOnPhone: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnState = rememberResponsiveColumnState()

    ScreenScaffold(
        scrollState = columnState,
        modifier = modifier,
    ) {
        when (state) {
            DeviceAuthState.Loading, DeviceAuthState.Complete -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                LoadingSpinner(Modifier.size(36.dp))
            }

            DeviceAuthState.Error -> ScalingLazyColumn(
                columnState = columnState,
            ) {
                item {
                    Text(
                        text = stringResource(LR.string.log_in_with_code_error),
                        textAlign = TextAlign.Center,
                    )
                }
                item {
                    WatchListChip(
                        title = stringResource(LR.string.retry),
                        onClick = onRetry,
                    )
                }
            }

            is DeviceAuthState.Ready -> ScalingLazyColumn(
                columnState = columnState,
            ) {
                item {
                    QrCode(content = state.verificationUriComplete)
                }
                item {
                    Text(
                        text = stringResource(
                            LR.string.log_in_with_code_instructions,
                            displayUrl(state.verificationUri),
                        ),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.caption2,
                    )
                }
                item {
                    Text(
                        text = state.userCode,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.title2,
                        letterSpacing = 4.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    WatchListChip(
                        title = stringResource(LR.string.settings_open_on_phone),
                        onClick = { onOpenOnPhone(state.verificationUriComplete) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QrCode(content: String, modifier: Modifier = Modifier) {
    val qrPainter = rememberQrPainter(content = content, size = QrSize)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(6.dp),
    ) {
        Image(
            painter = qrPainter,
            contentDescription = null,
            modifier = Modifier.size(QrSize),
        )
    }
}

private fun displayUrl(url: String) = url.removePrefix("https://").removePrefix("http://").trimEnd('/')

private val QrSize = 96.dp

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun LoginWithCodeReadyPreview() {
    WearAppTheme {
        LoginWithCodeContent(
            state = DeviceAuthState.Ready(
                userCode = "ABC234",
                verificationUri = "https://pocketcasts.com/pair",
                verificationUriComplete = "https://pocketcasts.com/pair?user_code=ABC234",
            ),
            onOpenOnPhone = {},
            onRetry = {},
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun LoginWithCodeErrorPreview() {
    WearAppTheme {
        LoginWithCodeContent(
            state = DeviceAuthState.Error,
            onOpenOnPhone = {},
            onRetry = {},
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun LoginWithCodeLoadingPreview() {
    WearAppTheme {
        LoginWithCodeContent(
            state = DeviceAuthState.Loading,
            onOpenOnPhone = {},
            onRetry = {},
        )
    }
}
