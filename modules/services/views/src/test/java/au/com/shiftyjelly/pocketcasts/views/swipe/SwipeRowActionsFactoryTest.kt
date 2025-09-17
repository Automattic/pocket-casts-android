package au.com.shiftyjelly.pocketcasts.views.swipe

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SwipeRowActionsFactoryTest {
    private var upNextAction = Settings.UpNextAction.PLAY_NEXT
    private var isEpisodeInUpNext = false

    private lateinit var factory: SwipeRowActions.Factory

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Before
    fun setup() {
        FeatureFlag.setEnabled(Feature.PLAYLISTS_REBRANDING, true)

        val upNextSetting = mock<UserSetting<Settings.UpNextAction>> {
            on { value } doAnswer { upNextAction }
        }
        val settings = mock<Settings> {
            on { upNextSwipe } doReturn upNextSetting
        }
        val queue = mock<UpNextQueue> {
            on { contains(any()) } doAnswer { isEpisodeInUpNext }
        }
        factory = SwipeRowActions.Factory(settings, queue, makeFlagImmutable = false)
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
                rtl3 = SwipeAction.AddToPlaylist,
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
                rtl3 = SwipeAction.AddToPlaylist,
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
                rtl3 = SwipeAction.AddToPlaylist,
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
                rtl3 = SwipeAction.AddToPlaylist,
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
                rtl3 = SwipeAction.AddToPlaylist,
            ),
            factory.availablePlaylistEpisode(Playlist.Type.Smart, archivedEpisode),
        )
        assertEquals(
            "unarchived, in queue",
            SwipeRowActions(
                ltr1 = SwipeAction.RemoveFromUpNext,
                rtl1 = SwipeAction.Archive,
                rtl2 = SwipeAction.Share,
                rtl3 = SwipeAction.AddToPlaylist,
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
                rtl3 = SwipeAction.AddToPlaylist,
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
                rtl3 = SwipeAction.AddToPlaylist,
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
                rtl3 = SwipeAction.AddToPlaylist,
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
                rtl3 = SwipeAction.AddToPlaylist,
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
                rtl3 = SwipeAction.AddToPlaylist,
            ),
            factory.podcastEpisode(archivedEpisode),
        )
        assertEquals(
            "unarchived, in queue",
            SwipeRowActions(
                ltr1 = SwipeAction.RemoveFromUpNext,
                rtl1 = SwipeAction.Archive,
                rtl2 = SwipeAction.Share,
                rtl3 = SwipeAction.AddToPlaylist,
            ),
            factory.podcastEpisode(unarchivedEpisode),
        )
    }

    @Test
    fun `user episode`() {
        val userEpisode = UserEpisode(
            uuid = "",
            publishedDate = Date(),
        )

        isEpisodeInUpNext = false
        upNextAction = Settings.UpNextAction.PLAY_NEXT
        assertEquals(
            "play next",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextTop,
                ltr2 = SwipeAction.AddToUpNextBottom,
                rtl1 = SwipeAction.DeleteUserEpisode,
            ),
            factory.userEpisode(userEpisode),
        )

        isEpisodeInUpNext = false
        upNextAction = Settings.UpNextAction.PLAY_LAST
        assertEquals(
            "play last",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextBottom,
                ltr2 = SwipeAction.AddToUpNextTop,
                rtl1 = SwipeAction.DeleteUserEpisode,
            ),
            factory.userEpisode(userEpisode),
        )

        isEpisodeInUpNext = true
        assertEquals(
            "in queue",
            SwipeRowActions(
                ltr1 = SwipeAction.RemoveFromUpNext,
                rtl1 = SwipeAction.DeleteUserEpisode,
            ),
            factory.userEpisode(userEpisode),
        )
    }

    @Test
    fun `up next episode`() {
        val podcastEpisode = PodcastEpisode(uuid = "", publishedDate = Date())
        val userEpisode = UserEpisode(uuid = "", publishedDate = Date())

        // Up Next shouldn't respect default up next action.
        // Set it to the opposite value.
        upNextAction = Settings.UpNextAction.PLAY_NEXT

        assertEquals(
            "podcast episode",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextTop,
                ltr2 = SwipeAction.AddToUpNextBottom,
                rtl1 = SwipeAction.RemoveFromUpNext,
                rtl2 = SwipeAction.AddToPlaylist,
            ),
            factory.upNextEpisode(podcastEpisode),
        )
        assertEquals(
            "user episode",
            SwipeRowActions(
                ltr1 = SwipeAction.AddToUpNextTop,
                ltr2 = SwipeAction.AddToUpNextBottom,
                rtl1 = SwipeAction.RemoveFromUpNext,
            ),
            factory.upNextEpisode(userEpisode),
        )
    }

    @Test
    fun `actions for disabled playlists`() {
        FeatureFlag.setEnabled(Feature.PLAYLISTS_REBRANDING, false)

        val podcastEpisode = PodcastEpisode(uuid = "", publishedDate = Date())
        val userEpisode = UserEpisode(uuid = "", publishedDate = Date())

        assertTrue(SwipeAction.AddToPlaylist !in factory.podcastEpisode(podcastEpisode))
        assertTrue(SwipeAction.AddToPlaylist !in factory.availablePlaylistEpisode(Playlist.Type.Smart, podcastEpisode))
        assertTrue(SwipeAction.AddToPlaylist !in factory.availablePlaylistEpisode(Playlist.Type.Manual, podcastEpisode))
        assertTrue(SwipeAction.AddToPlaylist !in factory.upNextEpisode(podcastEpisode))
        assertTrue(SwipeAction.AddToPlaylist !in factory.upNextEpisode(userEpisode))
        assertTrue(SwipeAction.AddToPlaylist !in factory.userEpisode(userEpisode))
    }
}
