package au.com.shiftyjelly.pocketcasts.wear.networking

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.networks.data.NetworkType
import com.google.android.horologist.networks.data.Networks
import com.google.android.horologist.networks.status.NetworkRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalHorologistApi::class)
@Singleton
class ConnectivityLogger @Inject constructor(
    private val networkRepository: NetworkRepository,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) {
    private var monitoringJob: Job? = null
    private var previousNetworks: Set<NetworkType> = emptySet()

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) {
            return
        }

        monitoringJob = coroutineScope.launch {
            try {
                networkRepository.networkStatus.collect { networks ->
                    handleNetworkUpdate(networks)
                }
            } catch (e: Exception) {
                LogBuffer.e(
                    LogBuffer.TAG_CONNECTIVITY,
                    e,
                    "Error monitoring network status",
                )
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun handleNetworkUpdate(networks: Networks) {
        val currentNetworks = networks.networks.map { it.networkInfo.type }.toSet()

        val connected = currentNetworks - previousNetworks
        val disconnected = previousNetworks - currentNetworks

        connected.forEach { networkType ->
            logConnectivityChange(networkType, true)
        }

        disconnected.forEach { networkType ->
            logConnectivityChange(networkType, false)
        }

        previousNetworks = currentNetworks
    }

    private fun logConnectivityChange(networkType: NetworkType, isConnected: Boolean) {
        val networkName = when (networkType) {
            NetworkType.BT -> "Bluetooth (Phone)"
            NetworkType.Wifi -> "WiFi"
            NetworkType.Cell -> "Cellular"
            else -> networkType.toString()
        }
        val status = if (isConnected) "CONNECTED" else "DISCONNECTED"
        LogBuffer.i(
            LogBuffer.TAG_CONNECTIVITY,
            "$networkName $status",
        )
    }
}
