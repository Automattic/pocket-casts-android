package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import org.junit.Assert.assertEquals
import org.junit.Test
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class EpisodeFragmentMergedTabsTest {

    @Test
    fun `all content available produces correct tab order`() {
        val tabs = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = true,
            hasSummary = true,
            hasChapters = true,
        )

        assertEquals(
            listOf(LR.string.details, LR.string.chapters, LR.string.bookmarks, LR.string.transcript, LR.string.summary),
            tabs,
        )
    }

    @Test
    fun `no optional content shows only details and bookmarks`() {
        val tabs = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = false,
            hasSummary = false,
            hasChapters = false,
        )

        assertEquals(
            listOf(LR.string.details, LR.string.bookmarks),
            tabs,
        )
    }

    @Test
    fun `details is always the first tab`() {
        val tabs = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = true,
            hasSummary = true,
            hasChapters = true,
        )

        assertEquals(LR.string.details, tabs.first())
    }

    @Test
    fun `bookmarks is always present`() {
        val tabs = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = false,
            hasSummary = false,
            hasChapters = false,
        )

        assert(tabs.contains(LR.string.bookmarks))
    }

    @Test
    fun `chapters tab appears only when chapters exist`() {
        val withChapters = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = false,
            hasSummary = false,
            hasChapters = true,
        )
        val withoutChapters = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = false,
            hasSummary = false,
            hasChapters = false,
        )

        assert(withChapters.contains(LR.string.chapters))
        assert(!withoutChapters.contains(LR.string.chapters))
    }

    @Test
    fun `transcript tab appears only when transcript exists`() {
        val withTranscript = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = true,
            hasSummary = false,
            hasChapters = false,
        )
        val withoutTranscript = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = false,
            hasSummary = false,
            hasChapters = false,
        )

        assert(withTranscript.contains(LR.string.transcript))
        assert(!withoutTranscript.contains(LR.string.transcript))
    }

    @Test
    fun `summary tab appears only when summary exists`() {
        val withSummary = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = false,
            hasSummary = true,
            hasChapters = false,
        )
        val withoutSummary = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = false,
            hasSummary = false,
            hasChapters = false,
        )

        assert(withSummary.contains(LR.string.summary))
        assert(!withoutSummary.contains(LR.string.summary))
    }

    @Test
    fun `chapters appears before bookmarks`() {
        val tabs = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = false,
            hasSummary = false,
            hasChapters = true,
        )

        assert(tabs.indexOf(LR.string.chapters) < tabs.indexOf(LR.string.bookmarks))
    }

    @Test
    fun `transcript appears after bookmarks`() {
        val tabs = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = true,
            hasSummary = false,
            hasChapters = false,
        )

        assert(tabs.indexOf(LR.string.transcript) > tabs.indexOf(LR.string.bookmarks))
    }

    @Test
    fun `summary is always the last tab when present`() {
        val tabs = EpisodeFragment.mergedTabLabelResIds(
            hasTranscript = true,
            hasSummary = true,
            hasChapters = true,
        )

        assertEquals(LR.string.summary, tabs.last())
    }
}
