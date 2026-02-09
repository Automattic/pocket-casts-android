package au.com.shiftyjelly.pocketcasts.wear.networking

import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.google.android.horologist.networks.data.NetworkInfo
import com.google.android.horologist.networks.data.NetworkStatus
import com.google.android.horologist.networks.data.NetworkType
import com.google.android.horologist.networks.data.Networks
import com.google.android.horologist.networks.data.Status
import com.google.android.horologist.networks.status.NetworkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityStateManagerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var networkRepository: NetworkRepository

    private lateinit var connectivityStateManager: ConnectivityStateManager
    private lateinit var testScope: TestScope

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
        testScope = TestScope(coroutineRule.testDispatcher)
        connectivityStateManager = ConnectivityStateManager(networkRepository, testScope)
    }

    @Test
    fun `isConnected is false when no networks available`() = runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = emptyList(),
        )
        advanceUntilIdle()

        assertEquals(false, connectivityStateManager.isConnected.value)
    }

    @Test
    fun `isConnected is false with only Bluetooth`() = runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.BT)),
        )
        advanceUntilIdle()

        assertEquals(false, connectivityStateManager.isConnected.value)
    }

    @Test
    fun `isConnected is true with WiFi`() = runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.Wifi)),
        )
        advanceUntilIdle()

        assertEquals(true, connectivityStateManager.isConnected.value)
    }

    @Test
    fun `isConnected is true with Cellular`() = runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.Cell)),
        )
        advanceUntilIdle()

        assertEquals(true, connectivityStateManager.isConnected.value)
    }

    @Test
    fun `isConnected is true with WiFi and Bluetooth`() = runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(
                buildNetworkStatus(NetworkType.Wifi),
                buildNetworkStatus(NetworkType.BT),
            ),
        )
        advanceUntilIdle()

        assertEquals(true, connectivityStateManager.isConnected.value)
    }

    @Test
    fun `isConnected is true with Cellular and Bluetooth`() = runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(
                buildNetworkStatus(NetworkType.Cell),
                buildNetworkStatus(NetworkType.BT),
            ),
        )
        advanceUntilIdle()

        assertEquals(true, connectivityStateManager.isConnected.value)
    }

    @Test
    fun `isConnected changes from false to true when WiFi connects`() = runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = emptyList(),
        )
        advanceUntilIdle()
        assertEquals(false, connectivityStateManager.isConnected.value)

        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.Wifi)),
        )
        advanceUntilIdle()

        assertEquals(true, connectivityStateManager.isConnected.value)
    }

    @Test
    fun `isConnected changes from true to false when WiFi disconnects`() = runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.Wifi)),
        )
        advanceUntilIdle()
        assertEquals(true, connectivityStateManager.isConnected.value)

        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = emptyList(),
        )
        advanceUntilIdle()

        assertEquals(false, connectivityStateManager.isConnected.value)
    }

    @Test
    fun `isConnected remains true when switching from WiFi to Cellular`() = runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.Wifi)),
        )
        advanceUntilIdle()
        assertEquals(true, connectivityStateManager.isConnected.value)

        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.Cell)),
        )
        advanceUntilIdle()

        assertEquals(true, connectivityStateManager.isConnected.value)
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
