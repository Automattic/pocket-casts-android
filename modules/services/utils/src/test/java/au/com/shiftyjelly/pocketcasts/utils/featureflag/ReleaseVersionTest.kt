package au.com.shiftyjelly.pocketcasts.utils.featureflag

import au.com.shiftyjelly.pocketcasts.featureflag.ReleaseVersion.Companion.comparedToEarlyPatronAccess
import junit.framework.TestCase.assertEquals
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
    fun `returns before when major version less than early access major version`() {
        val version = ReleaseVersion(major = 1, minor = 1)
        assertTrue(version.comparedToEarlyPatronAccess(ReleaseVersion(2, 1)) == EarlyAccessState.Before)
    }

    @Test
    fun `returns before when major version same abd minor version less than early access minor version`() {
        val version = ReleaseVersion(major = 1, minor = 1)
        assertTrue(version.comparedToEarlyPatronAccess(ReleaseVersion(1, 2)) == EarlyAccessState.Before)
    }

    @Test
    fun `returns during when major and minor versions match, and it's a release candidate`() {
        val version = ReleaseVersion(major = 2, minor = 3, releaseCandidate = 1)
        assertTrue(version.comparedToEarlyPatronAccess(ReleaseVersion(2, 3, 4)) == EarlyAccessState.During)
    }

    @Test
    fun `returns during when major and minor versions match, and it's not a release candidate`() {
        val version = ReleaseVersion(major = 2, minor = 3)
        assertTrue(version.comparedToEarlyPatronAccess(ReleaseVersion(2, 3, 4)) == EarlyAccessState.During)
    }

    @Test
    fun `returns during when major and minor versions match, even if current release is a patch`() {
        val version = ReleaseVersion(major = 1, minor = 1)
        assertTrue(version.comparedToEarlyPatronAccess(ReleaseVersion(1, 1, 1)) == EarlyAccessState.During)
    }

    @Test
    fun `returns after when major version more than early access major version`() {
        val version = ReleaseVersion(major = 2, minor = 1)
        assertTrue(version.comparedToEarlyPatronAccess(ReleaseVersion(1, 1)) == EarlyAccessState.After)
    }

    @Test
    fun `returns after when major version same abd minor version more than early access minor version`() {
        val version = ReleaseVersion(major = 1, minor = 2)
        assertTrue(version.comparedToEarlyPatronAccess(ReleaseVersion(1, 1)) == EarlyAccessState.After)
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
