package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.StateFlow

interface NetworkConnectionWatcher {
    val networkCapabilities: StateFlow<NetworkCapabilities?>
}
