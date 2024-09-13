package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer

data class Clickable(
    val text: String,
    val data: String = text,
    val onClick: (data: String) -> Unit,
)

/**
 * @param text the text that is displayed which must contain the text of each clickable
 * This is based on https://stackoverflow.com/a/73587919/1910286
 */
@Composable
fun ClickableTextHelper(
    text: String,
    clickables: List<Clickable>,
    lineHeight: TextUnit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = MaterialTheme.theme.colors.primaryText01,
) {
    data class TextData constructor(
        val text: String,
        val tag: String? = null,
        val data: String? = null,
        val onClick: ((data: String) -> Unit)? = null,
    )

    val textData = mutableListOf<TextData>()
    if (clickables.isEmpty()) {
        textData.add(
            TextData(
                text = text,
            ),
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
                        text = text.substring(startIndex, endIndex),
                    ),
                )
                textData.add(
                    TextData(
                        text = link.text,
                        tag = "${link.text}_TAG",
                        data = link.data,
                        onClick = { link.onClick(it) },
                    ),
                )
                startIndex = endIndex + link.text.length
                if (i == clickables.lastIndex && startIndex < text.length) {
                    textData.add(
                        TextData(
                            text = text.substring(startIndex, text.length),
                        ),
                    )
                }
            }
        }
    }

    val annotatedString = buildAnnotatedString {
        textData.forEach { linkTextData ->
            if (linkTextData.tag != null && linkTextData.data != null) {
                pushLink(
                    LinkAnnotation.Url(
                        url = linkTextData.data,
                        linkInteractionListener = { _ ->
                            linkTextData.onClick?.invoke(linkTextData.data)
                        },
                    ),
                )
                withStyle(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline,
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

    BasicText(
        text = annotatedString,
        style = TextStyle(
            color = color,
            lineHeight = lineHeight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            textAlign = textAlign,
        ),
        modifier = modifier,
    )
}
