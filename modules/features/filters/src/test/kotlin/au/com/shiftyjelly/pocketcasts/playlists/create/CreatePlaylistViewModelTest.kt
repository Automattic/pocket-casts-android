package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.ui.text.TextRange
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.playlists.create.CreatePlaylistViewModel.UiState
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CreatePlaylistViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val viewModel = CreatePlaylistViewModel(
        initialPlaylistTitle = "Playlist name",
        podcastManager = mock {
            on { findSubscribedFlow() } doReturn flowOf(emptyList())
        },
        playlistManager = mock {
            on { observeSmartEpisodes(any()) } doReturn flowOf(emptyList())
        },
        settings = run {
            val settingMock = mock<UserSetting<ArtworkConfiguration>> {
                on { flow } doReturn MutableStateFlow(ArtworkConfiguration(false))
            }
            mock {
                on { artworkConfiguration } doReturn settingMock
            }
        },
    )

    @Test
    fun `initial playlist name is highlighted`() {
        assertEquals("Playlist name", viewModel.playlistNameState.text)
        assertEquals(TextRange(0, 13), viewModel.playlistNameState.selection)
    }

    @Test
    fun `manage podcasts rule`() = runTest {
        viewModel.uiState.test {
            var state = awaitItem()
            assertEquals(UiState.Empty, state)

            viewModel.useAllPodcasts(false)
            state = awaitItem()
            assertFalse(state.rulesBuilder.useAllPodcasts)
            assertNull(state.appliedRules.podcasts)

            viewModel.useAllPodcasts(true)
            state = awaitItem()
            assertTrue(state.rulesBuilder.useAllPodcasts)
            assertNull(state.appliedRules.podcasts)

            viewModel.applyRule(RuleType.Podcasts)
            state = awaitItem()
            assertEquals(PodcastsRule.Any, state.appliedRules.podcasts)

            viewModel.useAllPodcasts(false)
            skipItems(1)

            viewModel.selectPodcast("id-1")
            state = awaitItem()
            assertEquals(setOf("id-1"), state.rulesBuilder.selectedPodcasts)
            assertEquals(PodcastsRule.Any, state.appliedRules.podcasts)

            viewModel.selectPodcast("id-2")
            state = awaitItem()
            assertEquals(setOf("id-1", "id-2"), state.rulesBuilder.selectedPodcasts)
            assertEquals(PodcastsRule.Any, state.appliedRules.podcasts)

            viewModel.applyRule(RuleType.Podcasts)
            state = awaitItem()
            assertEquals(PodcastsRule.Selected(listOf("id-1", "id-2")), state.appliedRules.podcasts)
        }
    }
}
