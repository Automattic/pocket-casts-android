package au.com.shiftyjelly.pocketcasts.utils.featureflag

import junit.framework.TestCase.assertFalse
import org.junit.Before
import org.junit.Test

class BookmarkFeatureControlTest {

    private lateinit var bookmarkFeatureControl: BookmarkFeatureControl

    @Before
    fun setup() {
        bookmarkFeatureControl = BookmarkFeatureControl()
    }

    @Test
    fun shouldBeAvailableForPatronSubscription() {
        assert(bookmarkFeatureControl.isAvailable(UserTier.Patron))
    }

    @Test
    fun shouldBeAvailableForPlusSubscription() {
        assert(bookmarkFeatureControl.isAvailable(UserTier.Plus))
    }

    @Test
    fun shouldNotBeAvailable() {
        assertFalse(bookmarkFeatureControl.isAvailable(UserTier.Free))
    }
}
