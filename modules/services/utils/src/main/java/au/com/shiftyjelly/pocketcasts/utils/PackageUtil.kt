package au.com.shiftyjelly.pocketcasts.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import javax.inject.Inject

class PackageUtil @Inject constructor() {
    fun getVersionCode(context: Context): Int {
        val packageInfo = getPackageInfo(context)
        return packageInfo?.let {
            PackageInfoCompat.getLongVersionCode(it).toInt()
        } ?: 0
    }

    private fun getPackageInfo(context: Context): PackageInfo? {
        return try {
            val manager = context.packageManager
            manager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
