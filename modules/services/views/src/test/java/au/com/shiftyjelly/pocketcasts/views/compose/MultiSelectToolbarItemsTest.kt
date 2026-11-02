package au.com.shiftyjelly.pocketcasts.views.compose

import android.os.Looper
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.widget.ActionMenuView
import androidx.core.view.children
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.dialog.HiltShimActivity
import au.com.shiftyjelly.pocketcasts.views.dialog.HiltShimApplication
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectAction
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectToolbar
import com.automattic.eventhorizon.EventHorizon
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltShimApplication::class)
class MultiSelectToolbarItemsTest {
    private val activity = Robolectric.buildActivity(HiltShimActivity::class.java).setup().get()

    @Test
    fun `episodes show the first four actions and send the rest to the bottom sheet`() {
        val items = toolbarItems(episodesHelper(), listOf(PodcastEpisode(uuid = "a", publishedDate = Date())))

        assertEquals(listOf(R.id.menu_download, R.id.menu_archive, R.id.menu_mark_played, R.id.menu_playnext), items.icons.map { it.actionId })
        assertEquals(
            MultiSelectToolbarOverflow.BottomSheet(
                actionIds = listOf(UR.id.menu_add_to_playlist, R.id.menu_playlast, R.id.menu_star, R.id.menu_share),
                showRemoveListeningHistory = false,
            ),
            items.overflow,
        )
    }

    @Test
    fun `episodes use contextual actions for mixed selections`() {
        val selection = listOf(
            PodcastEpisode(uuid = "a", publishedDate = Date(), isArchived = true, isStarred = true),
            UserEpisode(uuid = "b", publishedDate = Date()),
        )

        val items = toolbarItems(episodesHelper(), selection)

        assertEquals(listOf(R.id.menu_download, UR.id.menu_unarchive, R.id.menu_mark_played, R.id.menu_playnext), items.icons.map { it.actionId })
        assertEquals(listOf(R.id.menu_playlast), (items.overflow as MultiSelectToolbarOverflow.BottomSheet).actionIds)
    }

    @Test
    fun `episodes follow the user order`() {
        val helper = episodesHelper(order = listOf("share", "star", "play_last"))

        val items = toolbarItems(helper, listOf(PodcastEpisode(uuid = "a", publishedDate = Date())))

        assertEquals(listOf(R.id.menu_share, R.id.menu_star, R.id.menu_playlast, R.id.menu_download), items.icons.map { it.actionId })
    }

    @Test
    fun `remove listening history is only an icon on the listening history screen`() {
        val played = listOf(PodcastEpisode(uuid = "a", publishedDate = Date(), lastPlaybackInteraction = 1L))

        val history = toolbarItems(episodesHelper(order = listOf("listening_history")), played, SourceView.LISTENING_HISTORY)
        val filters = toolbarItems(episodesHelper(order = listOf("listening_history")), played, SourceView.FILTERS)

        assertEquals(UR.id.menu_remove_listening_history, history.icons.first().actionId)
        assertEquals(true, (history.overflow as MultiSelectToolbarOverflow.BottomSheet).showRemoveListeningHistory)
        assertEquals(R.id.menu_download, filters.icons.first().actionId)
        val filtersOverflow = filters.overflow as MultiSelectToolbarOverflow.BottomSheet
        assertEquals(UR.id.menu_remove_listening_history, filtersOverflow.actionIds.first())
        assertEquals(false, filtersOverflow.showRemoveListeningHistory)
    }

    @Test
    fun `a single shareable bookmark shows share edit and delete`() {
        val items = toolbarItems(bookmarksHelper(), listOf(Bookmark(uuid = "a")))

        assertEquals(listOf(R.id.menu_share, UR.id.menu_edit, R.id.menu_delete), items.icons.map { it.actionId })
        assertEquals(listOf(R.id.menu_select_all), (items.overflow as MultiSelectToolbarOverflow.Menu).items.map { it.actionId })
    }

    @Test
    fun `several bookmarks keep the hidden share and edit in the overflow menu`() {
        val items = toolbarItems(bookmarksHelper(), listOf(Bookmark(uuid = "a"), Bookmark(uuid = "b")))

        assertEquals(listOf(R.id.menu_delete, R.id.menu_select_all), items.icons.map { it.actionId })
        assertEquals(listOf(R.id.menu_share, UR.id.menu_edit), (items.overflow as MultiSelectToolbarOverflow.Menu).items.map { it.actionId })
    }

