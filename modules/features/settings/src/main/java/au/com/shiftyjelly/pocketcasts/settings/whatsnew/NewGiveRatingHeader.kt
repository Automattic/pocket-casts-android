package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun NewGiveRatingHeader() {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            Star(index = index, starDrawable = painterResource(id = IR.drawable.star_filled_whats_new))
        }
    }
}

@Composable
fun Star(index: Int, starDrawable: Painter) {
    var cycleCount by remember { mutableIntStateOf(0) }

    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        while (cycleCount < timesToRepeatAnimation) {
            delay(index * 50L)

            // Fade in and bounce
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = alphaAnimationSpec,
            )

            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = offsetYAnimationSpec,
            )

            // Pause
            delay(2000)

            // Fade out and move up
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = alphaAnimationSpec,
            )
            offsetY.animateTo(
                targetValue = -10f,
                animationSpec = offsetYAnimationSpec,
            )

            // Increment the cycle count
            cycleCount++
        }

        // Ensure the star remains visible after the loop
        alpha.snapTo(1f)
        offsetY.snapTo(0f)
    }

    Image(
        painter = starDrawable,
        contentDescription = null,
        modifier = Modifier
            .offset(y = offsetY.value.dp)
            .alpha(alpha.value)
            .size(33.dp),
    )
}

private val alphaAnimationSpec = tween<Float>(
    durationMillis = 300,
    easing = LinearOutSlowInEasing,
)

private val offsetYAnimationSpec = spring(
    dampingRatio = 1f,
    stiffness = 381.47f,
    visibilityThreshold = 0.01f,
)

private const val timesToRepeatAnimation = 4

@Preview
@Composable
private fun NewGiveRatingHeaderPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        Surface {
            NewGiveRatingHeader()
        }
    }
}
