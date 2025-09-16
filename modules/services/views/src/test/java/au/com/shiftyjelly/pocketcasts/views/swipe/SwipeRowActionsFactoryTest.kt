package au.com.shiftyjelly.pocketcasts.views.swipe

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SwipeRowActionsFactoryTest {
    private var upNextAction = Settings.UpNextAction.PLAY_NEXT
    private var isEpisodeInUpNext = false

    private lateinit var factory: SwipeRowActions.Factory

    @Before
    fun setup() {
        val upNextSetting = mock<UserSetting<Settings.UpNextAction>> {
            on { value } doAnswer { upNextAction }
        }
        val settings = mock<Settings> {
            on { upNextSwipe } doReturn upNextSetting
        }
        val queue = mock<UpNextQueue> {
            on { contains(any()) } doAnswer { isEpisodeInUpNext }
        }
        factory = SwipeRowActions.Factory(settings, queue)
    }

    @Test
    fun `unavailable playlist episode`() {
        val actions = factory.unavailablePlaylistEpisode()

        assertEquals(
            SwipeRowActions(
                rtl1 = SwipeAction.RemoveFromPlaylist,
            ),
            actions,
        )
    }

    @Test
    fun `available smart playlist episode`() {
        val archivedEpisode = PodcastEpisode(
            isArchived = true,
            uuid = "",
            publishedDate = Date(),
        )
        val unarchivedEpisode = PodcastEpisode(
            isArchived = false,
            uuid = "",
            publishedDate = Date(),
        )

        isEpisodeInUpNext = false
        upNextAction = Settings.UpNextAction.PLAY_NEXT
        assertEquals(
            "archived, play next",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextTop,
                ltr2 = SwipeAction.AddToUpNextBottom,
                rtl1 = SwipeAction.Unarchive,
                rtl2 = SwipeAction.Share,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Smart, archivedEpisode),
        )
        assertEquals(
            "unarchived, play next",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextTop,
                ltr2 = SwipeAction.AddToUpNextBottom,
                rtl1 = SwipeAction.Archive,
                rtl2 = SwipeAction.Share,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Smart, unarchivedEpisode),
        )

        isEpisodeInUpNext = false
        upNextAction = Settings.UpNextAction.PLAY_LAST
        assertEquals(
            "archived, play last",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextBottom,
                ltr2 = SwipeAction.AddToUpNextTop,
                rtl1 = SwipeAction.Unarchive,
                rtl2 = SwipeAction.Share,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Smart, archivedEpisode),
        )
        assertEquals(
            "unarchived, play last",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextBottom,
                ltr2 = SwipeAction.AddToUpNextTop,
                rtl1 = SwipeAction.Archive,
                rtl2 = SwipeAction.Share,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Smart, unarchivedEpisode),
        )

        isEpisodeInUpNext = true
        assertEquals(
            "archived, in queue",
            SwipeRowActions(
                ltr1 = SwipeAction.RemoveFromUpNext,
                rtl1 = SwipeAction.Unarchive,
                rtl2 = SwipeAction.Share,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Smart, archivedEpisode),
        )
        assertEquals(
            "unarchived, in queue",
            SwipeRowActions(
                ltr1 = SwipeAction.RemoveFromUpNext,
                rtl1 = SwipeAction.Archive,
                rtl2 = SwipeAction.Share,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Smart, unarchivedEpisode),
        )
    }

    @Test
    fun `available manual playlist episode`() {
        val archivedEpisode = PodcastEpisode(
            isArchived = true,
            uuid = "",
            publishedDate = Date(),
        )
        val unarchivedEpisode = PodcastEpisode(
            isArchived = false,
            uuid = "",
            publishedDate = Date(),
        )

        isEpisodeInUpNext = false
        upNextAction = Settings.UpNextAction.PLAY_NEXT
        assertEquals(
            "archived, play next",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextTop,
                ltr2 = SwipeAction.AddToUpNextBottom,
                rtl1 = SwipeAction.RemoveFromPlaylist,
                rtl2 = SwipeAction.Unarchive,
                rtl3 = SwipeAction.Share,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Manual, archivedEpisode),
        )
        assertEquals(
            "unarchived, play next",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextTop,
                ltr2 = SwipeAction.AddToUpNextBottom,
                rtl1 = SwipeAction.RemoveFromPlaylist,
                rtl2 = SwipeAction.Archive,
                rtl3 = SwipeAction.Share,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Manual, unarchivedEpisode),
        )

        isEpisodeInUpNext = false
        upNextAction = Settings.UpNextAction.PLAY_LAST
        assertEquals(
            "archived, play last",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextBottom,
                ltr2 = SwipeAction.AddToUpNextTop,
                rtl1 = SwipeAction.RemoveFromPlaylist,
                rtl2 = SwipeAction.Unarchive,
                rtl3 = SwipeAction.Share,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Manual, archivedEpisode),
        )
        assertEquals(
            "unarchived, play last",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextBottom,
                ltr2 = SwipeAction.AddToUpNextTop,
                rtl1 = SwipeAction.RemoveFromPlaylist,
                rtl2 = SwipeAction.Archive,
                rtl3 = SwipeAction.Share,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Manual, unarchivedEpisode),
        )

        isEpisodeInUpNext = true
        assertEquals(
            "archived, in queue",
            SwipeRowActions(
                ltr1 = SwipeAction.RemoveFromUpNext,
                rtl1 = SwipeAction.RemoveFromPlaylist,
                rtl2 = SwipeAction.Unarchive,
                rtl3 = SwipeAction.Share,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Manual, archivedEpisode),
        )
        assertEquals(
            "unarchived, in queue",
            SwipeRowActions(
                ltr1 = SwipeAction.RemoveFromUpNext,
                rtl1 = SwipeAction.RemoveFromPlaylist,
                rtl2 = SwipeAction.Archive,
                rtl3 = SwipeAction.Share,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Manual, unarchivedEpisode),
        )
    }

    @Test
    fun `podcast episode`() {
        val archivedEpisode = PodcastEpisode(
            isArchived = true,
            uuid = "",
            publishedDate = Date(),
        )
        val unarchivedEpisode = PodcastEpisode(
            isArchived = false,
            uuid = "",
            publishedDate = Date(),
        )

        isEpisodeInUpNext = false
        upNextAction = Settings.UpNextAction.PLAY_NEXT
        assertEquals(
            "archived, play next",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextTop,
                ltr2 = SwipeAction.AddToUpNextBottom,
                rtl1 = SwipeAction.Unarchive,
                rtl2 = SwipeAction.Share,
            ),
            factory.podcastEpisode(archivedEpisode),
        )
        assertEquals(
            "unarchived, play next",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextTop,
                ltr2 = SwipeAction.AddToUpNextBottom,
                rtl1 = SwipeAction.Archive,
                rtl2 = SwipeAction.Share,
            ),
            factory.podcastEpisode(unarchivedEpisode),
        )

        isEpisodeInUpNext = false
        upNextAction = Settings.UpNextAction.PLAY_LAST
        assertEquals(
            "archived, play last",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextBottom,
                ltr2 = SwipeAction.AddToUpNextTop,
                rtl1 = SwipeAction.Unarchive,
                rtl2 = SwipeAction.Share,
            ),
            factory.podcastEpisode(archivedEpisode),
        )
        assertEquals(
            "unarchived, play last",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextBottom,
                ltr2 = SwipeAction.AddToUpNextTop,
                rtl1 = SwipeAction.Archive,
                rtl2 = SwipeAction.Share,
            ),
            factory.podcastEpisode(unarchivedEpisode),
        )

        isEpisodeInUpNext = true
        assertEquals(
            "archived, in queue",
            SwipeRowActions(
                ltr1 = SwipeAction.RemoveFromUpNext,
                rtl1 = SwipeAction.Unarchive,
                rtl2 = SwipeAction.Share,
            ),
            factory.podcastEpisode(archivedEpisode),
        )
        assertEquals(
            "unarchived, in queue",
            SwipeRowActions(
                ltr1 = SwipeAction.RemoveFromUpNext,
                rtl1 = SwipeAction.Archive,
                rtl2 = SwipeAction.Share,
            ),
            factory.podcastEpisode(unarchivedEpisode),
        )
    }
}
