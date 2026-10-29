package au.com.shiftyjelly.pocketcasts.views.compose

import android.os.Looper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.dialog.HiltShimActivity
import au.com.shiftyjelly.pocketcasts.views.dialog.HiltShimApplication
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import com.automattic.eventhorizon.EventHorizon
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltShimApplication::class)
class MultiSelectControllerTest {
    private val activity = Robolectric.buildActivity(HiltShimActivity::class.java).setup().get()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + Job())
    private val calls = mutableListOf<String>()

    private val episodesHelper = MultiSelectEpisodesHelper(
        episodeManager = mock(),
        userEpisodeManager = mock(),
        podcastManager = mock(),
        playbackManager = mock(),
        downloadQueue = mock(),
        settings = mock<Settings> { on { multiSelectItemsFlow } doReturn MutableStateFlow(emptyList()) },
        eventHorizon = EventHorizon {},
        applicationScope = mock<CoroutineScope>(),
        crashLogging = mock(),
        shareDialogFactory = mock(),
        addToPlaylistFragmentFactory = mock(),
    )

    private val bookmarksHelper = MultiSelectBookmarksHelper(
        bookmarkManager = mock<BookmarkManager> { on { sourceView } doReturn SourceView.FILES },
        bookmarkDeleter = mock(),
        episodeManager = mock(),
    ).apply { listener = recordingListener() }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `selection state follows the helper`() {
        val controller = MultiSelectController(episodesHelper, scope)
        val (first, second) = List(2) { PodcastEpisode(uuid = "episode-$it", publishedDate = Date()) }

        controller.onLongPress(first, activity.supportFragmentManager)
        controller.toggle(second)
        idle()

        assertTrue(controller.isMultiSelecting.value)
        assertEquals(listOf(first, second), controller.selected.value)
        assertEquals(2, controller.selectedCount.value)
        assertTrue(controller.isSelected(second))

        controller.deselectAllInList(listOf(first))
        idle()
        assertEquals(listOf(second), controller.selected.value)

        controller.exit()
        idle()
        assertFalse(controller.isMultiSelecting.value)
        assertEquals(0, controller.selectedCount.value)
    }

    @Test
    fun `episode toolbar items use the bottom sheet and the screen source`() {
        val controller = MultiSelectController(episodesHelper, scope, SourceView.LISTENING_HISTORY)

        controller.onLongPress(PodcastEpisode(uuid = "episode", publishedDate = Date()), activity.supportFragmentManager)
        idle()

        val items = controller.toolbarItems.value
        assertEquals(listOf(R.id.menu_download, R.id.menu_archive, R.id.menu_mark_played, R.id.menu_playnext), items.icons.map { it.actionId })
        assertTrue((items.overflow as MultiSelectToolbarOverflow.BottomSheet).showRemoveListeningHistory)
    }

    @Test
    fun `bookmark toolbar items use the overflow menu and clicks reach the helper`() {
        val controller = MultiSelectController(bookmarksHelper, scope)

        controller.onLongPress(Bookmark(uuid = "bookmark"), activity.supportFragmentManager)
        idle()
        val handled = controller.onMenuItemSelected(R.id.menu_select_all, activity.resources, activity)

        assertEquals(listOf(R.id.menu_share), (controller.toolbarItems.value.overflow as MultiSelectToolbarOverflow.Menu).items.map { it.actionId })
        assertTrue(handled)
        assertEquals(listOf("selectAll"), calls)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun <T> recordingListener() = object : MultiSelectHelper.Listener<T> {
        override fun multiSelectSelectAll() {
            calls += "selectAll"
        }

        override fun multiSelectSelectNone() = Unit
        override fun multiSelectSelectAllUp(multiSelectable: T) = Unit
        override fun multiSelectSelectAllDown(multiSelectable: T) = Unit
        override fun multiDeselectAllBelow(multiSelectable: T) = Unit
        override fun multiDeselectAllAbove(multiSelectable: T) = Unit
    }
}
