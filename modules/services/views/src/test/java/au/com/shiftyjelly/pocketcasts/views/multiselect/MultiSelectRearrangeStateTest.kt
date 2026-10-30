package au.com.shiftyjelly.pocketcasts.views.multiselect

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.MultiSelectViewOverflowMenuRearrangeActionMovedEvent
import com.automattic.eventhorizon.MultiSelectViewOverflowMenuRearrangeFinishedEvent
import com.automattic.eventhorizon.ShelfActionSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class MultiSelectRearrangeStateTest {
    private val settings = mock<Settings>()
    private val sink = TestEventSink()

    private fun createState(showRemoveListeningHistory: Boolean = false) = MultiSelectRearrangeState(
        settings = settings,
        eventHorizon = EventHorizon(sink),
        maxToolbarIcons = 4,
        source = SourceView.PODCAST_SCREEN,
        shouldShowRemoveListeningHistory = showRemoveListeningHistory,
    ).apply { onSettingsChanged(emptyList()) }

    @Test
    fun `places the shown header first and the hidden header after the toolbar icons`() {
        val items = createState().items.value

        assertEquals(MultiSelectRearrangeTitle(LR.string.multiselect_actions_shown), items[0])
        assertEquals(MultiSelectRearrangeTitle(LR.string.multiselect_actions_hidden), items[5])
        assertEquals(10, items.size)
    }

    @Test
    fun `shows remove listening history only when asked`() {
        assertTrue(createState(showRemoveListeningHistory = true).items.value.contains(MultiSelectEpisodeAction.RemoveListeningHistory))
        assertFalse(createState().items.value.contains(MultiSelectEpisodeAction.RemoveListeningHistory))
    }

    @Test
    fun `dropping persists the order and tracks the move`() {
        val state = createState()

        state.onItemStartDrag(1)
        state.onItemMove(fromPosition = 1, toPosition = 2)
        state.onItemTouchFinished(2)

        verify(settings).setMultiSelectItems(listOf("archive", "download", "mark_as_played", "play_next", "add_to_playlist", "play_last", "star", "share"))
        assertEquals(
            MultiSelectViewOverflowMenuRearrangeActionMovedEvent(
                source = SourceView.PODCAST_SCREEN.analyticsValue,
                title = MultiSelectEpisodeAction.Download.analyticsValue,
                position = 1,
                movedFrom = ShelfActionSourceType.Shelf,
                movedTo = ShelfActionSourceType.Shelf,
            ),
            sink.pollEvent(),
        )
        assertTrue(sink.isEmpty())
    }

    @Test
    fun `moving into the overflow persists the order and keeps the absolute position quirk`() {
        val state = createState()

        state.onItemStartDrag(1)
        state.onItemMove(fromPosition = 1, toPosition = 6)
        state.onItemTouchFinished(5)

        verify(settings).setMultiSelectItems(listOf("archive", "mark_as_played", "play_next", "add_to_playlist", "download", "play_last", "star", "share"))
        assertEquals(
            MultiSelectViewOverflowMenuRearrangeActionMovedEvent(
                source = SourceView.PODCAST_SCREEN.analyticsValue,
                title = MultiSelectEpisodeAction.Download.analyticsValue,
                position = 5,
                movedFrom = ShelfActionSourceType.Shelf,
                movedTo = ShelfActionSourceType.OverflowMenu,
            ),
            sink.pollEvent(),
        )
    }

    @Test
    fun `the saved order rebuild moves the hidden header back after a drag`() {
        val state = createState()
        val hiddenHeader = MultiSelectRearrangeTitle(LR.string.multiselect_actions_hidden)

        state.onItemMove(fromPosition = 1, toPosition = 2)
        assertEquals(hiddenHeader, state.items.value[6])

        state.onItemTouchFinished(2)
        state.onSettingsChanged(state.items.value.filterIsInstance<MultiSelectAction>().map { it.groupId })

        assertEquals(hiddenHeader, state.items.value[5])
    }

    @Test
    fun `saved order rebuilds the headers around the toolbar actions`() {
        val state = createState()

        state.onSettingsChanged(listOf("archive", "mark_as_played", "play_next", "add_to_playlist", "download"))

        val items = state.items.value
        assertEquals(MultiSelectEpisodeAction.AddToPlaylist, items[4])
        assertEquals(MultiSelectRearrangeTitle(LR.string.multiselect_actions_hidden), items[5])
        assertEquals(MultiSelectEpisodeAction.Download, items[6])
    }

    @Test
    fun `a drop without a handle drag persists without tracking`() {
        val state = createState()

        state.onItemMove(fromPosition = 2, toPosition = 1)
        state.onItemTouchFinished(1)

        verify(settings).setMultiSelectItems(listOf("archive", "download", "mark_as_played", "play_next", "add_to_playlist", "play_last", "star", "share"))
        assertTrue(sink.isEmpty())
    }

    @Test
    fun `a header position is tracked as invalid`() {
        val state = createState()

        state.onItemStartDrag(1)
        state.onItemTouchFinished(0)

        assertEquals(AnalyticsTracker.INVALID_OR_NULL_VALUE, (sink.pollEvent() as MultiSelectViewOverflowMenuRearrangeActionMovedEvent).title)
    }

    @Test
    fun `finishing tracks the source`() {
        createState().onFinished()

        assertEquals(MultiSelectViewOverflowMenuRearrangeFinishedEvent(source = SourceView.PODCAST_SCREEN.analyticsValue), sink.pollEvent())
    }
}
