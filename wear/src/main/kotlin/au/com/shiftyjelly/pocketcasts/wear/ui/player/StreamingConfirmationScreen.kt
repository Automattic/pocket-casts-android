package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ObtainConfirmationScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.StreamingConfirmationScreen.Result
import com.google.android.horologist.compose.layout.ScreenScaffold
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object StreamingConfirmationScreen {
    const val ROUTE = "streaming_confirmation"
    const val RESULT_KEY = "${ROUTE}_result"

    enum class Result {
        CONFIRMED,
        CANCELLED,
    }
}

@Composable
fun StreamingConfirmationScreen(
    onFinish: (Result) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        timeText = {},
        modifier = modifier,
    ) {
        ObtainConfirmationScreen(
            text = stringResource(LR.string.stream_warning_summary_short),
            onConfirm = { onFinish(Result.CONFIRMED) },
            onCancel = { onFinish(Result.CANCELLED) },
        )
    }
}
