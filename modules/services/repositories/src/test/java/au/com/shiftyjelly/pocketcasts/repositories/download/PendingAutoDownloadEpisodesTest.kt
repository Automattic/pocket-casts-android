package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.appreview.TestSetting
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

class PendingAutoDownloadEpisodesTest {
    private val pendingUuids = TestSetting<List<String>>(emptyList())

    private val settings = mock<Settings> {
        on { pendingAutoDownloadEpisodeUuids } doAnswer { pendingUuids }
    }

    private val pendingEpisodes = PendingAutoDownloadEpisodes(settings)

    @Test
    fun `record nothing when there are no episodes`() {
        pendingEpisodes.record(emptyList())

        assertEquals(emptyList<String>(), pendingEpisodes.all())
    }

    @Test
    fun `record episodes`() {
        pendingEpisodes.record(listOf("uuid-1", "uuid-2"))

        assertEquals(listOf("uuid-1", "uuid-2"), pendingEpisodes.all())
    }

    @Test
    fun `accumulate episodes across recordings`() {
        pendingEpisodes.record(listOf("uuid-1"))
        pendingEpisodes.record(listOf("uuid-2"))

        assertEquals(listOf("uuid-1", "uuid-2"), pendingEpisodes.all())
    }

    @Test
    fun `do not record duplicated episodes`() {
        pendingEpisodes.record(listOf("uuid-1", "uuid-2"))
        pendingEpisodes.record(listOf("uuid-2", "uuid-3"))

        assertEquals(listOf("uuid-1", "uuid-2", "uuid-3"), pendingEpisodes.all())
    }

    @Test
    fun `drop the oldest episodes when the limit is exceeded`() {
        val uuids = List(501) { index -> "uuid-$index" }

        pendingEpisodes.record(uuids)

        assertEquals(uuids.drop(1), pendingEpisodes.all())
    }

    @Test
    fun `drop the oldest episodes when the limit is exceeded across recordings`() {
        pendingEpisodes.record(List(500) { index -> "uuid-$index" })

        pendingEpisodes.record(listOf("uuid-500", "uuid-501"))

        assertEquals(List(500) { index -> "uuid-${index + 2}" }, pendingEpisodes.all())
    }

    @Test
    fun `remove only the given episodes`() {
        pendingEpisodes.record(listOf("uuid-1", "uuid-2", "uuid-3"))

        pendingEpisodes.remove(listOf("uuid-1", "uuid-3"))

        assertEquals(listOf("uuid-2"), pendingEpisodes.all())
    }

    @Test
    fun `keep episodes recorded while others are being removed`() {
        pendingEpisodes.record(listOf("uuid-1"))
        val removedByAnotherRefresh = pendingEpisodes.all()
        pendingEpisodes.record(listOf("uuid-2"))

        pendingEpisodes.remove(removedByAnotherRefresh)

        assertEquals(listOf("uuid-2"), pendingEpisodes.all())
    }

    @Test
    fun `remove nothing when there are no episodes`() {
        pendingEpisodes.record(listOf("uuid-1"))

        pendingEpisodes.remove(emptyList())

        assertEquals(listOf("uuid-1"), pendingEpisodes.all())
    }

    @Test
    fun `ignore unknown episodes when removing`() {
        pendingEpisodes.record(listOf("uuid-1"))

        pendingEpisodes.remove(listOf("uuid-2"))

        assertEquals(listOf("uuid-1"), pendingEpisodes.all())
    }

    @Test
    fun `record episodes after they are removed`() {
        pendingEpisodes.record(listOf("uuid-1"))
        pendingEpisodes.remove(listOf("uuid-1"))

        pendingEpisodes.record(listOf("uuid-2"))

        assertEquals(listOf("uuid-2"), pendingEpisodes.all())
    }
}
