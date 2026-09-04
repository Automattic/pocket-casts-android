package au.com.shiftyjelly.pocketcasts.onboarding

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.tvOnboardingBackground(base: Color, sunken: Color): Modifier = drawBehind {
    drawRect(sunken)
    drawRect(
        Brush.linearGradient(
            colors = listOf(base, sunken),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
    )
}
