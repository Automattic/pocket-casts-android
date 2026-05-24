package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragmentViewModel.EpisodeContentTab.DESCRIPTION
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragmentViewModel.EpisodeContentTab.SUMMARY
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragmentViewModel.EpisodePageState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeFragmentViewModelTest {
    @Test
    fun `clearing summary resets selected tab to description`() {
        val state = EpisodePageState(
            summary = "Episode summary",
            selectedContentTab = SUMMARY,
        )

        val newState = state.withSummary(null)

        assertNull(newState.summary)
        assertEquals(DESCRIPTION, newState.selectedContentTab)
    }

    @Test
    fun `clearing summary keeps description selected`() {
        val state = EpisodePageState(
            summary = "Episode summary",
            selectedContentTab = DESCRIPTION,
        )

        val newState = state.withSummary(null)

        assertNull(newState.summary)
        assertEquals(DESCRIPTION, newState.selectedContentTab)
    }

    @Test
    fun `summary tab cannot be selected without summary`() {
        val state = EpisodePageState()

        val newState = state.selectContentTab(SUMMARY)

        assertEquals(DESCRIPTION, newState.selectedContentTab)
    }

    @Test
    fun `summary tab can be selected when summary exists`() {
        val state = EpisodePageState(summary = "Episode summary")

        val newState = state.selectContentTab(SUMMARY)

        assertEquals(SUMMARY, newState.selectedContentTab)
    }
}
