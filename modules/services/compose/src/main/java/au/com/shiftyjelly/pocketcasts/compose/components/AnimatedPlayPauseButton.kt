package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val FRAME_MIN = 0
private const val FRAME_MAX = 20
private const val FRAME_PLAY_IMAGE = 10

@Composable
fun AnimatedPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.Unspecified,
    iconWidth: Dp = 40.dp,
    iconHeight: Dp = 40.dp,
    circleSize: Dp = 80.dp,
    circleColor: Color = Color.White,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(UR.raw.large_play_button))
    val lottieAnimatable = rememberLottieAnimatable()
    val clipSpec = if (isPlaying) {
        LottieClipSpec.Frame(FRAME_PLAY_IMAGE, FRAME_MAX)
    } else {
        LottieClipSpec.Frame(FRAME_MIN, FRAME_PLAY_IMAGE)
    }

    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value = SimpleColorFilter(iconTint.toArgb()),
            keyPath = arrayOf("**"),
        ),
    )

    Box(
        modifier = modifier
            .size(circleSize)
            .clip(CircleShape)
            .background(circleColor)
            .clickable(
                role = Role.Button,
                onClickLabel = if (isPlaying) stringResource(LR.string.pause) else stringResource(LR.string.play),
                onClick = onClick,
            ),
    ) {
        LottieAnimation(
            composition = composition,
            progress = { lottieAnimatable.progress },
            modifier = Modifier
                .size(iconWidth, iconHeight)
                .align(Alignment.Center),
            dynamicProperties = dynamicProperties,
        )
    }

    LaunchedEffect(isPlaying) {
        lottieAnimatable.animate(
            composition = composition,
            clipSpec = clipSpec,
        )
    }
}

@Preview(widthDp = 80, heightDp = 80)
@Composable
private fun AnimatedPlayButtonPreview() {
    AnimatedPlayPauseButton(
        isPlaying = false,
        onClick = {},
    )
}
