package au.com.shiftyjelly.pocketcasts.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

fun Context.getVersionCode() = getPackageInfo(packageName)?.let {
    PackageInfoCompat.getLongVersionCode(it).toInt()
} ?: 0

fun Context.getPackageInfo(packageName: String) = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        packageManager.getPackageInfo(packageName, 0)
    }
} catch (e: PackageManager.NameNotFoundException) {
    null
}
