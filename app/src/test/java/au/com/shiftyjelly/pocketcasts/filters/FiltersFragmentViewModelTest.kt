package au.com.shiftyjelly.pocketcasts.filters

import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SmartPlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SmartPlaylistManagerImpl.Companion.IN_PROGRESS_UUID
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SmartPlaylistManagerImpl.Companion.NEW_RELEASE_UUID
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Flowable
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
        SmartPlaylist(id = 1, uuid = NEW_RELEASE_UUID),
        SmartPlaylist(id = 2, uuid = IN_PROGRESS_UUID),
    )

    @Before
    fun setUp() {
        viewModel = initViewModel(tooltipValue = true)
    }

    @Test
    fun `should show tooltip`() = runTest {
        var showTooltip = false
        viewModel.shouldShowTooltipSuspend(defaultFilters) {
            showTooltip = true
        }
        assertEquals(true, showTooltip)
    }

    @Test
    fun `should not show tooltip if setting is disabled`() = runTest {
        val viewModel = initViewModel(tooltipValue = false)

        var showTooltip = false
        viewModel.shouldShowTooltip(emptyList()) {
            showTooltip = true
        }
        assertEquals(false, showTooltip)
    }

    @Test
    fun `should not show tooltip if created custom filter`() = runTest {
        var showTooltip = false
        viewModel.shouldShowTooltip(defaultFilters + listOf(SmartPlaylist(id = 3, uuid = "custom"))) {
            showTooltip = true
        }

        assertEquals(false, showTooltip)
    }

    @Test
    fun `should return false when at least one playlist has episodes`() = runTest {
        val viewModel = initViewModel(tooltipValue = false, shouldMockEpisodeForFirstFilter = true)

        var showTooltip = false
        viewModel.shouldShowTooltip(defaultFilters) {
            showTooltip = true
        }

        assertEquals(false, showTooltip)
    }

    private fun initViewModel(tooltipValue: Boolean, shouldMockEpisodeForFirstFilter: Boolean = false): FiltersFragmentViewModel {
        val settings = mock<Settings>()
        val episodeManager = mock<EpisodeManager>()
        val playbackManager = mock<PlaybackManager>()
        val tooltipMock = mock<UserSetting<Boolean>>()
        val userManager = mock<UserManager>()
        val bannerSetting = mock<UserSetting<Boolean>>()

        whenever(tooltipMock.flow).thenReturn(MutableStateFlow(tooltipValue))
        whenever(tooltipMock.value).thenReturn(tooltipValue)
        whenever(userManager.getSignInState()).thenReturn(Flowable.empty())
        whenever(settings.isFreeAccountFiltersBannerDismissed).thenReturn(bannerSetting)
        whenever(bannerSetting.flow).thenReturn(MutableStateFlow(false))

        whenever(settings.showEmptyFiltersListTooltip).thenReturn(tooltipMock)

        val smartPlaylistManager = mock<SmartPlaylistManager>()
        whenever(smartPlaylistManager.findAllRxFlowable()).thenReturn(mock())

        if (shouldMockEpisodeForFirstFilter) {
            whenever(smartPlaylistManager.countEpisodesBlocking(1, episodeManager, playbackManager)).thenReturn(5)
        }

        return FiltersFragmentViewModel(smartPlaylistManager, mock(), settings, episodeManager, playbackManager, userManager)
    }
}
