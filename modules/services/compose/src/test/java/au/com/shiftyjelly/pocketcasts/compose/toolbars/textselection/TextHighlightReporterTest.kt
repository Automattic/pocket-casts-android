package au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection

import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import org.junit.Assert.assertEquals
import org.junit.Test

class TextHighlightReporterTest {
    @Test
    fun `reports first non-empty selection once`() {
        var reportCount = 0
        val reporter = TextHighlightReporter { reportCount++ }

        reporter.reportIfNeeded(copyData())
        reporter.reportIfNeeded(copyData())

        assertEquals(1, reportCount)
    }

    @Test
    fun `does not reset while selection remains copyable`() {
        var reportCount = 0
        val reporter = TextHighlightReporter { reportCount++ }

        reporter.reportIfNeeded(copyData())
        reporter.resetIfSelectionCleared(copyData())
        reporter.reportIfNeeded(copyData())

        assertEquals(1, reportCount)
    }

    @Test
    fun `resets after selection is cleared`() {
        var reportCount = 0
        val reporter = TextHighlightReporter { reportCount++ }

        reporter.reportIfNeeded(copyData())
        reporter.resetIfSelectionCleared(nonCopyData())
        reporter.reportIfNeeded(copyData())

        assertEquals(2, reportCount)
    }

    @Test
    fun `does not report menu data without copy action`() {
        var reportCount = 0
        val reporter = TextHighlightReporter { reportCount++ }

        reporter.reportIfNeeded(nonCopyData())
        reporter.reportIfNeeded(TextContextMenuData.Empty)

        assertEquals(0, reportCount)
    }

    private fun copyData() = TextContextMenuData(
        listOf(
            TextContextMenuItem(TextContextMenuKeys.CopyKey, "Copy") {},
            TextContextMenuItem(TextContextMenuKeys.SelectAllKey, "Select All") {},
        ),
    )

    private fun nonCopyData() = TextContextMenuData(
        listOf(
            TextContextMenuItem(TextContextMenuKeys.SelectAllKey, "Select All") {},
        ),
    )
}
