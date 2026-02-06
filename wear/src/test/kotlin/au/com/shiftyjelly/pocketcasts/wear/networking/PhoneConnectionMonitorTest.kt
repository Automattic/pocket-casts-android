package au.com.shiftyjelly.pocketcasts.wear.networking

import com.google.android.horologist.networks.data.NetworkInfo
import com.google.android.horologist.networks.data.NetworkStatus
import com.google.android.horologist.networks.data.NetworkType
import com.google.android.horologist.networks.data.Networks
import com.google.android.horologist.networks.data.Status
import com.google.android.horologist.networks.status.NetworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class PhoneConnectionMonitorTest {
    @Mock
    private lateinit var networkRepository: NetworkRepository

    private lateinit var phoneConnectionMonitor: PhoneConnectionMonitor

    private val networkStatusFlow = MutableStateFlow(
        Networks(
            activeNetwork = null,
            networks = emptyList(),
        ),
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(networkRepository.networkStatus).thenReturn(networkStatusFlow)
        phoneConnectionMonitor = PhoneConnectionMonitor(networkRepository)
    }

    @Test
    fun `isPhoneConnected returns true when Bluetooth network is available`() = runTest {
        // Given - Bluetooth network is available
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.BT)),
        )

        // When
        val result = phoneConnectionMonitor.isPhoneConnected()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isPhoneConnected returns false when no Bluetooth network available`() = runTest {
        // Given - Only WiFi network available, no Bluetooth
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.Wifi)),
        )

        // When
        val result = phoneConnectionMonitor.isPhoneConnected()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isPhoneConnected returns false when no networks available`() = runTest {
        // Given - Empty network list
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = emptyList(),
        )

        // When
        val result = phoneConnectionMonitor.isPhoneConnected()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isPhoneConnected returns true when Bluetooth is one of multiple networks`() = runTest {
        // Given - Multiple networks including Bluetooth
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(
                buildNetworkStatus(NetworkType.Wifi),
                buildNetworkStatus(NetworkType.BT),
            ),
        )

        // When
        val result = phoneConnectionMonitor.isPhoneConnected()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isPhoneConnected returns false on exception`() = runTest {
        // Given - NetworkRepository throws exception
        whenever(networkRepository.networkStatus).thenThrow(RuntimeException("Network error"))
        val monitorWithException = PhoneConnectionMonitor(networkRepository)

        // When
        val result = monitorWithException.isPhoneConnected()

        // Then - Should fail safely and return false
        assertFalse(result)
    }

    private fun buildNetworkStatus(networkType: NetworkType) = NetworkStatus(
        id = "",
        status = Status.Available,
        networkInfo = buildNetworkInfo(networkType),
        addresses = emptyList(),
        capabilities = null,
        linkProperties = null,
        bindSocket = {},
    )

    private fun buildNetworkInfo(networkType: NetworkType): NetworkInfo = when (networkType) {
        NetworkType.Wifi -> NetworkInfo.Wifi(name = "")
        NetworkType.Cell -> NetworkInfo.Cellular(name = "")
        NetworkType.BT -> NetworkInfo.Bluetooth(name = "")
        else -> NetworkInfo.Unknown()
    }
}
