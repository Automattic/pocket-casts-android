package au.com.shiftyjelly.pocketcasts.wear.networking

import au.com.shiftyjelly.pocketcasts.wear.WearLogging
import com.google.android.horologist.networks.data.NetworkType
import com.google.android.horologist.networks.status.NetworkRepository
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Monitors the connection status between the watch and the paired phone.
 *
 * Uses Bluetooth connectivity as an indicator of phone availability since
 * watch-phone communication typically happens over Bluetooth.
 */
@Singleton
class PhoneConnectionMonitor @Inject constructor(
    private val networkRepository: NetworkRepository,
) {
    fun isPhoneConnected(): Boolean {
        return try {
            val networks = networkRepository.networkStatus.value
            networks.networks.any { networkStatus ->
                networkStatus.networkInfo.type == NetworkType.BT
            }
        } catch (e: Exception) {
            Timber.d("${WearLogging.PREFIX} PhoneConnectionMonitor.isPhoneConnected failed: ${e.message}")
            false
        }
    }
}
