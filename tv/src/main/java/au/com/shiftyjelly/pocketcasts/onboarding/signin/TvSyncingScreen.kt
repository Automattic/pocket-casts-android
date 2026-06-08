package au.com.shiftyjelly.pocketcasts.onboarding.signin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val SIMULATED_SYNC_DELAY_MS = 3000L

@Composable
fun TvSyncingScreen(
    onSyncComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {}

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Dark),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SpinningArc()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(LR.string.tv_onboarding_syncing),
                color = Color.White,
                fontSize = 22.sp,
            )
        }
    }

    val currentOnSyncComplete by rememberUpdatedState(onSyncComplete)
    LaunchedEffect(Unit) {
        delay(SIMULATED_SYNC_DELAY_MS)
        currentOnSyncComplete()
    }
}

@Composable
private fun SpinningArc(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_spinner")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sync_spinner_angle",
    )

    Canvas(modifier = modifier.size(48.dp)) {
        drawArc(
            color = Color.White,
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSyncingScreenPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvSyncingScreen(onSyncComplete = {})
        }
    }
}
