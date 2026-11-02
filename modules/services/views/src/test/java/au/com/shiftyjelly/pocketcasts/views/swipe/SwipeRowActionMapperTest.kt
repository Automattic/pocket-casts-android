package au.com.shiftyjelly.pocketcasts.views.swipe

import android.view.ContextThemeWrapper
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowAction
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowState
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@RunWith(RobolectricTestRunner::class)
class SwipeRowActionMapperTest {
    private val lightContext = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), UR.style.ThemeLight)
    private val darkContext = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), UR.style.ThemeDark)

    private val upNextSwipeSetting = mock<UserSetting<Settings.UpNextAction>> {
        on { value } doReturn Settings.UpNextAction.PLAY_NEXT
    }

    private val factory = SwipeRowActions.Factory(
        settings = mock<Settings> { on { upNextSwipe } doReturn upNextSwipeSetting },
        queue = mock<UpNextQueue> { on { contains(any()) } doReturn false },
    )

    private val episode = PodcastEpisode(uuid = "", publishedDate = Date())

    @Test
    fun `maps icon, description and colours from the swipe action`() {
        val action = SwipeAction.AddToUpNextTop.toSwipeRowAction(lightContext) {}

        assertEquals(IR.drawable.ic_upnext_movetotop, action.iconId)
        assertEquals("Play next in Up Next", action.contentDescription)
        assertEquals(Color(0xFF187ADC), action.backgroundColor)
        assertEquals(Color.White, action.iconTint)
        assertTrue(action.isFullSwipeEnabled)
    }

    @Test
    fun `background follows the theme`() {
        assertEquals(Color(0xFF4C5052), SwipeAction.Archive.toSwipeRowAction(lightContext) {}.backgroundColor)
        assertEquals(Color(0xFF7C8A94), SwipeAction.Archive.toSwipeRowAction(darkContext) {}.backgroundColor)
    }

    @Test
    fun `podcast episode`() {
        assertActions(
            factory.podcastEpisode(episode),
            leading = listOf("Play next in Up Next", "Play last in Up Next"),
            trailing = listOf("Archive", "Share", "Add to playlist"),
        )
    }

    @Test
    fun `manual playlist episode`() {
        assertActions(
            factory.availablePlaylistEpisode(Playlist.Type.Manual, episode),
            leading = listOf("Play next in Up Next", "Play last in Up Next"),
            trailing = listOf("Remove from playlist", "Archive", "Share"),
        )
    }

    @Test
    fun `smart playlist episode`() {
        assertActions(
            factory.availablePlaylistEpisode(Playlist.Type.Smart, episode),
            leading = listOf("Play next in Up Next", "Play last in Up Next"),
            trailing = listOf("Archive", "Share", "Add to playlist"),
        )
    }

    @Test
    fun `unavailable playlist episode`() {
        assertActions(
            factory.unavailablePlaylistEpisode(),
            leading = emptyList(),
            trailing = listOf("Remove from playlist"),
        )
    }

    @Test
    fun `user episode`() {
        assertActions(
            factory.userEpisode(UserEpisode(uuid = "", publishedDate = Date())),
            leading = listOf("Play next in Up Next", "Play last in Up Next"),
            trailing = listOf("Delete"),
        )
    }

    @Test
    fun `up next episode`() {
        assertActions(
            factory.upNextEpisode(episode),
            leading = listOf("Play next in Up Next", "Play last in Up Next"),
            trailing = listOf("Remove from Up Next", "Add to playlist"),
        )
    }

    @Test
    fun `clicking a mapped action settles the row and reports its swipe action`() {
        val swipeState = mock<SwipeRowState>()
        val clicked = mutableListOf<SwipeAction>()

        factory.podcastEpisode(episode)
            .trailingSwipeRowActions(lightContext, clicked::add)
            .forEach { it.onClick(swipeState) }

        assertEquals(listOf(SwipeAction.Archive, SwipeAction.Share, SwipeAction.AddToPlaylist), clicked)
        verify(swipeState, times(3)).settle()
    }

    private fun assertActions(actions: SwipeRowActions, leading: List<String>, trailing: List<String>) {
        assertEquals(leading, actions.leadingSwipeRowActions(lightContext) {}.map(SwipeRowAction::contentDescription))
        assertEquals(trailing, actions.trailingSwipeRowActions(lightContext) {}.map(SwipeRowAction::contentDescription))
    }
}
