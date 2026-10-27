package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.os.Looper
import android.view.View
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadType
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.HiltShimActivity
import au.com.shiftyjelly.pocketcasts.views.dialog.HiltShimApplication
import com.automattic.eventhorizon.EpisodeBulkArchivedEvent
import com.automattic.eventhorizon.EpisodeBulkMarkedAsPlayedEvent
import com.automattic.eventhorizon.EventHorizon
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltShimApplication::class)
class MultiSelectEpisodesHelperTest {
    private val activity = Robolectric.buildActivity(HiltShimActivity::class.java).setup().get()
    private val fragmentManager = activity.supportFragmentManager
    private val resources = activity.resources

    private val episodeManager = mock<EpisodeManager>()
    private val playbackManager = mock<PlaybackManager>()
    private val downloadQueue = mock<DownloadQueue>()
    private val settings = mock<Settings> {
        on { multiSelectItemsFlow } doReturn MutableStateFlow(emptyList())
        on { getMaxUpNextEpisodes() } doReturn 2
    }
    private val eventSink = TestEventSink()

    private val helper = MultiSelectEpisodesHelper(
        episodeManager = episodeManager,
        userEpisodeManager = mock(),
        podcastManager = mock(),
        playbackManager = playbackManager,
        downloadQueue = downloadQueue,
        settings = settings,
        eventHorizon = EventHorizon(eventSink),
        applicationScope = mock<CoroutineScope>(),
        crashLogging = mock(),
        shareDialogFactory = mock(),
        addToPlaylistFragmentFactory = mock(),
    ).apply { source = SourceView.PODCAST_SCREEN }

    @Test
    fun `archiving up to the warning limit runs without confirmation`() {
        val episodes = selectPodcastEpisodes(3) + selectUserEpisodes(2)

        helper.archive(resources = resources, fragmentManager = fragmentManager)
        awaitMultiSelectClosed()

        assertNull(fragmentManager.findFragmentByTag("confirm_archive_all_"))
        verifyBlocking(episodeManager) { archiveAllInList(episodes.take(3).map { it as PodcastEpisode }, playbackManager) }
        assertEquals(EpisodeBulkArchivedEvent(count = 3, source = SourceView.PODCAST_SCREEN.analyticsValue), eventSink.pollEvent())
    }

    @Test
    fun `archiving above the warning limit requires confirmation`() {
        val episodes = selectPodcastEpisodes(4)

        helper.archive(resources = resources, fragmentManager = fragmentManager)
        val dialog = awaitDialog("confirm_archive_all_")

        verifyBlocking(episodeManager, never()) { archiveAllInList(any(), any()) }
        assertTrue(eventSink.isEmpty())
        assertTrue(helper.isMultiSelecting)

        dialog.requireView().findViewById<View>(R.id.btnConfirm).performClick()
        awaitMultiSelectClosed()

        verifyBlocking(episodeManager) { archiveAllInList(episodes, playbackManager) }
        assertEquals(EpisodeBulkArchivedEvent(count = 4, source = SourceView.PODCAST_SCREEN.analyticsValue), eventSink.pollEvent())
    }

    @Test
    fun `marking up to the warning limit as played runs without confirmation`() {
        val episodes = selectPodcastEpisodes(2) + selectUserEpisodes(1)

        helper.markAsPlayed(resources = resources, fragmentManager = fragmentManager)
        awaitMultiSelectClosed()

        assertNull(fragmentManager.findFragmentByTag("confirm_played_all_"))
        verifyBlocking(episodeManager) { markAllAsPlayed(episodes, playbackManager, helper.podcastManager) }
        assertEquals(EpisodeBulkMarkedAsPlayedEvent(count = 3, source = SourceView.PODCAST_SCREEN.analyticsValue), eventSink.pollEvent())
    }

