package au.com.shiftyjelly.pocketcasts.compose.text

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

// See: https://iamjosephmj.medium.com/how-to-display-styled-strings-in-jetpack-compose-decd6b705746

fun Spanned.toAnnotatedString(urlColor: Int? = null): AnnotatedString = buildAnnotatedString {
    val timmedText = this@toAnnotatedString.toString().trim()
    // Step 1: Copy over the raw text
    append(timmedText)
    // Step 2: Go through each span
    getSpans(0, timmedText.length, Any::class.java).forEach { span ->
        val start = getSpanStart(span)
        val end = getSpanEnd(span)
        when (span) {
            // Bold, Italic, Bold-Italic
            is StyleSpan -> {
                when (span.style) {
                    Typeface.BOLD -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        start,
                        end,
                    )

                    Typeface.ITALIC -> addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic),
                        start,
                        end,
                    )

                    Typeface.BOLD_ITALIC -> addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                        ),
                        start,
                        end,
                    )
                }
            }
            // Underline
            is UnderlineSpan -> {
                addStyle(
                    SpanStyle(textDecoration = TextDecoration.Underline),
                    start,
                    end,
                )
            }
            // Foreground Color
            is ForegroundColorSpan -> {
                addStyle(
                    SpanStyle(color = Color(span.foregroundColor)),
                    start,
                    end,
                )
            }
            // Background Color
            is BackgroundColorSpan -> {
                addStyle(
                    SpanStyle(background = Color(span.backgroundColor)),
                    start,
                    end,
                )
            }
            // Strikethrough (Line-through)
            is StrikethroughSpan -> {
                addStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough),
                    start,
                    end,
                )
            }
            // Relative Size (scales the text)
            is RelativeSizeSpan -> {
                // For a real-world app, you'd need the base font size to multiply by span.sizeChange.
                // Here, for simplicity, let's assume a base size or do a rough conversion:
                val baseFontSize = 16.sp
                val newFontSize = baseFontSize * span.sizeChange
                addStyle(
                    SpanStyle(fontSize = newFontSize),
                    start,
                    end,
                )
            }
            // URL or clickable text
            is URLSpan -> {
                // You can store the URL as an annotation and optionally add a style
                addStringAnnotation(
                    tag = "URL",
                    annotation = span.url,
                    start = start,
                    end = end,
                )
                // Optional: add a style (color or underline) to make it look clickable
                val color = if (urlColor != null) Color(urlColor) else Color.Blue
                addStyle(
                    SpanStyle(
                        color = color,
                        textDecoration = TextDecoration.Underline,
                    ),
                    start,
                    end,
                )
            }
            // Subscript
            is SubscriptSpan -> {
                // Compose doesn't have a built-in subscript style,
                // so you'd either skip or handle it with a custom solution
                // For demonstration, let's apply a smaller font size
                val baseFontSize = 16.sp
                addStyle(
                    SpanStyle(fontSize = baseFontSize * 0.8f, baselineShift = BaselineShift.Subscript),
                    start,
                    end,
                )
            }
            // Superscript
            is SuperscriptSpan -> {
                // Similarly, let's demonstrate a smaller font size with a shift
                val baseFontSize = 16.sp
                addStyle(
                    SpanStyle(fontSize = baseFontSize * 0.8f, baselineShift = BaselineShift.Superscript),
                    start,
                    end,
                )
            }
            // You can keep adding more span types as needed
            else -> {}
        }
    }
}
