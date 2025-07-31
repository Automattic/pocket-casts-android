package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.ui.text.TextRange
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
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

    @Test
    fun `manage episode status rule`() = runTest {
        viewModel.uiState.test {
            var state = awaitItem()
            assertEquals(UiState.Empty, state)

            viewModel.useUnplayedEpisodes(false)
            state = awaitItem()
            assertFalse(state.rulesBuilder.episodeStatusRule.unplayed)
            assertNull(state.appliedRules.episodeStatus)

            viewModel.useInProgressEpisodes(false)
            state = awaitItem()
            assertFalse(state.rulesBuilder.episodeStatusRule.inProgress)
            assertNull(state.appliedRules.episodeStatus)

            viewModel.useCompletedEpisodes(false)
            state = awaitItem()
            assertFalse(state.rulesBuilder.episodeStatusRule.completed)
            assertNull(state.appliedRules.episodeStatus)

            viewModel.applyRule(RuleType.EpisodeStatus)
            state = awaitItem()
            assertEquals(
                EpisodeStatusRule(unplayed = false, inProgress = false, completed = false),
                state.appliedRules.episodeStatus,
            )

            viewModel.useUnplayedEpisodes(true)
            state = awaitItem()
            assertTrue(state.rulesBuilder.episodeStatusRule.unplayed)

            viewModel.applyRule(RuleType.EpisodeStatus)
            state = awaitItem()
            assertEquals(
                EpisodeStatusRule(unplayed = true, inProgress = false, completed = false),
                state.appliedRules.episodeStatus,
            )

            viewModel.useInProgressEpisodes(true)
            state = awaitItem()
            assertTrue(state.rulesBuilder.episodeStatusRule.inProgress)

            viewModel.applyRule(RuleType.EpisodeStatus)
            state = awaitItem()
            assertEquals(
                EpisodeStatusRule(unplayed = true, inProgress = true, completed = false),
                state.appliedRules.episodeStatus,
            )

            viewModel.useCompletedEpisodes(true)
            state = awaitItem()
            assertTrue(state.rulesBuilder.episodeStatusRule.completed)

            viewModel.applyRule(RuleType.EpisodeStatus)
            state = awaitItem()
            assertEquals(
                EpisodeStatusRule(unplayed = true, inProgress = true, completed = true),
                state.appliedRules.episodeStatus,
            )
        }
    }

    @Test
    fun `manage release date rule`() = runTest {
        viewModel.uiState.test {
            var state = awaitItem()
            assertEquals(UiState.Empty, state)

            viewModel.useReleaseDate(ReleaseDateRule.LastMonth)
            state = awaitItem()
            assertEquals(ReleaseDateRule.LastMonth, state.rulesBuilder.releaseDateRule)
            assertNull(state.appliedRules.releaseDate)

            viewModel.useReleaseDate(ReleaseDateRule.Last2Weeks)
            state = awaitItem()
            assertEquals(ReleaseDateRule.Last2Weeks, state.rulesBuilder.releaseDateRule)
            assertNull(state.appliedRules.releaseDate)

            viewModel.applyRule(RuleType.ReleaseDate)
            state = awaitItem()
            assertEquals(ReleaseDateRule.Last2Weeks, state.appliedRules.releaseDate)
        }
    }
}
