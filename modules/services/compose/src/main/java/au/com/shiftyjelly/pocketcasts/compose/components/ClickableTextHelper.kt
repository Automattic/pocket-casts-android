package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer

data class Clickable(
    val text: String,
    val data: String = text,
    val onClick: (data: String) -> Unit
)

/**
 * @param text the text that is displayed which must contain the text of each clickable
 * This is based on https://stackoverflow.com/a/73587919/1910286
 */
@Composable
fun ClickableTextHelper(
    text: String,
    clickables: List<Clickable>,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier,
) {
    data class TextData(
        val text: String,
        val tag: String? = null,
        val data: String? = null,
        val onClick: ((data: AnnotatedString.Range<String>) -> Unit)? = null
    )

    val textData = mutableListOf<TextData>()
    if (clickables.isEmpty()) {
        textData.add(
            TextData(
                text = text
            )
        )
    } else {
        var startIndex = 0
        clickables.forEachIndexed { i, link ->
            val endIndex = text.indexOf(link.text)
            if (endIndex == -1) {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "ClickableText failed to find clickable: ${link.text} in $text")
            } else {
                textData.add(
                    TextData(
                        text = text.substring(startIndex, endIndex)
                    )
                )
                textData.add(
                    TextData(
                        text = link.text,
                        tag = "${link.text}_TAG",
                        data = link.data,
                        onClick = {
                            link.onClick(it.item)
                        }
                    )
                )
                startIndex = endIndex + link.text.length
                if (i == clickables.lastIndex && startIndex < text.length) {
                    textData.add(
                        TextData(
                            text = text.substring(startIndex, text.length)
                        )
                    )
                }
            }
        }
    }

    val annotatedString = buildAnnotatedString {
        textData.forEach { linkTextData ->
            if (linkTextData.tag != null && linkTextData.data != null) {
                pushStringAnnotation(
                    tag = linkTextData.tag,
                    annotation = linkTextData.data,
                )
                withStyle(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline
                    ),
                ) {
                    append(linkTextData.text)
                }
                pop()
            } else {
                append(linkTextData.text)
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = TextStyle(
            color = color,
            lineHeight = 16.sp,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            textAlign = textAlign,
        ),
        onClick = { offset ->
            textData.forEach { annotatedStringData ->
                if (annotatedStringData.tag != null && annotatedStringData.data != null) {
                    annotatedString.getStringAnnotations(
                        tag = annotatedStringData.tag,
                        start = offset,
                        end = offset,
                    ).firstOrNull()?.let {
                        annotatedStringData.onClick?.invoke(it)
                    }
                }
            }
        },
        modifier = modifier
    )
}
