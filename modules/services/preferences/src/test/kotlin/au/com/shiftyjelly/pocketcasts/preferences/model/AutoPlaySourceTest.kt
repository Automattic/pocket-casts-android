package au.com.shiftyjelly.pocketcasts.preferences.model

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoPlaySourceTest {
    @Test
    fun `create PodcastOrFilter from uuid ID`() {
        val uuid = UUID.randomUUID().toString()

        val source = AutoPlaySource.fromId(uuid)

        assertEquals(AutoPlaySource.PodcastOrFilter(uuid), source)
    }

    @Test
    fun `create None from malformed uuid ID`() {
        val malformedUuid = UUID.randomUUID().toString().replace("-", "")

        val source = AutoPlaySource.fromId(malformedUuid)

        assertEquals(AutoPlaySource.None, source)
    }

    @Test
    fun `create Files from ID`() {
        val source = AutoPlaySource.fromId("files")

        assertEquals(AutoPlaySource.Files, source)
    }

    @Test
    fun `create Downloads from ID`() {
        val source = AutoPlaySource.fromId("downloads")

        assertEquals(AutoPlaySource.Downloads, source)
    }

    @Test
    fun `create Starred from ID`() {
        val source = AutoPlaySource.fromId("starred")

        assertEquals(AutoPlaySource.Starred, source)
    }

    @Test
    fun `create PodcastOrFilter from uuid server ID`() {
        val uuid = UUID.randomUUID().toString()

        val source = AutoPlaySource.fromServerId(uuid)

        assertEquals(AutoPlaySource.PodcastOrFilter(uuid), source)
    }

    @Test
    fun `create None from malformed uuid server ID`() {
        val malformedUuid = UUID.randomUUID().toString().replace("-", "")

        val source = AutoPlaySource.fromServerId(malformedUuid)

        assertEquals(AutoPlaySource.None, source)
    }

    @Test
    fun `create Files from server ID`() {
        val source = AutoPlaySource.fromServerId("files")

        assertEquals(AutoPlaySource.Files, source)
    }

    @Test
    fun `create Downloads from server ID`() {
        val source = AutoPlaySource.fromServerId("downloads")

        assertEquals(AutoPlaySource.Downloads, source)
    }

    @Test
    fun `create Starred from server ID`() {
        val source = AutoPlaySource.fromServerId("starred")

        assertEquals(AutoPlaySource.Starred, source)
    }
}
