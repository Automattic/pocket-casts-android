package au.com.shiftyjelly.pocketcasts.shared

import android.net.NetworkCapabilities
import au.com.shiftyjelly.pocketcasts.crashlogging.ConnectionStatusProvider
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import javax.inject.Inject

class NetworkConnectionStatusProvider @Inject constructor(
    private val networkConnectionWatcher: NetworkConnectionWatcher,
) : ConnectionStatusProvider {
    override fun isConnected(): Boolean {
        return networkConnectionWatcher.networkCapabilities
            .value?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }
}
