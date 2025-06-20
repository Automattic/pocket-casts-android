package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.AnalyticsProp
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.ERROR_MINIMUM_SHELF_ITEMS
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.ERROR_SHELF_ITEM_INVALID_MOVE_POSITION
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfRowItem
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlin.collections.listOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ShelfViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var transcriptManager: TranscriptManager

    @Mock
    private lateinit var analyticsTracker: AnalyticsTracker

    @Mock
    private lateinit var settings: Settings

    private lateinit var shelfViewModel: ShelfViewModel

    @Test
    fun `given editable state, when setData is called, then minimum 4 shelf items should be passed in argument`() = runTest {
        val items = listOf(
            mock<ShelfItem>(),
            mock<ShelfItem>(),
        )
        val episode = mock<PodcastEpisode>()
        initViewModel(isEditable = true)

        try {
            shelfViewModel.setData(items, episode)
        } catch (e: IllegalArgumentException) {
            assertEquals(ERROR_MINIMUM_SHELF_ITEMS, e.message)
        }
    }

    @Test
    fun `given editable state, when setData is called, then shelfRowItems are updated with titles`() = runTest {
        val items = listOf(
            mock<ShelfItem>(),
            mock<ShelfItem>(),
            mock<ShelfItem>(),
            mock<ShelfItem>(),
            mock<ShelfItem>(),
        )
        val episode = mock<PodcastEpisode>()
        initViewModel(isEditable = true)

        shelfViewModel.setData(items, episode)

        val expectedItems = listOf(
            ShelfViewModel.shortcutTitle,
            items[0],
            items[1],
            items[2],
            items[3],
            ShelfViewModel.moreActionsTitle,
            items[4],
        )

        assertEquals(expectedItems, shelfViewModel.uiState.value.shelfRowItems)
    }

    @Test
    fun `given non-editable state, when setData is called, then shelfRowItems are updated without titles`() = runTest {
        initViewModel(isEditable = false)
        val items = listOf<ShelfItem>(mock(), mock())
        val episode = mock<PodcastEpisode>()

        shelfViewModel.setData(items, episode)

        assertEquals(items, shelfViewModel.uiState.value.shelfRowItems)
    }

    @Test
    fun `given valid positions, when onShelfItemMove is called, then items are swapped correctly`() = runTest {
        moveShelfItem(6, 3)

        val expectedItems = buildList<ShelfRowItem> {
            add(ShelfViewModel.shortcutTitle)
            add(ShelfItem.entries[0])
            add(ShelfItem.entries[1])
            add(ShelfItem.entries[4])
            add(ShelfItem.entries[2])
            add(ShelfViewModel.moreActionsTitle)
            add(ShelfItem.entries[3])
            add(ShelfItem.entries[5])
            addAll(ShelfItem.entries.subList(6, ShelfItem.entries.size))
        }
        assertEquals(expectedItems, shelfViewModel.uiState.value.shelfRowItems)
    }

    @Test
    fun `given valid positions, when onShelfItemMove is called, then titles remain in correct positions`() = runTest {
        moveShelfItem(3, 7)

        assertEquals(ShelfViewModel.shortcutTitle, shelfViewModel.uiState.value.shelfRowItems[0])
        assertEquals(ShelfViewModel.moreActionsTitle, shelfViewModel.uiState.value.shelfRowItems[5])
    }

    @Test
    fun `given valid positions, when item moved from shelf to overflow section, then rearrange action is properly tracked`() = runTest {
        moveShelfItem(3, 6)

        verify(analyticsTracker).track(
            AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_ACTION_MOVED,
            mapOf(
                AnalyticsProp.Key.ACTION to ShelfItem.entries[2].analyticsValue,
                AnalyticsProp.Key.POSITION to 0, // it is the new position in section it was moved to
                AnalyticsProp.Key.MOVED_FROM to AnalyticsProp.Value.SHELF,
                AnalyticsProp.Key.MOVED_TO to AnalyticsProp.Value.OVERFLOW_MENU,
            ),
        )
    }

    @Test
    fun `given valid positions, when item moved from overflow to shelf section, then rearrange action is properly tracked`() = runTest {
        moveShelfItem(6, 3)

        verify(analyticsTracker).track(
            AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_ACTION_MOVED,
            mapOf(
                AnalyticsProp.Key.ACTION to ShelfItem.entries[4].analyticsValue,
                AnalyticsProp.Key.POSITION to 2, // it is the new position in section it was moved to
                AnalyticsProp.Key.MOVED_FROM to AnalyticsProp.Value.OVERFLOW_MENU,
                AnalyticsProp.Key.MOVED_TO to AnalyticsProp.Value.SHELF,
            ),
        )
    }

    @Test
    fun `given valid positions, when item moved from shelf to shelf section, then rearrange action is properly tracked`() = runTest {
        moveShelfItem(1, 2)

        verify(analyticsTracker).track(
            AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_ACTION_MOVED,
            mapOf(
                AnalyticsProp.Key.ACTION to ShelfItem.entries[0].analyticsValue,
                AnalyticsProp.Key.POSITION to 1, // it is the new position in section it was moved to
                AnalyticsProp.Key.MOVED_FROM to AnalyticsProp.Value.SHELF,
                AnalyticsProp.Key.MOVED_TO to AnalyticsProp.Value.SHELF,
            ),
        )
    }

    @Test
    fun `given valid positions, when item moved from overflow to overflow section, then rearrange action is properly tracked`() = runTest {
        moveShelfItem(6, 7)

        verify(analyticsTracker).track(
            AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_ACTION_MOVED,
            mapOf(
                AnalyticsProp.Key.ACTION to ShelfItem.entries[4].analyticsValue,
                AnalyticsProp.Key.POSITION to 1, // it is the new position in section it was moved to
                AnalyticsProp.Key.MOVED_FROM to AnalyticsProp.Value.OVERFLOW_MENU,
                AnalyticsProp.Key.MOVED_TO to AnalyticsProp.Value.OVERFLOW_MENU,
            ),
        )
    }

    @Test
    fun `given invalid positions, when onShelfItemMove is called, then invalid move position error is returned`() = runTest {
        val from = 1
        val to = 4 // shelf title position
        try {
            moveShelfItem(from, to)
        } catch (e: IllegalArgumentException) {
            assertEquals("$ERROR_SHELF_ITEM_INVALID_MOVE_POSITION from: $from to: $to", e.message)
        }
    }

    private fun moveShelfItem(
        from: Int,
        to: Int,
    ) {
        val items = ShelfItem.entries
        initViewModel(isEditable = true)
        shelfViewModel.setData(items, mock<PodcastEpisode>())

        shelfViewModel.onShelfItemMove(from, to)
    }

    private fun initViewModel(
        isEditable: Boolean = true,
    ) {
        val episodeId = "testEpisodeId"
        whenever(transcriptManager.observeIsTranscriptAvailable(episodeId)).thenReturn(flowOf(true))
        val userSetting = mock<UserSetting<List<ShelfItem>>>()
        whenever(settings.shelfItems).thenReturn(userSetting)

        shelfViewModel = ShelfViewModel(
            episodeId = episodeId,
            isEditable = isEditable,
            transcriptManager = transcriptManager,
            analyticsTracker = analyticsTracker,
            settings = settings,
        )
    }
}
