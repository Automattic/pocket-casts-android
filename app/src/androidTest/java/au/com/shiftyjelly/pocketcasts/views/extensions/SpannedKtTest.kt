package au.com.shiftyjelly.pocketcasts.views.extensions

import android.text.SpannableStringBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class SpannedKtTest {

    @Test
    fun trimPaddingEmptyString() {
        val input = SpannableStringBuilder("")
        val result = input.trimPadding()
        assertEquals("", result.toString())
    }

    @Test
    fun trimPaddingNoPadding() {
        val input = SpannableStringBuilder("No padding")
        val result = input.trimPadding()
        assertEquals("No padding", result.toString())
    }

    @Test
    fun trimPaddingTopPadding() {
        val input = SpannableStringBuilder("\n\nTop padding")
        val result = input.trimPadding()
        assertEquals("Top padding", result.toString())
    }

    @Test
    fun trimPaddingBottomPadding() {
        val input = SpannableStringBuilder("Bottom padding\n\n")
        val result = input.trimPadding()
        assertEquals("Bottom padding", result.toString())
    }

    @Test
    fun trimPaddingTopAndBottomPadding() {
        val input = SpannableStringBuilder("\n\nTop and bottom padding\n\n")
        assertEquals("\n\nTop and bottom padding\n\n", input.toString())
        val result = input.trimPadding()
        assertEquals("Top and bottom padding", result.toString())
    }
}
