package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40

@Composable
fun StoryPrimaryText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    TextH20(
        text = text,
        textAlign = TextAlign.Center,
        color = color,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
    )
}

@Composable
fun StorySecondaryText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    TextP40(
        text = text,
        textAlign = TextAlign.Center,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .alpha(0.8f)
            .padding(horizontal = 40.dp)
    )
}