    @Test
    fun `a bookmark from files cannot be shared`() {
        val items = toolbarItems(bookmarksHelper(SourceView.FILES), listOf(Bookmark(uuid = "a")))

        assertEquals(listOf(UR.id.menu_edit, R.id.menu_delete, R.id.menu_select_all), items.icons.map { it.actionId })
        assertEquals(listOf(R.id.menu_share), (items.overflow as MultiSelectToolbarOverflow.Menu).items.map { it.actionId })
    }

    @Test
    fun `up next uses the static menu split`() {
        val items = staticMultiSelectToolbarItems(activity.resources, R.menu.menu_multiselect_upnext)
        val toolbar = setUpToolbar(episodesHelper(), R.menu.menu_multiselect_upnext, null)

        assertEquals(listOf(R.id.menu_archive, R.id.menu_removefromupnext, R.id.menu_download, R.id.menu_mark_played), items.icons.map { it.actionId })
        assertEquals(toolbar.iconIds(), items.icons.map { it.actionId })
        assertEquals(toolbar.menuIds() - toolbar.iconIds().toSet(), (items.overflow as MultiSelectToolbarOverflow.Menu).items.map { it.actionId })
        assertEquals(toolbar.menu.findItem(R.id.menu_movetotop).title.toString(), activity.getString(items.overflow.items.first().title))
    }

    private fun <T> toolbarItems(helper: MultiSelectHelper<T>, selection: List<T>, sourceView: SourceView? = null): MultiSelectToolbarItems {
        helper.isMultiSelecting = true
        selection.forEach(helper::select)
        val toolbar = setUpToolbar(helper, null, sourceView)
        val actions = helper.toolbarActions.value.orEmpty()
        val style = if (helper is MultiSelectBookmarksHelper) MultiSelectOverflowStyle.Menu else MultiSelectOverflowStyle.BottomSheet
        val items = multiSelectToolbarItems(actions, helper.maxToolbarIcons, style, sourceView)

        assertEquals(toolbar.iconIds() - R.id.menu_overflow, items.icons.map { it.actionId })
        when (val overflow = items.overflow) {
            is MultiSelectToolbarOverflow.BottomSheet -> assertEquals(toolbar.sheetActionIds(), overflow.actionIds)
            is MultiSelectToolbarOverflow.Menu -> assertEquals(toolbar.menuIds() - toolbar.iconIds().toSet(), overflow.items.map { it.actionId })
            MultiSelectToolbarOverflow.None -> Unit
        }
        return items
    }

    private fun setUpToolbar(helper: MultiSelectHelper<*>, menuRes: Int?, sourceView: SourceView?): MultiSelectToolbar {
        val toolbar = MultiSelectToolbar(activity)
        activity.setContentView(toolbar)
        toolbar.setup(activity, helper, menuRes, activity, sourceView, includeStatusBarPadding = false)
        shadowOf(Looper.getMainLooper()).idle()
        return toolbar
    }

    private fun MultiSelectToolbar.iconIds() = children.filterIsInstance<ActionMenuView>().single()
        .children.filterIsInstance<ActionMenuItemView>().map { it.id }.toList()

    private fun MultiSelectToolbar.menuIds() = List(menu.size()) { menu.getItem(it).itemId }

    @Suppress("UNCHECKED_CAST")
    private fun MultiSelectToolbar.sheetActionIds() = MultiSelectToolbar::class.java.getDeclaredField("overflowItems")
        .apply { isAccessible = true }
        .get(this)
        .let { it as List<MultiSelectAction> }
        .map { it.actionId }

    private fun episodesHelper(order: List<String> = emptyList()) = MultiSelectEpisodesHelper(
        episodeManager = mock(),
        userEpisodeManager = mock(),
        podcastManager = mock(),
        playbackManager = mock(),
        downloadQueue = mock(),
        settings = mock<Settings> { on { multiSelectItemsFlow } doReturn MutableStateFlow(order) },
        eventHorizon = EventHorizon {},
        applicationScope = mock<CoroutineScope>(),
        crashLogging = mock(),
        shareDialogFactory = mock(),
        addToPlaylistFragmentFactory = mock(),
    )

    private fun bookmarksHelper(source: SourceView = SourceView.PLAYER) = MultiSelectBookmarksHelper(
        bookmarkManager = mock<BookmarkManager> { on { sourceView } doReturn source },
        bookmarkDeleter = mock(),
        episodeManager = mock<EpisodeManager> {
            on { findEpisodeByUuid(any()) } doReturn PodcastEpisode(uuid = "episode", publishedDate = Date())
        },
    )
}
