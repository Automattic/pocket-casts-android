package au.com.shiftyjelly.pocketcasts

import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.ui.helper.AppIcon
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIconAliasTest {

    @Test
    fun everyAppIconTypeIsDeclaredAsAnActivityAlias() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            packageManager.getPackageInfo(context.packageName, flags)
        }
        val declaredActivities = packageInfo.activities.orEmpty().mapTo(mutableSetOf()) { it.name }

        AppIcon.AppIconType.entries.forEach { iconType ->
            val aliasName = "au.com.shiftyjelly.pocketcasts${iconType.aliasName}"
            assertTrue("Missing activity alias $aliasName for $iconType", aliasName in declaredActivities)
        }
    }
}
