package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton

private val ButtonTextColor = Color(0xFF161718)

@Composable
fun StoryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RowButton(
        text = text,
        fontFamily = StoryFontFamily,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
        cornerRadius = 4.dp,
        textColor = ButtonTextColor,
        onClick = onClick,
        modifier = modifier
            .fillMaxSize(.65f)
    )
}
