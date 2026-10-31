package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewAlignment
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText

@Composable
fun ViewMaterialButton(
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    letterSpacing: TextUnit = 0.0892857143.em,
    cornerRadius: Dp? = null,
    elevation: Dp = 2.dp,
    verticalPadding: Dp = 4.dp,
    strokeColor: Color? = null,
    maxLines: Int = Int.MAX_VALUE,
    horizontalPadding: Dp = 16.dp,
    fontWeight: FontWeight = FontWeight.Medium,
    enabled: Boolean = true,
) {
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { (cornerRadius ?: 4.dp).roundToPx().toFloat() }
    val strokeWidthPx = with(density) { 1.dp.roundToPx().toFloat() }
    val insetVertical = with(density) { 6.dp.toPx().toInt().toDp() }
    val shape = RoundedCornerShape(cornerRadiusPx)
    Box(
        propagateMinConstraints = true,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = insetVertical),
    ) {
        Box(
            contentAlignment = TextViewAlignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation, shape)
                .background(backgroundColor, shape)
                .drawBehind {
                    if (strokeColor != null) {
                        val inset = strokeWidthPx / 2
                        drawRoundRect(
                            color = strokeColor,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
                            cornerRadius = CornerRadius(cornerRadiusPx - inset),
                            style = Stroke(strokeWidthPx),
                        )
                    }
                }
                .clip(shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        ) {
            TextViewText(
                text = text,
                fontSize = fontSize,
                color = textColor,
                fontWeight = fontWeight,
                letterSpacing = letterSpacing,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
