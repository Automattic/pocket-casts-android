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

        assertEquals(AutoPlaySource.Predefined.None, source)
    }

    @Test
    fun `create Files from ID`() {
        val source = AutoPlaySource.fromId("files")

        assertEquals(AutoPlaySource.Predefined.Files, source)
    }

    @Test
    fun `create Downloads from ID`() {
        val source = AutoPlaySource.fromId("downloads")

        assertEquals(AutoPlaySource.Predefined.Downloads, source)
    }

    @Test
    fun `create Starred from ID`() {
        val source = AutoPlaySource.fromId("starred")

        assertEquals(AutoPlaySource.Predefined.Starred, source)
    }
}
