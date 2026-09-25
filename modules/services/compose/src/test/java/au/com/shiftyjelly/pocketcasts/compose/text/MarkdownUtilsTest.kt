package au.com.shiftyjelly.pocketcasts.compose.text

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MarkdownUtilsTest {

    @Test
    fun `empty string returns empty html`() {
        assertEquals("<br>", markdownToHtml(""))
    }

    @Test
    fun `h1 header is converted`() {
        assertEquals("<h1>Title</h1>", markdownToHtml("# Title"))
    }

    @Test
    fun `h2 header is converted`() {
        assertEquals("<h2>Section</h2>", markdownToHtml("## Section"))
    }

    @Test
    fun `h3 header is converted`() {
        assertEquals("<h3>Subsection</h3>", markdownToHtml("### Subsection"))
    }

    @Test
    fun `dash list item is converted`() {
        assertEquals("&#8226; Item one<br>", markdownToHtml("- Item one"))
    }

    @Test
    fun `asterisk list item is converted`() {
        assertEquals("&#8226; Item two<br>", markdownToHtml("* Item two"))
    }

    @Test
    fun `bold text is converted`() {
        assertEquals("This is <b>bold</b> text<br>", markdownToHtml("This is **bold** text"))
    }

    @Test
    fun `blank line becomes br`() {
        assertEquals("First<br>\n<br>\nSecond<br>", markdownToHtml("First\n\nSecond"))
    }

    @Test
    fun `regular text gets br suffix`() {
        assertEquals("Hello world<br>", markdownToHtml("Hello world"))
    }

    @Test
    fun `special characters are html encoded`() {
        val result = markdownToHtml("Use <div> & \"quotes\"")
        assertEquals("Use &lt;div&gt; &amp; &quot;quotes&quot;<br>", result)
    }

    @Test
    fun `mixed content is converted correctly`() {
        val markdown = """
            ## Episode Highlights

            - First point
            - **Key** takeaway
        """.trimIndent()

        val expected = "<h2>Episode Highlights</h2>\n<br>\n&#8226; First point<br>\n&#8226; <b>Key</b> takeaway<br>"
        assertEquals(expected, markdownToHtml(markdown))
    }
}
