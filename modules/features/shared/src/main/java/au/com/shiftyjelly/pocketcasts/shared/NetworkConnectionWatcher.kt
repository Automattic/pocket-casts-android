package au.com.shiftyjelly.pocketcasts.shared

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.VisibleForTesting
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.utils.Network as PCNetworkUtils

class NetworkConnectionWatcher @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
    private val playbackManager: PlaybackManager,
) {

    // Default to true so we do not detect a change to being metered when the class is initialized
    private var connectionIsMetered: Boolean = true

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            applicationScope.launch {
                this@NetworkConnectionWatcher.onCapabilitiesChanged(networkCapabilities)
            }
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

    @VisibleForTesting
    internal suspend fun onCapabilitiesChanged(networkCapabilities: NetworkCapabilities) {
        val newConnectionIsMetered = networkCapabilities.isMetered()
        val changedToMetered = !connectionIsMetered && newConnectionIsMetered
        if (changedToMetered) {
            playbackManager.onSwitchedToMeteredConnection()
        }
        connectionIsMetered = newConnectionIsMetered
    }
}

private fun NetworkCapabilities.isMetered() =
    !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
