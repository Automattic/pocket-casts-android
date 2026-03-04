package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class Media3LibrarySessionCallbackTest {

    private lateinit var sessionCallback: Media3SessionCallback
    private lateinit var browseTreeProvider: BrowseTreeProvider
    private lateinit var callback: Media3LibrarySessionCallback
    private lateinit var mockSession: MediaLibraryService.MediaLibrarySession
    private lateinit var mockController: MediaSession.ControllerInfo
    private lateinit var mockContext: Context
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        sessionCallback = mock()
        browseTreeProvider = mock()
        mockSession = mock()
        mockController = mock()
        mockContext = mock()
        testScope = TestScope(UnconfinedTestDispatcher())

        callback = Media3LibrarySessionCallback(
            sessionCallback = sessionCallback,
            browseTreeProvider = browseTreeProvider,
            scope = testScope,
            contextProvider = { mockContext },
        )
    }

    // --- onGetLibraryRoot ---

    @Test
    fun `onGetLibraryRoot returns root media item for default params`() {
        whenever(browseTreeProvider.getRootId(isRecent = false, isSuggested = false, hasCurrentEpisode = true))
            .thenReturn(MEDIA_ID_ROOT)

        val result = callback.onGetLibraryRoot(mockSession, mockController, null)

        val libraryResult = result.get()
        assertEquals(MEDIA_ID_ROOT, libraryResult.value?.mediaId)
    }

    @Test
    fun `onGetLibraryRoot returns suggested root when params isSuggested`() {
        whenever(browseTreeProvider.getRootId(isRecent = false, isSuggested = true, hasCurrentEpisode = true))
            .thenReturn(SUGGESTED_ROOT)

        val params = MediaLibraryService.LibraryParams.Builder()
            .setSuggested(true)
            .build()

        val result = callback.onGetLibraryRoot(mockSession, mockController, params)

        val libraryResult = result.get()
        assertEquals(SUGGESTED_ROOT, libraryResult.value?.mediaId)
    }

    @Test
    fun `onGetLibraryRoot returns recent root when params isRecent`() {
        whenever(browseTreeProvider.getRootId(isRecent = true, isSuggested = false, hasCurrentEpisode = true))
            .thenReturn(RECENT_ROOT)

        val params = MediaLibraryService.LibraryParams.Builder()
            .setRecent(true)
            .build()

        val result = callback.onGetLibraryRoot(mockSession, mockController, params)

        val libraryResult = result.get()
        assertEquals(RECENT_ROOT, libraryResult.value?.mediaId)
    }

    // --- onGetChildren ---

    @Test
    fun `onGetChildren returns browse items from provider`() = runTest {
        val compatItems = listOf(
            createCompatItem("id1", "Title 1", browsable = true),
            createCompatItem("id2", "Title 2", playable = true),
        )
        whenever(browseTreeProvider.loadChildren(eq(MEDIA_ID_ROOT), any())).thenReturn(compatItems)

        val result = callback.onGetChildren(mockSession, mockController, MEDIA_ID_ROOT, 0, Int.MAX_VALUE, null)
        val libraryResult = result.get()

        val items = libraryResult.value!!
        assertEquals(2, items.size)
        assertEquals("id1", items[0].mediaId)
        assertEquals("id2", items[1].mediaId)
    }

    @Test
    fun `onGetChildren paginates correctly`() = runTest {
        val compatItems = (1..10).map { createCompatItem("id$it", "Title $it", browsable = true) }
        whenever(browseTreeProvider.loadChildren(eq(MEDIA_ID_ROOT), any())).thenReturn(compatItems)

        // Page 0, size 3
        val page0 = callback.onGetChildren(mockSession, mockController, MEDIA_ID_ROOT, 0, 3, null).get()
        assertEquals(3, page0.value!!.size)
        assertEquals("id1", page0.value!![0].mediaId)

        // Page 1, size 3
        val page1 = callback.onGetChildren(mockSession, mockController, MEDIA_ID_ROOT, 1, 3, null).get()
        assertEquals(3, page1.value!!.size)
        assertEquals("id4", page1.value!![0].mediaId)

        // Page 3, size 3 (last page, only 1 item)
        val page3 = callback.onGetChildren(mockSession, mockController, MEDIA_ID_ROOT, 3, 3, null).get()
        assertEquals(1, page3.value!!.size)
        assertEquals("id10", page3.value!![0].mediaId)
    }

    // --- onGetSearchResult ---

    @Test
    fun `onGetSearchResult returns results from provider`() = runTest {
        val compatItems = listOf(
            createCompatItem("podcast1", "My Podcast", browsable = true),
        )
        whenever(browseTreeProvider.search(eq("test"), any())).thenReturn(compatItems)

        val result = callback.onGetSearchResult(mockSession, mockController, "test", 0, Int.MAX_VALUE, null)
        val libraryResult = result.get()

        assertEquals(1, libraryResult.value!!.size)
        assertEquals("podcast1", libraryResult.value!![0].mediaId)
    }

    @Test
    fun `onGetSearchResult returns error when search returns null`() = runTest {
        whenever(browseTreeProvider.search(eq("fail"), any())).thenReturn(null)

        val result = callback.onGetSearchResult(mockSession, mockController, "fail", 0, Int.MAX_VALUE, null)
        val libraryResult = result.get()

        assertEquals(androidx.media3.session.LibraryResult.RESULT_ERROR_UNKNOWN, libraryResult.resultCode)
    }

    // --- Session callback delegation ---

    @Test
    fun `onConnect delegates to sessionCallback`() {
        val connectionResult = MediaSession.ConnectionResult.accept(
            androidx.media3.session.SessionCommands.Builder().build(),
            android.media.session.MediaSession.Token::class.java.let {
                androidx.media3.common.Player.Commands.Builder().build()
            },
        )
        whenever(sessionCallback.onConnect(any(), any())).thenReturn(connectionResult)

        val result = callback.onConnect(mockSession, mockController)

        verify(sessionCallback).onConnect(mockSession, mockController)
    }

    @Test
    fun `onCustomCommand delegates to sessionCallback`() {
        val command = SessionCommand(APP_ACTION_STAR, Bundle.EMPTY)

        callback.onCustomCommand(mockSession, mockController, command, Bundle.EMPTY)

        verify(sessionCallback).onCustomCommand(mockSession, mockController, command, Bundle.EMPTY)
    }

    // --- toMedia3MediaItem ---

    @Test
    fun `toMedia3MediaItem converts browsable item correctly`() {
        val compatItem = createCompatItem("testId", "Test Title", browsable = true)

        val result = Media3LibrarySessionCallback.toMedia3MediaItem(compatItem)

        assertEquals("testId", result.mediaId)
        assertEquals("Test Title", result.mediaMetadata.title)
        assertTrue(result.mediaMetadata.isBrowsable == true)
        assertFalse(result.mediaMetadata.isPlayable == true)
    }

    @Test
    fun `toMedia3MediaItem converts playable item correctly`() {
        val compatItem = createCompatItem("epId", "Episode Title", playable = true)

        val result = Media3LibrarySessionCallback.toMedia3MediaItem(compatItem)

        assertEquals("epId", result.mediaId)
        assertFalse(result.mediaMetadata.isBrowsable == true)
        assertTrue(result.mediaMetadata.isPlayable == true)
    }

    // --- Helpers ---

    private fun createCompatItem(
        mediaId: String,
        title: String,
        browsable: Boolean = false,
        playable: Boolean = false,
    ): MediaBrowserCompat.MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setTitle(title)
            .build()
        var flags = 0
        if (browsable) flags = flags or MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        if (playable) flags = flags or MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        return MediaBrowserCompat.MediaItem(description, flags)
    }
}
