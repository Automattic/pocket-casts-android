package au.com.shiftyjelly.pocketcasts.utils.featureflag

import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import junit.framework.TestCase.assertEquals
import org.junit.Test

class DoubleTest {

    @Test
    fun `roundedSpeed rounds correctly`() {
        assertEquals(1.2, 1.19.roundedSpeed())
        assertEquals(2.8, 2.75.roundedSpeed())
        assertEquals(1.2, 1.2000000000000002.roundedSpeed())
    }

    @Test
    fun `roundedSpeed respects lower boundary`() {
        assertEquals(0.5, 0.1.roundedSpeed())
    }

    @Test
    fun `roundedSpeed respects upper boundary`() {
        assertEquals(3.0, 5.0.roundedSpeed())
    }

    @Test
    fun `roundedSpeed works within boundaries`() {
        assertEquals(2.0, 2.0.roundedSpeed())
    }

    @Test
    fun `roundedSpeed adjusts below lower boundary`() {
        assertEquals(0.5, 0.4.roundedSpeed())
    }

    @Test
    fun `roundedSpeed adjusts above upper boundary`() {
        assertEquals(3.0, 3.5.roundedSpeed())
    }
}
