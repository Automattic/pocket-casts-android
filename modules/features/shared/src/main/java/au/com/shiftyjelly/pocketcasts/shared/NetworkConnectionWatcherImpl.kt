package au.com.shiftyjelly.pocketcasts.shared

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.VisibleForTesting
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import au.com.shiftyjelly.pocketcasts.utils.Network as PCNetworkUtils

@Singleton
class NetworkConnectionWatcherImpl @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) : NetworkConnectionWatcher {

    private val _networkCapabilities = MutableStateFlow<NetworkCapabilities?>(null)
    override val networkCapabilities: StateFlow<NetworkCapabilities?> = _networkCapabilities

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            this@NetworkConnectionWatcherImpl.onCapabilitiesChanged(networkCapabilities)
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
    internal fun onCapabilitiesChanged(networkCapabilities: NetworkCapabilities) {
        _networkCapabilities.value = networkCapabilities
    }
}
