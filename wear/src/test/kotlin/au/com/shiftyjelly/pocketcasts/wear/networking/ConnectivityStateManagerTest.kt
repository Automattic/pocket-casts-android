package au.com.shiftyjelly.pocketcasts.wear.networking

import app.cash.turbine.test
import com.google.android.horologist.networks.data.NetworkInfo
import com.google.android.horologist.networks.data.NetworkStatus
import com.google.android.horologist.networks.data.NetworkType
import com.google.android.horologist.networks.data.Networks
import com.google.android.horologist.networks.data.Status
import com.google.android.horologist.networks.status.NetworkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityStateManagerTest {

    @Mock
    private lateinit var networkRepository: NetworkRepository

    private val testScope = TestScope()

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
    }

    private fun createConnectivityStateManager() = ConnectivityStateManager(networkRepository, testScope.backgroundScope)

    @Test
    fun `isConnected is false when no networks available`() = testScope.runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = emptyList(),
        )
        val connectivityStateManager = createConnectivityStateManager()

        connectivityStateManager.isConnected.test {
            assertEquals(false, awaitItem())
            cancel()
        }
    }

    @Test
    fun `isConnected is false with only Bluetooth`() = testScope.runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.BT)),
        )
        val connectivityStateManager = createConnectivityStateManager()

        connectivityStateManager.isConnected.test {
            assertEquals(false, awaitItem())
            cancel()
        }
    }

    @Test
    fun `isConnected is true with WiFi`() = testScope.runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.Wifi)),
        )
        val connectivityStateManager = createConnectivityStateManager()
        testScheduler.runCurrent()

        connectivityStateManager.isConnected.test {
            assertEquals(true, awaitItem())
            cancel()
        }
    }

    @Test
    fun `isConnected is true with Cellular`() = testScope.runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.Cell)),
        )
        val connectivityStateManager = createConnectivityStateManager()
        testScheduler.runCurrent()

        connectivityStateManager.isConnected.test {
            assertEquals(true, awaitItem())
            cancel()
        }
    }

    @Test
    fun `isConnected is true with WiFi and Bluetooth`() = testScope.runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(
                buildNetworkStatus(NetworkType.Wifi),
                buildNetworkStatus(NetworkType.BT),
            ),
        )
        val connectivityStateManager = createConnectivityStateManager()
        testScheduler.runCurrent()

        connectivityStateManager.isConnected.test {
            assertEquals(true, awaitItem())
            cancel()
        }
    }

    @Test
    fun `isConnected is true with Cellular and Bluetooth`() = testScope.runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(
                buildNetworkStatus(NetworkType.Cell),
                buildNetworkStatus(NetworkType.BT),
            ),
        )
        val connectivityStateManager = createConnectivityStateManager()
        testScheduler.runCurrent()

        connectivityStateManager.isConnected.test {
            assertEquals(true, awaitItem())
            cancel()
        }
    }

    @Test
    fun `isConnected changes from false to true when WiFi connects`() = testScope.runTest {
        val connectivityStateManager = createConnectivityStateManager()

        connectivityStateManager.isConnected.test {
            assertEquals(false, awaitItem())

            networkStatusFlow.value = Networks(
                activeNetwork = null,
                networks = listOf(buildNetworkStatus(NetworkType.Wifi)),
            )
            assertEquals(true, awaitItem())
            cancel()
        }
    }

    @Test
    fun `isConnected changes from true to false when WiFi disconnects`() = testScope.runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.Wifi)),
        )
        val connectivityStateManager = createConnectivityStateManager()
        testScheduler.runCurrent()

        connectivityStateManager.isConnected.test {
            assertEquals(true, awaitItem())

            networkStatusFlow.value = Networks(
                activeNetwork = null,
                networks = emptyList(),
            )
            assertEquals(false, awaitItem())
            cancel()
        }
    }

    @Test
    fun `isConnected remains true when switching from WiFi to Cellular`() = testScope.runTest {
        networkStatusFlow.value = Networks(
            activeNetwork = null,
            networks = listOf(buildNetworkStatus(NetworkType.Wifi)),
        )
        val connectivityStateManager = createConnectivityStateManager()
        testScheduler.runCurrent()

        connectivityStateManager.isConnected.test {
            assertEquals(true, awaitItem())

            networkStatusFlow.value = Networks(
                activeNetwork = null,
                networks = listOf(buildNetworkStatus(NetworkType.Cell)),
            )
            expectNoEvents()
            cancel()
        }
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
