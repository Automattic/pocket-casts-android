package au.com.shiftyjelly.pocketcasts.wear.networking

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.networks.data.NetworkType
import com.google.android.horologist.networks.status.NetworkRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalHorologistApi::class)
@Singleton
class ConnectivityStateManager @Inject constructor(
    networkRepository: NetworkRepository,
    @ApplicationScope coroutineScope: CoroutineScope,
) {
    val isConnected: StateFlow<Boolean> = networkRepository.networkStatus
        .map { networks ->
            val networkTypes = networks.networks.map { it.networkInfo.type }.toSet()
            networkTypes.contains(NetworkType.Wifi) || networkTypes.contains(NetworkType.Cell)
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )
}
