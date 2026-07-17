package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.os.Process
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class PackageValidatorTest {

    private lateinit var packageManager: PackageManager
    private lateinit var validator: PackageValidator

    @Before
    fun setUp() {
        val emptyAllowListParser = mock<XmlResourceParser>()
        whenever(emptyAllowListParser.next()).thenReturn(XmlResourceParser.END_DOCUMENT)

        val resources = mock<Resources>()
        whenever(resources.getXml(any())).thenReturn(emptyAllowListParser)

        val platformPackageInfo = PackageInfo().apply {
            packageName = "android"
            signatures = arrayOf(Signature(byteArrayOf(1, 2, 3)))
        }
        packageManager = mock()
        whenever(packageManager.getPackageInfo(eq("android"), any<Int>())).thenReturn(platformPackageInfo)

        val context = mock<Context>()
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.resources).thenReturn(resources)
        whenever(context.packageManager).thenReturn(packageManager)

        validator = PackageValidator(context, xmlResId = 0)
    }

    @Test
    fun `treats caller as unknown when its package cannot be resolved`() {
        // Missing packages and packages hidden by Android 11+ visibility rules throw instead of returning null.
        val callingPackage = "com.google.android.carassistant"
        whenever(packageManager.getPackageInfo(eq(callingPackage), any<Int>()))
            .thenThrow(PackageManager.NameNotFoundException(callingPackage))

        assertFalse(validator.isKnownCaller(callingPackage, callingUid = 12345))
    }

    @Test
    fun `treats own app as known caller`() {
        val callingPackage = "au.com.shiftyjelly.pocketcasts"
        val myUid = Process.myUid()
        val ownPackageInfo = PackageInfo().apply {
            packageName = callingPackage
            signatures = arrayOf(Signature(byteArrayOf(4, 5, 6)))
            applicationInfo = ApplicationInfo().apply {
                uid = myUid
                nonLocalizedLabel = "Pocket Casts"
            }
        }
        whenever(packageManager.getPackageInfo(eq(callingPackage), any<Int>())).thenReturn(ownPackageInfo)

        assertTrue(validator.isKnownCaller(callingPackage, callingUid = myUid))
    }
}
