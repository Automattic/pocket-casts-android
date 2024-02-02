package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.NetworkCapabilities
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class PlaybackManagerNetworkWatcher @AssistedInject constructor(
    private val networkConnectionWatcher: NetworkConnectionWatcher,
    @Assisted private val onSwitchToMeteredConnection: suspend () -> Unit,
) {
    private var connectionIsMetered: Boolean? = null

    suspend fun observeConnection(): Nothing {
        networkConnectionWatcher.networkCapabilities.collect { networkCapabilties ->
            networkCapabilties?.let {
                onNetworkStateChanged(it)
            }
        }
    }

    suspend fun onNetworkStateChanged(networkCapabilities: NetworkCapabilities) {
        val newConnectionIsMetered = networkCapabilities.isMetered()
        val changedToMetered = connectionIsMetered == false && newConnectionIsMetered
        if (changedToMetered) {
            onSwitchToMeteredConnection()
        }
        connectionIsMetered = newConnectionIsMetered
    }

    @AssistedFactory
    interface Factory {
        fun create(onSwitchToMeteredConnection: suspend () -> Unit): PlaybackManagerNetworkWatcher
    }
}

fun NetworkCapabilities.isMetered() =
    !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