    @Test
    fun `marking above the warning limit as played requires confirmation`() {
        val episodes = selectPodcastEpisodes(3) + selectUserEpisodes(1)

        helper.markAsPlayed(resources = resources, fragmentManager = fragmentManager)
        val dialog = awaitDialog("confirm_played_all_")

        verifyBlocking(episodeManager, never()) { markAllAsPlayed(any(), any(), any()) }
        assertTrue(eventSink.isEmpty())

        dialog.requireView().findViewById<View>(R.id.btnConfirm).performClick()
        awaitMultiSelectClosed()

        verifyBlocking(episodeManager) { markAllAsPlayed(episodes, playbackManager, helper.podcastManager) }
        assertEquals(EpisodeBulkMarkedAsPlayedEvent(count = 4, source = SourceView.PODCAST_SCREEN.analyticsValue), eventSink.pollEvent())
    }

    @Test
    fun `downloading fewer than five episodes runs without confirmation`() {
        val episodes = selectPodcastEpisodes(4)

        helper.download(resources, fragmentManager)
        idle()

        assertNull(fragmentManager.findFragmentByTag("multiselect_download"))
        verify(downloadQueue).enqueueAll(episodes.map { it.uuid }, DownloadType.UserTriggered(waitForWifi = false), SourceView.PODCAST_SCREEN)
        assertFalse(helper.isMultiSelecting)
    }

    @Test
    fun `downloading above the maximum is trimmed to the first selected episodes after confirmation`() {
        val episodes = selectPodcastEpisodes(Settings.MAX_DOWNLOAD + 1)

        helper.download(resources, fragmentManager)
        val dialog = awaitDialog("multiselect_download")
        verify(downloadQueue, never()).enqueueAll(any(), any(), any())

        dialog.requireView().findViewById<View>(R.id.btnConfirm).performClick()
        idle()

        val expected = episodes.take(Settings.MAX_DOWNLOAD).map { it.uuid }
        verify(downloadQueue).enqueueAll(expected, DownloadType.UserTriggered(waitForWifi = false), SourceView.PODCAST_SCREEN)
    }

    @Test
    fun `play next and play last are trimmed to the up next maximum`() {
        val playNext = selectPodcastEpisodes(3)
        helper.onMenuItemSelected(R.id.menu_playnext, resources, activity)
        awaitMultiSelectClosed()
        verify(playbackManager).playEpisodesNext(playNext.take(2), SourceView.PODCAST_SCREEN)

        val playLast = selectPodcastEpisodes(3, prefix = "last")
        helper.onMenuItemSelected(R.id.menu_playlast, resources, activity)
        awaitMultiSelectClosed()
        verify(playbackManager).playEpisodesLast(playLast.take(2), SourceView.PODCAST_SCREEN)
    }

    @Test
    fun `selection keeps insertion order across list updates`() {
        val (first, second, third, fourth) = podcastEpisodes(4)
        helper.isMultiSelecting = true
        helper.select(third)
        helper.select(first)
        helper.select(third)

        helper.selectAllInList(listOf(first, second, fourth))

        assertEquals(listOf(third, first, second, fourth), helper.selectedListLive.value)
    }

    @Test
    fun `deselecting the last episode exits multi select`() {
        val (first, second) = selectPodcastEpisodes(2)

        helper.deselect(first)
        assertTrue(helper.isMultiSelecting)
        assertEquals(listOf(second), helper.selectedListLive.value)

        helper.deselect(second)
        assertFalse(helper.isMultiSelecting)
    }

    private fun podcastEpisodes(count: Int, prefix: String = "episode") = List(count) { PodcastEpisode(uuid = "$prefix-$it", publishedDate = Date()) }

    private fun selectPodcastEpisodes(count: Int, prefix: String = "episode") = select(podcastEpisodes(count, prefix))

    private fun selectUserEpisodes(count: Int) = select(List(count) { UserEpisode(uuid = "user-$it", publishedDate = Date()) })

    private fun <T : BaseEpisode> select(episodes: List<T>): List<T> {
        if (!helper.isMultiSelecting) helper.isMultiSelecting = true
        episodes.forEach(helper::select)
        return episodes
    }

    private fun awaitMultiSelectClosed() = awaitUntil { !helper.isMultiSelecting }

    private fun awaitDialog(tag: String): ConfirmationDialog {
        awaitUntil { fragmentManager.findFragmentByTag(tag)?.view != null }
        return fragmentManager.findFragmentByTag(tag) as ConfirmationDialog
    }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Condition not met in time" }
            Thread.sleep(10)
            idle()
        }
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
}
