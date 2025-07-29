package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.ui.text.TextRange
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CreatePlaylistViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val viewModel = CreatePlaylistViewModel(
        initialPlaylistName = "Playlist name",
    )

    @Test
    fun `initial playlist name is highlighted`() {
        assertEquals("Playlist name", viewModel.playlistNameState.text)
        assertEquals(TextRange(0, 13), viewModel.playlistNameState.selection)
    }
}
