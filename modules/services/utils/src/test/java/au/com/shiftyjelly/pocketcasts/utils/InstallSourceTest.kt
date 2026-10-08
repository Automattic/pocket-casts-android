package au.com.shiftyjelly.pocketcasts.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class InstallSourceTest {
    @Test
    fun `maps Google Play installer`() {
        assertEquals(InstallSource.GooglePlay, InstallSource.fromInstallerPackageName("com.android.vending"))
    }

    @Test
    fun `maps Amazon Appstore installer`() {
        assertEquals(InstallSource.AmazonAppstore, InstallSource.fromInstallerPackageName("com.amazon.venezia"))
    }

    @Test
    fun `maps other installers to other`() {
        assertEquals(InstallSource.Other, InstallSource.fromInstallerPackageName("com.android.shell"))
    }

    @Test
    fun `maps a missing installer to unknown`() {
        assertEquals(InstallSource.Unknown, InstallSource.fromInstallerPackageName(null))
    }
}
