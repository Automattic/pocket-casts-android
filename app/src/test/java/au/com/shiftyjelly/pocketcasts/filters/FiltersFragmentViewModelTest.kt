package au.com.shiftyjelly.pocketcasts.filters

import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManagerImpl.Companion.IN_PROGRESS_UUID
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManagerImpl.Companion.NEW_RELEASE_UUID
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class FiltersFragmentViewModelTest {

    lateinit var viewModel: FiltersFragmentViewModel

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val defaultFilters = listOf(
        Playlist(id = 1, uuid = NEW_RELEASE_UUID),
        Playlist(id = 2, uuid = IN_PROGRESS_UUID),
    )

    @Before
    fun setUp() {
        viewModel = initViewModel(tooltipValue = true)
    }

    @Test
    fun `should show tooltip`() = runTest {
        val result = viewModel.shouldShowTooltip(defaultFilters)

        assertEquals(true, result)
    }

    @Test
    fun `should not show tooltip if setting is disabled`() = runTest {
        val viewModel = initViewModel(tooltipValue = false)

        val result = viewModel.shouldShowTooltip(emptyList())

        assertEquals(false, result)
    }

    @Test
    fun `should not show tooltip if created custom filter`() = runTest {
        val result = viewModel.shouldShowTooltip(defaultFilters + listOf(Playlist(id = 3, uuid = "custom")))

        assertEquals(false, result)
    }

    @Test
    fun `should return false when at least one playlist has episodes`() = runTest {
        val viewModel = initViewModel(tooltipValue = false, shouldMockEpisodeForFirstFilter = true)

        val result = viewModel.shouldShowTooltip(defaultFilters)

        assertEquals(false, result)
    }

    private fun initViewModel(tooltipValue: Boolean, shouldMockEpisodeForFirstFilter: Boolean = false): FiltersFragmentViewModel {
        val settings = mock<Settings>()
        val episodeManager = mock<EpisodeManager>()
        val playbackManager = mock<PlaybackManager>()
        val tooltipMock = mock<UserSetting<Boolean>>()

        whenever(tooltipMock.flow).thenReturn(MutableStateFlow(tooltipValue))
        whenever(tooltipMock.value).thenReturn(tooltipValue)

        whenever(settings.showEmptyFiltersListTooltip).thenReturn(tooltipMock)

        val playlistManager = mock<PlaylistManager>()
        whenever(playlistManager.findAllRxFlowable()).thenReturn(mock())

        if (shouldMockEpisodeForFirstFilter) {
            whenever(playlistManager.countEpisodesBlocking(1, episodeManager, playbackManager)).thenReturn(5)
        }

        return FiltersFragmentViewModel(playlistManager, mock(), settings, episodeManager, playbackManager)
    }
}
