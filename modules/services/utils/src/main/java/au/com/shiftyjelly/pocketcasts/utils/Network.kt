package au.com.shiftyjelly.pocketcasts.utils

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build

object Network {

    @Suppress("DEPRECATION")
    fun isConnected(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        val networkInfo = getConnectivityManager(context).activeNetworkInfo ?: return false
        return networkInfo.isConnected
    }

    @Suppress("DEPRECATION")
    fun getConnectedNetworkTypeName(context: Context): String {
        val networkInfo = getConnectivityManager(context).activeNetworkInfo
        return if (networkInfo == null) "unknown" else networkInfo.typeName
    }

    fun getRestrictBackgroundStatusString(context: Context): String {
        if (Build.VERSION.SDK_INT < 24) {
            return "API not available"
        }
        return when (getConnectivityManager(context).restrictBackgroundStatus) {
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED -> "Device is restricting metered network activity while application is running on background."
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED -> "Device is restricting metered network activity while application is running on background, but application is allowed to bypass it."
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED -> "Device is not restricting metered network activity while application is running on background."
            else -> "Unknown"
        }
    }

    fun isActiveNetworkMetered(context: Context): Boolean {
        return getConnectivityManager(context).isActiveNetworkMetered
    }

    @Suppress("DEPRECATION")
    fun isWifiConnection(context: Context): Boolean {
        val connectivityManager = getConnectivityManager(context)
        val networks = connectivityManager.allNetworks

        for (index in networks.indices) {
            val network = networks[index]
            val networkInfo = connectivityManager.getNetworkInfo(network)
            if (networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected) {
                return true
            }
        }
        return false
    }
    fun getConnectivityManager(context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Suppress("DEPRECATION")
    fun isUnmeteredConnection(context: Context): Boolean {
        val connectivityManager = getConnectivityManager(context)
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected && activeNetworkInfo.isAvailable && !connectivityManager.isActiveNetworkMetered
    }
}
