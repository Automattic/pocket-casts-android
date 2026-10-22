package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.buildAnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpandableTextTest {
    @Test
    fun `returns link at the given text offset`() {
        val text = buildAnnotatedString {
            append("Visit ")
            pushLink(LinkAnnotation.Url("https://pocketcasts.com"))
            append("Pocket Casts")
            pop()
            append(" today")
        }

        assertEquals(
            UrlRange(
                url = "https://pocketcasts.com",
                start = 6,
                end = 18,
            ),
            text.urlRangeAt(offset = 10),
        )
    }

    @Test
    fun `does not return a link outside the linked text`() {
        val text = buildAnnotatedString {
            append("Visit ")
            pushLink(LinkAnnotation.Url("https://pocketcasts.com"))
            append("Pocket Casts")
            pop()
            append(" today")
        }

        assertNull(text.urlRangeAt(offset = 5))
        assertNull(text.urlRangeAt(offset = 18))
    }

    @Test
    fun `ignores blank link targets`() {
        val text = buildAnnotatedString {
            pushLink(LinkAnnotation.Url(""))
            append("Empty link")
            pop()
        }

        assertNull(text.urlRangeAt(offset = 2))
    }

    @Test
    fun `returns the correct target when text contains multiple links`() {
        val text = buildAnnotatedString {
            pushLink(LinkAnnotation.Url("https://example.com/one"))
            append("First")
            pop()
            append(" and ")
            pushLink(LinkAnnotation.Url("https://example.com/two"))
            append("second")
            pop()
        }

        assertEquals("https://example.com/one", text.urlRangeAt(offset = 2)?.url)
        assertEquals("https://example.com/two", text.urlRangeAt(offset = 12)?.url)
    }

    @Test
    fun `ignores clickable annotations that are not URLs`() {
        val text = buildAnnotatedString {
            pushLink(
                LinkAnnotation.Clickable(
                    tag = "action",
                    linkInteractionListener = LinkInteractionListener {},
                ),
            )
            append("Action")
            pop()
        }

        assertNull(text.urlRangeAt(offset = 2))
    }
}
