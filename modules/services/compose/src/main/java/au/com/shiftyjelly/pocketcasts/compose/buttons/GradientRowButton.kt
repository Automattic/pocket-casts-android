package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.AutoResizeText
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import com.airbnb.android.showkase.annotation.ShowkaseComposable

private val plusBackgroundBrush = Brush.horizontalGradient(
    0f to Color(0xFFFED745),
    1f to Color(0xFFFEB525),
)

@Composable
fun GradientRowButton(
    primaryText: String,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    fontWeight: FontWeight = FontWeight.W600,
    textColor: Color,
    gradientBackgroundColor: Brush = plusBackgroundBrush,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradientBackgroundColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AutoResizeText(
                text = primaryText,
                color = textColor,
                maxFontSize = 18.sp,
                lineHeight = 21.sp,
                fontWeight = fontWeight,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            secondaryText?.let {
                TextP60(
                    text = it,
                    textAlign = TextAlign.Center,
                    color = textColor,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@ShowkaseComposable(name = "GradientRowButton", group = "Button")
@Preview
@Composable
fun GradientRowButtonPreview() {
    GradientRowButton(
        primaryText = "Upgrade Now",
        textColor = Color.Black,
        onClick = {},
    )
}
