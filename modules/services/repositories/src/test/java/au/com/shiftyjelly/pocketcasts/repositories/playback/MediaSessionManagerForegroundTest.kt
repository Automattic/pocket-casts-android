package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.ComponentName
import android.content.IntentFilter
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.EventHorizon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MediaSessionManagerForegroundTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context = RuntimeEnvironment.getApplication()

    private lateinit var manager: MediaSessionManager

    @Before
    fun setUp() {
        // Register a media browser service so the gate can resolve a component and dispatch a start.
        val component = ComponentName(context, PlaybackService::class.java)
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.addServiceIfNotPresent(component)
        shadowPackageManager.addIntentFilterForService(component, IntentFilter("android.media.browse.MediaBrowserService"))

        manager = MediaSessionManager(
            playbackManager = mock(),
            podcastManager = mock<PodcastManager>(),
            episodeManager = mock<EpisodeManager>(),
            playlistManager = mock<PlaylistManager>(),
            settings = mock<Settings>(),
            context = context,
            eventHorizon = mock<EventHorizon>(),
            bookmarkManager = mock<BookmarkManager>(),
            browseTreeProvider = mock(),
            applicationScope = CoroutineScope(testDispatcher),
        )
    }

    @Test
    fun `setServiceForeground updates the foreground state`() {
        assertFalse(manager.isServiceForeground.value)

        manager.setServiceForeground(true)
        assertTrue(manager.isServiceForeground.value)

        manager.setServiceForeground(false)
        assertFalse(manager.isServiceForeground.value)
    }

    @Test
    fun `ensureForegroundServiceStarted returns immediately when already foreground`() = runTest(testDispatcher) {
        FeatureFlag.setEnabled(Feature.FOREGROUND_BEFORE_PLAYBACK, true)
        manager.setServiceForeground(true)

        assertTrue(manager.ensureForegroundServiceStarted(context, timeoutMs = 50))
    }

    @Test
    fun `ensureForegroundServiceStarted resolves once the service reaches foreground`() = runTest(testDispatcher) {
        FeatureFlag.setEnabled(Feature.FOREGROUND_BEFORE_PLAYBACK, true)

        val result = async { manager.ensureForegroundServiceStarted(context, timeoutMs = 5000) }
        manager.setServiceForeground(true)

        assertTrue(result.await())
    }

    @Test
    fun `ensureForegroundServiceStarted returns false when the service never reaches foreground`() = runTest(testDispatcher) {
        FeatureFlag.setEnabled(Feature.FOREGROUND_BEFORE_PLAYBACK, true)

        assertFalse(manager.ensureForegroundServiceStarted(context, timeoutMs = 50))
    }
}
