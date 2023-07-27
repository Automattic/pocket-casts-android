package au.com.shiftyjelly.pocketcasts.views.helper

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SwipeButtonLayoutViewModelTest {

    @Mock private lateinit var playbackManager: PlaybackManager

    @Mock private lateinit var upNextQueue: UpNextQueue

    private val buttons: SwipeButtonLayoutViewModel.SwipeButtons =
        SwipeButtonLayoutViewModel.SwipeButtons(
            addToUpNextTop = mock(name = "addToUpNextTop"),
            addToUpNextBottom = mock(name = "addToUpNextBottom"),
            removeFromUpNext = mock(name = "removeFromUpNext"),
            archive = mock(name = "archive"),
            deleteFile = mock(name = "deleteFile"),
            share = mock(name = "share"),
        )

    private lateinit var testSubject: SwipeButtonLayoutViewModel

    @Before
    fun setup() {
        testSubject = SwipeButtonLayoutViewModel(
            analyticsTracker = mock(),
            context = mock(),
            episodeAnalytics = mock(),
            episodeManager = mock(),
            playbackManager = playbackManager,
            podcastManager = mock(),
            userEpisodeManager = mock(),
        )
    }

    /*
     * Add/Remove from queue
     */

    @Test
    fun `show queue buttons in user settings order when not on up next screen when defaulting to top`() {
        val topDefault = getSwipeButtonLayout(
            onUpNextScreen = false,
            defaultUpNextSwipeAction = Settings.UpNextAction.PLAY_NEXT
        )
        assertEquals(buttons.addToUpNextTop, topDefault.leftPrimary())
        assertEquals(buttons.addToUpNextBottom, topDefault.leftSecondary())
    }

    @Test
    fun `show queue buttons in user setting order when not on up next screen when defaulting to bottom`() {
        val bottomDefault = getSwipeButtonLayout(
            onUpNextScreen = false,
            defaultUpNextSwipeAction = Settings.UpNextAction.PLAY_LAST
        )
        assertEquals(buttons.addToUpNextBottom, bottomDefault.leftPrimary())
        assertEquals(buttons.addToUpNextTop, bottomDefault.leftSecondary())
    }

    @Test
    fun `ignores user setting for queue actions order when on up next screen`() {
        val verifyTopFirst = { layout: SwipeButtonLayout ->
            assertEquals(buttons.addToUpNextTop, layout.leftPrimary())
            assertEquals(buttons.addToUpNextBottom, layout.leftSecondary())
        }

        val defaultNext = getSwipeButtonLayout(
            onUpNextScreen = true,
            defaultUpNextSwipeAction = Settings.UpNextAction.PLAY_NEXT
        )
        verifyTopFirst(defaultNext)

        val defaultLast = getSwipeButtonLayout(
            onUpNextScreen = true,
            defaultUpNextSwipeAction = Settings.UpNextAction.PLAY_LAST
        )
        verifyTopFirst(defaultLast)
    }

    @Test
    fun `if episode is queued, shows remove queue button on left when not on Up Next screen`() {
        val verifyRemoveButton = { layout: SwipeButtonLayout ->
            assertEquals(buttons.removeFromUpNext, layout.leftPrimary())
            assertNull(layout.leftSecondary())
        }

        val withPodcastEpisode = getSwipeButtonLayout(
            onUpNextScreen = false,
            episode = mock<PodcastEpisode>(),
            episodeInUpNext = true
        )
        verifyRemoveButton(withPodcastEpisode)

        val withUserEpisode = getSwipeButtonLayout(
            onUpNextScreen = false,
            episode = mock<UserEpisode>(),
            episodeInUpNext = true
        )
        verifyRemoveButton(withUserEpisode)
    }

    @Test
    fun `shows remove from queue button always on right on Up Next screen`() {
        val verifyRemoveButton = { layout: SwipeButtonLayout ->
            // remove button is right primary...
            assertEquals(buttons.removeFromUpNext, layout.rightPrimary())

            // ...and nowhere else
            assertNull(layout.rightSecondary())
            assertNotEquals(buttons.removeFromUpNext, layout.leftPrimary())
            assertNotEquals(buttons.removeFromUpNext, layout.leftSecondary())
        }

        val withPodcastEpisode = getSwipeButtonLayout(
            episode = mock<PodcastEpisode>(),
            onUpNextScreen = true,
            episodeInUpNext = true
        )
        verifyRemoveButton(withPodcastEpisode)

        val withUserEpisode = getSwipeButtonLayout(
            episode = mock<UserEpisode>(),
            onUpNextScreen = true,
            episodeInUpNext = true
        )
        verifyRemoveButton(withUserEpisode)
    }

    /*
     * Delete / archive buttons
     */

    @Test
    fun `shows archive button for podcast episodes when not on Up Next screen`() {
        val verifyArchiveButton = { layout: SwipeButtonLayout ->
            // archive button is right primary...
            assertEquals(buttons.archive, layout.rightPrimary())

            // ...and nowhere else
            assertNotEquals(buttons.archive, layout.rightSecondary())
            assertNotEquals(buttons.archive, layout.leftPrimary())
            assertNotEquals(buttons.archive, layout.leftSecondary())

            // ...and there's no deleteFile button
            assertNotEquals(buttons.deleteFile, layout.rightSecondary())
            assertNotEquals(buttons.deleteFile, layout.leftPrimary())
            assertNotEquals(buttons.deleteFile, layout.leftSecondary())
        }

        val default = getSwipeButtonLayout(
            episode = mock<PodcastEpisode>(),
        )
        verifyArchiveButton(default)

        val inUpNextQueue = getSwipeButtonLayout(
            episode = mock<PodcastEpisode>(),
            episodeInUpNext = true
        )
        verifyArchiveButton(inUpNextQueue)
    }

    @Test
    fun `shows archive button for user episodes when not on Up Next Screen`() {
        val verifyDeleteFileButton = { layout: SwipeButtonLayout ->
            // deleteFile button is right primary...
            assertEquals(buttons.deleteFile, layout.rightPrimary())

            // ...and nowhere else
            assertNotEquals(buttons.deleteFile, layout.rightSecondary())
            assertNotEquals(buttons.deleteFile, layout.leftPrimary())
            assertNotEquals(buttons.deleteFile, layout.leftSecondary())

            // ...and there's no archive button
            assertNotEquals(buttons.archive, layout.rightSecondary())
            assertNotEquals(buttons.archive, layout.leftPrimary())
            assertNotEquals(buttons.archive, layout.leftSecondary())
        }

        val default = getSwipeButtonLayout(
            episode = mock<UserEpisode>(),
        )
        verifyDeleteFileButton(default)

        val inUpNextQueue = getSwipeButtonLayout(
            episode = mock<UserEpisode>(),
            episodeInUpNext = true
        )
        verifyDeleteFileButton(inUpNextQueue)
    }

    /*
     * Share Button
     */

    @Test
    fun `shows share button when not on Up Next Screen`() {
        val withShare = getSwipeButtonLayout(
            onUpNextScreen = false,
            showShareButton = true
        )
        assertEquals(buttons.share, withShare.rightSecondary())
    }

    @Test
    fun `does not show share button if showShareButton is false`() {
        val shareSetToFalse = getSwipeButtonLayout(
            onUpNextScreen = false,
            showShareButton = false
        )
        verifyNoShareButton(shareSetToFalse)
    }

    @Test
    fun `does not show share button for User Episodes`() {
        val withUserEpisode = getSwipeButtonLayout(
            episode = mock<UserEpisode>(),
            onUpNextScreen = false,
            showShareButton = false
        )
        verifyNoShareButton(withUserEpisode)
    }

    @Test
    fun `does not show share button on Up Next screen, even if showShareButton is true`() {
        val layout = getSwipeButtonLayout(
            episode = mock<PodcastEpisode>(),
            onUpNextScreen = true,
            showShareButton = true
        )
        verifyNoShareButton(layout)
    }

    private fun verifyNoShareButton(layout: SwipeButtonLayout) {
        assertNotEquals(buttons.share, layout.leftPrimary())
        assertNotEquals(buttons.share, layout.leftSecondary())
        assertNotEquals(buttons.share, layout.rightPrimary())
        assertNotEquals(buttons.share, layout.rightSecondary())
    }

    /*
     * Helpers
     */

    private fun getSwipeButtonLayout(
        episode: BaseEpisode = mock<PodcastEpisode>(),
        episodeInUpNext: Boolean = false,
        showShareButton: Boolean = true,
        onUpNextScreen: Boolean = false,
        defaultUpNextSwipeAction: Settings.UpNextAction = Settings.UpNextAction.PLAY_NEXT,
    ): SwipeButtonLayout {

        if (!onUpNextScreen) {
            // Only stub these when we're not on the up next screen. Otherwise mockito gets upset about
            // unnecessary stubbings
            whenever(playbackManager.upNextQueue).thenReturn(upNextQueue)
            whenever(upNextQueue.contains(episode.uuid)).thenReturn(episodeInUpNext)
        }

        val swipeSource = if (onUpNextScreen) {
            EpisodeItemTouchHelper.SwipeSource.UP_NEXT
        } else {
            // All of these are treated the same, so picking one at random
            // to make sure they're all (kind of) covered. Better would be
            // to convert this to a parameterized test at some point.
            listOf(
                EpisodeItemTouchHelper.SwipeSource.PODCAST_DETAILS,
                EpisodeItemTouchHelper.SwipeSource.FILES,
                EpisodeItemTouchHelper.SwipeSource.FILTERS,
                EpisodeItemTouchHelper.SwipeSource.DOWNLOADS,
                EpisodeItemTouchHelper.SwipeSource.LISTENING_HISTORY,
                EpisodeItemTouchHelper.SwipeSource.STARRED,
            ).random()
        }
        return testSubject.getSwipeButtonLayout(
            episode = episode,
            swipeSource = swipeSource,
            showShareButton = showShareButton,
            defaultUpNextSwipeAction = { defaultUpNextSwipeAction },
            buttons = buttons
        )
    }
}
