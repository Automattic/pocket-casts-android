package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val MaxFontScale = 1.15f
val StoryFontFamily = FontFamily(listOf(Font(UR.font.dm_sans)))

@Composable
fun StoryPrimaryText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    TextH10(
        text = text,
        textAlign = TextAlign.Center,
        color = color,
        disableScale = true,
        fontFamily = FontFamily(listOf(Font(UR.font.dm_sans))),
        fontWeight = FontWeight.W600,
        fontSize = 25.sp,
        lineHeight = 31.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    )
}

@Composable
fun StorySecondaryText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    TextH50(
        text = text,
        textAlign = TextAlign.Center,
        color = color,
        fontFamily = FontFamily(listOf(Font(UR.font.dm_sans))),
        fontWeight = FontWeight.W600,
        disableScale = disableScale(),
        modifier = modifier
            .fillMaxWidth()
            .alpha(0.8f)
            .padding(horizontal = 24.dp)
    )
}

@Composable
fun disableScale() = LocalDensity.current.fontScale > MaxFontScale
