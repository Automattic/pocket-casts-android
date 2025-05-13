package au.com.shiftyjelly.pocketcasts.compose.text

import android.graphics.Color
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpannable
import org.junit.Test

class ExtensionTest {

    @Test
    fun toAnnotatedString() {
        val string = "<h3><br /></h3><h3>The</h3><h3>Podcast</h3>"
        val html = HtmlCompat.fromHtml(
            string,
            HtmlCompat.FROM_HTML_MODE_COMPACT and
                HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH.inv(),
        )
        val annotatedString = html.toSpannable().toAnnotatedString(urlColor = Color.RED)
        annotatedString.spanStyles.forEach { spanStyle ->
            assert(spanStyle.end > spanStyle.start) { "Span end is not greater than start: ${spanStyle.end} <= ${spanStyle.start}" }
            val length = spanStyle.toString().length
            assert(spanStyle.start <= length) { "Span start is greater than length: ${spanStyle.start} > $length" }
            assert(spanStyle.end <= length) { "Span end is greater than length: ${spanStyle.end} > $length" }
            assert(spanStyle.start >= 0) { "Span start is negative: ${spanStyle.start}" }
            assert(spanStyle.end >= 0) { "Span end is negative: ${spanStyle.end}" }
        }
    }
}
