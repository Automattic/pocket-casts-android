package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.EventHorizon
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MediaSessionManagerReleaseTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        applicationScope.cancel()
    }

    @Test
    fun `release keeps the Media3 observers for the session a restarted service creates`() {
        FeatureFlag.setEnabled(Feature.MEDIA3_SESSION, true)
        val manager = createManager()
        manager.startObserving()
        val observerCount = manager.disposables.size()

        manager.release()

        assertTrue(observerCount > 0)
        assertEquals(observerCount, manager.disposables.size())
    }

    private fun createManager(): MediaSessionManager {
        val upNextQueue = mock<UpNextQueue> {
            on { changesObservable } doReturn Observable.never()
        }
        val playbackManager = mock<PlaybackManager>()
        whenever(playbackManager.playbackStateRelay).thenReturn(BehaviorRelay.createDefault(PlaybackState()))
        whenever(playbackManager.upNextQueue).thenReturn(upNextQueue)

        val settings = mock<Settings>()
        val artworkConfiguration = ArtworkConfiguration(useEpisodeArtwork = false)
        whenever(settings.artworkConfiguration).thenReturn(UserSetting.Mock(artworkConfiguration, mock()))
        whenever(settings.showArtworkOnLockScreen).thenReturn(UserSetting.Mock(true, mock()))
        whenever(settings.customMediaActionsVisibility).thenReturn(UserSetting.Mock(true, mock()))
        whenever(settings.mediaControlItems).thenReturn(UserSetting.Mock(emptyList(), mock()))

        return MediaSessionManager(
            playbackManager = playbackManager,
            podcastManager = mock<PodcastManager>(),
            episodeManager = mock<EpisodeManager>(),
            playlistManager = mock<PlaylistManager>(),
            settings = settings,
            context = RuntimeEnvironment.getApplication(),
            eventHorizon = mock<EventHorizon>(),
            errorReporter = mock<PlaybackServiceErrorReporter>(),
            bookmarkManager = mock<BookmarkManager>(),
            browseTreeProvider = mock<BrowseTreeProvider>(),
            applicationScope = applicationScope,
        )
    }
}
