package au.com.shiftyjelly.pocketcasts.shared

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.utils.Network as PCNetworkUtils

class NetworkConnectionWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackManager: PlaybackManager,
) {

    // Default to true so we do not detect a change to being metered when the class is initialized
    private var connectionIsMetered: Boolean = true
        set(value) {
            val changedToMetered = !field && value
            if (changedToMetered) {
                playbackManager.onMeteredConnection()
            }
            field = value
        }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            connectionIsMetered = networkCapabilities.isMetered()
            super.onCapabilitiesChanged(network, networkCapabilities)
        }
    }

    fun startWatching() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        PCNetworkUtils.getConnectivityManager(context)
            .registerNetworkCallback(networkRequest, networkCallback)
    }

    fun stopWatching() {
        PCNetworkUtils.getConnectivityManager(context)
            .unregisterNetworkCallback(networkCallback)
    }
}

private fun NetworkCapabilities.isMetered() =
    !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
