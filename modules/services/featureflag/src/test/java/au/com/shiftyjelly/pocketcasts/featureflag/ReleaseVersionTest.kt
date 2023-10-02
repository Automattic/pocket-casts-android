package au.com.shiftyjelly.pocketcasts.featureflag

import au.com.shiftyjelly.pocketcasts.featureflag.ReleaseVersion.Companion.matchesCurrentReleaseForEarlyPatronAccess
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ReleaseVersionTest {

    /*
     * fromString
     */

    @Test
    fun `fromString handles simple release`() {
        assertEquals(
            ReleaseVersion(1, 2),
            ReleaseVersion.fromString("1.2")
        )
    }

    @Test
    fun `fromString handles patch release`() {
        assertEquals(
            ReleaseVersion(major = 1, minor = 2, patch = 3),
            ReleaseVersion.fromString("1.2.3")
        )
    }

    @Test
    fun `fromString handles rc release`() {
        assertEquals(
            ReleaseVersion(major = 1, minor = 2, releaseCandidate = 3),
            ReleaseVersion.fromString("1.2-rc-3")
        )
    }

    @Test
    fun `fromString handles rc release for patch`() {
        assertEquals(
            ReleaseVersion(major = 1, minor = 2, patch = 3, releaseCandidate = 4),
            ReleaseVersion.fromString("1.2.3-rc-4")
        )
    }

    @Test
    fun `fromString handles multiple digits`() {
        assertEquals(
            ReleaseVersion(major = 1111, minor = 222222, patch = 333333333, releaseCandidate = 444444),
            ReleaseVersion.fromString("1111.222222.333333333-rc-444444")
        )
    }

    /*
     * Comparing release versions
     */

    @Test
    fun `compares major before minor`() {
        assertFirstLessThanSecond("1.2", "2.1")
    }

    @Test
    fun `compares minor if major is equal`() {
        assertFirstLessThanSecond("1.1", "1.2")
    }

    @Test
    fun `compares patch if major and minor are equal`() {
        assertFirstLessThanSecond("1.1.1", "1.1.2")
    }

    @Test
    fun `considers absence of patch as lower than its presence`() {
        assertFirstLessThanSecond("1.2", "1.2.3")
    }

    @Test
    fun `compares releaseCandidate if major, minor, and patch are equal`() {
        assertFirstLessThanSecond("1.2.3-rc-4", "1.2.3-rc-5")
    }

    @Test
    fun `considers release with same major and minor higher than its RC counterpart`() {
        assertFirstLessThanSecond("1.2-rc-3", "1.2")
    }

    @Test
    fun `considers release with same major, minor, and patch higher than its RC counterpart`() {
        assertFirstLessThanSecond("1.2.3-rc-4", "1.2.3")
    }

    /*
     * matchesCurrentReleaseForEarlyPatronAccess
     */

    @Test
    fun `returns false when called on null`() {
        assertFalse(null.matchesCurrentReleaseForEarlyPatronAccess(ReleaseVersion(1, 1)))
    }

    @Test
    fun `returns false when major version does not match`() {
        val version = ReleaseVersion(major = 1, minor = 1)
        assertFalse(version.matchesCurrentReleaseForEarlyPatronAccess(ReleaseVersion(2, 1)))
    }

    @Test
    fun `returns false when minor version does not match`() {
        val version = ReleaseVersion(major = 1, minor = 1)
        assertFalse(version.matchesCurrentReleaseForEarlyPatronAccess(ReleaseVersion(1, 2)))
    }

    @Test
    fun `returns false for release candidates`() {
        val version = ReleaseVersion(major = 2, minor = 3, releaseCandidate = 1)
        assertFalse(version.matchesCurrentReleaseForEarlyPatronAccess(ReleaseVersion(2, 3, 4)))
    }

    @Test
    fun `returns true when major and minor versions match, and it's not a release candidate`() {
        val version = ReleaseVersion(major = 2, minor = 3)
        assertTrue(version.matchesCurrentReleaseForEarlyPatronAccess(ReleaseVersion(2, 3, 4)))
    }

    @Test
    fun `returns true when major and minor versions match, even if current release is a patch`() {
        val version = ReleaseVersion(major = 1, minor = 1)
        assertTrue(
            version.matchesCurrentReleaseForEarlyPatronAccess(
                currentReleaseVersion = ReleaseVersion(1, 1, 1)
            )
        )
    }

    private fun assertFirstLessThanSecond(first: String, second: String) {
        val firstVersion = ReleaseVersion.fromString(first)
        val secondVersion = ReleaseVersion.fromString(second)
        if (firstVersion == null || secondVersion == null) {
            Assert.fail()
        } else {
            assertTrue(firstVersion < secondVersion)
        }
    }
}
