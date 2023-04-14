package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.LocalTextStyle
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun ExpandableText(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontStyle: FontStyle? = null,
    text: String,
    collapsedMaxLine: Int = 3,
    textAlign: TextAlign? = null,
    onClick: (isExpanded: Boolean) -> Unit = {},
) {

    var isExpanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }
    var lastCharIndex by remember { mutableStateOf(0) }
    val showCollapsedState by remember { derivedStateOf { hasOverflow && !isExpanded } }

    Box(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .clickable(enabled = hasOverflow) {
                isExpanded = !isExpanded
                onClick(isExpanded)
            }
            .then(modifier)
    ) {
        Text(
            modifier = textModifier.fillMaxWidth(),
            text = if (showCollapsedState) {
                text.substring(startIndex = 0, endIndex = lastCharIndex)
            } else {
                text
            },
            maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLine,
            fontStyle = fontStyle,
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.hasVisualOverflow && !isExpanded) {
                    hasOverflow = true
                    lastCharIndex = textLayoutResult.getLineEnd(collapsedMaxLine - 1)
                }
            },
            style = style,
            textAlign = textAlign
        )

        if (showCollapsedState) {
            val gradientBrush = Brush.verticalGradient(
                listOf(Color.Transparent, MaterialTheme.colors.background)
            )
            Box(
                Modifier
                    .background(gradientBrush)
                    .fillMaxSize()
            )
        }
    }
}
