package au.com.shiftyjelly.pocketcasts.utils.extensions

import junit.framework.TestCase.assertEquals
import org.junit.Test

class ListTest {
    @Test
    fun `do not pad end if length is smaller than list`() {
        val list = listOf(1, 2, 3)

        val paddedList = list.padEnd(1)

        assertEquals(list, paddedList)
    }

    @Test
    fun `do not pad end if length is equal to list`() {
        val list = listOf(1, 2, 3)

        val paddedList = list.padEnd(3)

        assertEquals(list, paddedList)
    }

    @Test
    fun `pad end cyclically`() {
        val list = listOf(1, 2, 3)

        val paddedList = list.padEnd(7)

        assertEquals(listOf(1, 2, 3, 1, 2, 3, 1), paddedList)
    }

    @Test
    fun `use custom pad function`() {
        val list = listOf(1, 2, 3)

        val paddedList = list.padEnd(5, padItem = { _, _ -> 0 })

        assertEquals(listOf(1, 2, 3, 0, 0), paddedList)
    }
}
