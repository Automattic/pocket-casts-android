package au.com.shiftyjelly.pocketcasts.views.multiselect

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MultiSelectEpisodeActionTest {

    @Test
    fun testListFromIds() {
        val ids = listOf("star", "play_last", "play_next", "download", "archive", "share", "mark_as_played")
        val result = MultiSelectEpisodeAction.listFromIds(ids)
        val expectedActions = listOf(
            MultiSelectEpisodeAction.Star,
            MultiSelectEpisodeAction.PlayLast,
            MultiSelectEpisodeAction.PlayNext,
            MultiSelectEpisodeAction.Download,
            MultiSelectEpisodeAction.Archive,
            MultiSelectEpisodeAction.Share,
            MultiSelectEpisodeAction.MarkAsPlayed,
        )
        assertEquals(expectedActions, result)
    }
}
