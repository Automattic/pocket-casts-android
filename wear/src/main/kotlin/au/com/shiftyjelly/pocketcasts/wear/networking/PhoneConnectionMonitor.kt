package au.com.shiftyjelly.pocketcasts.wear.networking

import android.content.Context
import androidx.core.net.toUri
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Monitors whether a paired phone running Pocket Casts is reachable.
 *
 * Uses the Wearable [CapabilityClient] with [CapabilityClient.FILTER_REACHABLE] so the
 * phone is detected over any transport (Bluetooth, Wi-Fi or cloud), rather than assuming
 * a Bluetooth connection.
 */
@Singleton
class PhoneConnectionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) {
    private val capabilityInfo = MutableStateFlow<CapabilityInfo?>(null)

    // null means the capability has not been queried yet, so callers can distinguish
    // "not yet known" from "queried and no phone reachable" and avoid a cold-start flash.
    val isPhoneConnected: StateFlow<Boolean?> = capabilityInfo
        .map { info -> info?.nodes?.isNotEmpty() }
        .stateIn(coroutineScope, SharingStarted.Lazily, null)

    private val listener = CapabilityClient.OnCapabilityChangedListener { info ->
        capabilityInfo.value = info
    }

    init {
        coroutineScope.launch {
            try {
                capabilityInfo.value = Wearable.getCapabilityClient(context)
                    .getCapability(CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                    .await()
            } catch (e: Exception) {
                Timber.e(e, "Failed to query phone capability")
            }
        }

        Wearable.getCapabilityClient(context).addListener(
            listener,
            "wear://*/$CAPABILITY_NAME".toUri(),
            CapabilityClient.FILTER_REACHABLE,
        )
    }

    companion object {
        private const val CAPABILITY_NAME = "pocket_casts_wear_listener"
    }
}
