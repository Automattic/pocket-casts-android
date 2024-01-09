package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.NetworkCapabilities
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlaybackManagerNetworkWatcher @Inject constructor(
    private val networkConnectionWatcher: NetworkConnectionWatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    private var connectionIsMetered: Boolean? = null
    private lateinit var onSwitchToMeteredConnection: suspend () -> Unit

    init {
        applicationScope.launch {
            networkConnectionWatcher.networkCapabilities.collect { networkCapabilties ->
                networkCapabilties?.let {
                    onNetworkStateChanged(it)
                }
            }
        }
    }

    fun initialize(onSwitchToMeteredConnection: suspend () -> Unit) {
        this.onSwitchToMeteredConnection = onSwitchToMeteredConnection
    }

    private fun onNetworkStateChanged(networkCapabilities: NetworkCapabilities) {
        val newConnectionIsMetered = networkCapabilities.isMetered()
        val changedToMetered = connectionIsMetered == false && newConnectionIsMetered
        applicationScope.launch {
            if (changedToMetered) {
                onSwitchToMeteredConnection()
            }
            connectionIsMetered = newConnectionIsMetered
        }
    }
}

fun NetworkCapabilities.isMetered() =
    !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
