package au.com.shiftyjelly.pocketcasts.component

import org.junit.Assert.assertSame
import org.junit.Test

class TvCategoryStyleTest {

    @Test
    fun `gradient cycles through the palette`() {
        assertSame(TvCategoryStyle.gradient(0), TvCategoryStyle.gradient(3))
        assertSame(TvCategoryStyle.gradient(1), TvCategoryStyle.gradient(4))
        assertSame(TvCategoryStyle.gradient(2), TvCategoryStyle.gradient(5))
    }

    @Test
    fun `gradient wraps a negative index`() {
        assertSame(TvCategoryStyle.gradient(2), TvCategoryStyle.gradient(-1))
        assertSame(TvCategoryStyle.gradient(0), TvCategoryStyle.gradient(-3))
    }
}
