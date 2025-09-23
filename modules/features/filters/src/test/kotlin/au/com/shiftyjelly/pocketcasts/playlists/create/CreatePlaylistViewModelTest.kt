package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.ui.text.TextRange
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.StarredRule
import au.com.shiftyjelly.pocketcasts.playlists.CreatePlaylistViewModel
import au.com.shiftyjelly.pocketcasts.playlists.CreatePlaylistViewModel.UiState
import au.com.shiftyjelly.pocketcasts.playlists.smart.AppliedRules
import au.com.shiftyjelly.pocketcasts.playlists.smart.RuleType
import au.com.shiftyjelly.pocketcasts.playlists.smart.RulesBuilder
import au.com.shiftyjelly.pocketcasts.playlists.smart.SmartRulesEditor
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist.Type
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistDraft
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CreatePlaylistViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val playlistManager = FakePlaylistManager()

    private val followedPodcasts = MutableStateFlow(emptyList<Podcast>())

    private val viewModel = CreatePlaylistViewModel(
        playlistManager = playlistManager,
        rulesEditorFactory = object : SmartRulesEditor.Factory {
            override fun create(scope: CoroutineScope, initialBuilder: RulesBuilder, initialAppliedRules: AppliedRules): SmartRulesEditor {
                return SmartRulesEditor(
                    playlistManager = playlistManager,
                    podcastManager = mock {
                        on { findSubscribedFlow() } doReturn followedPodcasts
                    },
                    scope = scope,
                    initialBuilder = initialBuilder,
                    initialAppliedRules = initialAppliedRules,
                )
            }
        },
        settings = run {
            val settingMock = mock<UserSetting<ArtworkConfiguration>> {
                on { flow } doReturn MutableStateFlow(ArtworkConfiguration(false))
            }
            mock {
                on { artworkConfiguration } doReturn settingMock
            }
        },
        analyticsTracker = AnalyticsTracker.test(),
        initialPlaylistTitle = "Playlist name",
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
            assertEquals(PodcastsRule.Selected(setOf("id-1", "id-2")), state.appliedRules.podcasts)

            followedPodcasts.value = List(4) { index -> Podcast(uuid = "id-$index") }
            skipItems(1)

            viewModel.selectAllPodcasts()
            state = awaitItem()
            assertEquals(setOf("id-0", "id-1", "id-2", "id-3"), state.rulesBuilder.selectedPodcasts)

            viewModel.deselectAllPodcasts()
            state = awaitItem()
            assertEquals(emptySet<String>(), state.rulesBuilder.selectedPodcasts)
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

    @Test
    fun `manage episode duration rule`() = runTest {
        viewModel.uiState.test {
            var state = awaitItem()
            assertEquals(UiState.Empty, state)

            viewModel.useConstrainedDuration(true)
            state = awaitItem()
            assertTrue(state.rulesBuilder.isEpisodeDurationConstrained)
            assertEquals(20.minutes, state.rulesBuilder.minEpisodeDuration)
            assertEquals(40.minutes, state.rulesBuilder.maxEpisodeDuration)
            assertNull(state.appliedRules.episodeDuration)

            viewModel.decrementMinDuration()
            state = awaitItem()
            assertTrue(state.rulesBuilder.isEpisodeDurationConstrained)
            assertEquals(15.minutes, state.rulesBuilder.minEpisodeDuration)
            assertEquals(40.minutes, state.rulesBuilder.maxEpisodeDuration)
            assertNull(state.appliedRules.episodeDuration)

            viewModel.incrementMinDuration()
            state = awaitItem()
            assertTrue(state.rulesBuilder.isEpisodeDurationConstrained)
            assertEquals(20.minutes, state.rulesBuilder.minEpisodeDuration)
            assertEquals(40.minutes, state.rulesBuilder.maxEpisodeDuration)
            assertNull(state.appliedRules.episodeDuration)

            viewModel.incrementMaxDuration()
            state = awaitItem()
            assertTrue(state.rulesBuilder.isEpisodeDurationConstrained)
            assertEquals(20.minutes, state.rulesBuilder.minEpisodeDuration)
            assertEquals(45.minutes, state.rulesBuilder.maxEpisodeDuration)
            assertNull(state.appliedRules.episodeDuration)

            viewModel.decrementMaxDuration()
            state = awaitItem()
            assertTrue(state.rulesBuilder.isEpisodeDurationConstrained)
            assertEquals(20.minutes, state.rulesBuilder.minEpisodeDuration)
            assertEquals(40.minutes, state.rulesBuilder.maxEpisodeDuration)
            assertNull(state.appliedRules.episodeDuration)

            viewModel.applyRule(RuleType.EpisodeDuration)
            state = awaitItem()
            assertEquals(
                EpisodeDurationRule.Constrained(
                    longerThan = 20.minutes,
                    shorterThan = 40.minutes,
                ),
                state.appliedRules.episodeDuration,
            )

            viewModel.useConstrainedDuration(false)
            state = awaitItem()
            assertFalse(state.rulesBuilder.isEpisodeDurationConstrained)

            viewModel.applyRule(RuleType.EpisodeDuration)
            state = awaitItem()
            assertEquals(EpisodeDurationRule.Any, state.appliedRules.episodeDuration)
        }
    }

    @Test
    fun `manage download status rule`() = runTest {
        viewModel.uiState.test {
            var state = awaitItem()
            assertEquals(UiState.Empty, state)

            viewModel.useDownloadStatus(DownloadStatusRule.Downloaded)
            state = awaitItem()
            assertEquals(DownloadStatusRule.Downloaded, state.rulesBuilder.downloadStatusRule)
            assertNull(state.appliedRules.downloadStatus)

            viewModel.useDownloadStatus(DownloadStatusRule.Any)
            state = awaitItem()
            assertEquals(DownloadStatusRule.Any, state.rulesBuilder.downloadStatusRule)
            assertNull(state.appliedRules.downloadStatus)

            viewModel.useDownloadStatus(DownloadStatusRule.NotDownloaded)
            state = awaitItem()
            assertEquals(DownloadStatusRule.NotDownloaded, state.rulesBuilder.downloadStatusRule)
            assertNull(state.appliedRules.downloadStatus)

            viewModel.applyRule(RuleType.DownloadStatus)
            state = awaitItem()
            assertEquals(DownloadStatusRule.NotDownloaded, state.appliedRules.downloadStatus)
        }
    }

    @Test
    fun `manage media type rule`() = runTest {
        viewModel.uiState.test {
            var state = awaitItem()
            assertEquals(UiState.Empty, state)

            viewModel.useMediaType(MediaTypeRule.Audio)
            state = awaitItem()
            assertEquals(MediaTypeRule.Audio, state.rulesBuilder.mediaTypeRule)
            assertNull(state.appliedRules.mediaType)

            viewModel.useMediaType(MediaTypeRule.Any)
            state = awaitItem()
            assertEquals(MediaTypeRule.Any, state.rulesBuilder.mediaTypeRule)
            assertNull(state.appliedRules.mediaType)

            viewModel.useMediaType(MediaTypeRule.Video)
            state = awaitItem()
            assertEquals(MediaTypeRule.Video, state.rulesBuilder.mediaTypeRule)
            assertNull(state.appliedRules.mediaType)

            viewModel.applyRule(RuleType.MediaType)
            state = awaitItem()
            assertEquals(MediaTypeRule.Video, state.appliedRules.mediaType)
        }
    }

    @Test
    fun `manage starred episodes rule`() = runTest {
        viewModel.uiState.test {
            var state = awaitItem()
            assertEquals(UiState.Empty, state)

            viewModel.useStarredEpisodes(true)
            state = awaitItem()
            assertEquals(StarredRule.Starred, state.rulesBuilder.starredRule)
            assertNull(state.appliedRules.starred)

            viewModel.useStarredEpisodes(false)
            state = awaitItem()
            assertEquals(StarredRule.Any, state.rulesBuilder.starredRule)
            assertNull(state.appliedRules.starred)

            viewModel.applyRule(RuleType.Starred)
            state = awaitItem()
            assertEquals(StarredRule.Any, state.appliedRules.starred)
        }
    }

    @Test
    fun `create smart playlist with applied rules`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.useAllPodcasts(false)
            skipItems(1)
            viewModel.selectPodcast("id-1")
            skipItems(1)
            viewModel.selectPodcast("id-2")
            skipItems(1)
            viewModel.useCompletedEpisodes(false)
            skipItems(1)
            viewModel.useReleaseDate(ReleaseDateRule.Last2Weeks)
            skipItems(1)
            viewModel.useConstrainedDuration(true)
            skipItems(1)
            viewModel.useMediaType(MediaTypeRule.Audio)
            skipItems(1)
            viewModel.useStarredEpisodes(true)
            skipItems(1)
            RuleType.entries.forEach { ruleType ->
                viewModel.applyRule(ruleType)
                skipItems(1)
            }

            assertFalse(viewModel.createdPlaylist.isCompleted)

            viewModel.createSmartPlaylist()
            assertEquals(
                SmartPlaylistDraft(
                    title = "Playlist name",
                    rules = viewModel.uiState.value.appliedRules.toSmartRules()!!,
                ),
                playlistManager.createSmartPlaylistTurbine.awaitItem(),
            )
            assertEquals(
                Type.Smart,
                viewModel.createdPlaylist.await().type,
            )
        }
    }

    @Test
    fun `crate manual playlist`() = runTest {
        viewModel.playlistNameState.setTextAndSelectAll("My playlist")

        viewModel.uiState.test {
            skipItems(1)

            assertFalse(viewModel.createdPlaylist.isCompleted)

            viewModel.createManualPlaylist()

            assertEquals(
                "My playlist",
                playlistManager.createManualPlaylistTurbine.awaitItem(),
            )
            assertEquals(
                Type.Manual,
                viewModel.createdPlaylist.await().type,
            )
        }
    }

    @Test
    fun `do not create more than a single playlist`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.createSmartPlaylist()
            playlistManager.createSmartPlaylistTurbine.skipItems(1)

            viewModel.createSmartPlaylist()
            viewModel.createManualPlaylist()
            playlistManager.createSmartPlaylistTurbine.expectNoEvents()
            playlistManager.createManualPlaylistTurbine.expectNoEvents()
        }
    }

    @Test
    fun `clear unsaved builder rules`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.useStarredEpisodes(true)
            skipItems(1)

            viewModel.useDownloadStatus(DownloadStatusRule.NotDownloaded)
            skipItems(1)

            viewModel.useMediaType(MediaTypeRule.Video)
            skipItems(1)
            viewModel.applyRule(RuleType.MediaType)
            skipItems(1)

            viewModel.clearTransientRules()
            val builder = awaitItem().rulesBuilder

            assertEquals(
                RulesBuilder.Empty.copy(mediaTypeRule = MediaTypeRule.Video),
                builder,
            )
        }
    }
}
