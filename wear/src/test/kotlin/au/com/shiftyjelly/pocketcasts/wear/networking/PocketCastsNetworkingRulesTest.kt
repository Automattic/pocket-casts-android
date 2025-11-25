package au.com.shiftyjelly.pocketcasts.wear.networking

import com.google.android.horologist.networks.data.NetworkInfo
import com.google.android.horologist.networks.data.NetworkStatus
import com.google.android.horologist.networks.data.NetworkType
import com.google.android.horologist.networks.data.NetworkType.BT
import com.google.android.horologist.networks.data.NetworkType.Cell
import com.google.android.horologist.networks.data.NetworkType.Wifi
import com.google.android.horologist.networks.data.Networks
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.data.Status
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PocketCastsNetworkingRulesTest {

    @Test
    fun `prefer returns match`() {
        assertPrefers(
            input = listOf(Cell, Wifi, BT),
            prefer = listOf(Cell),
            expectedOutput = Cell,
        )

        assertPrefers(
            input = listOf(Cell, Wifi, BT),
            prefer = listOf(Wifi),
            expectedOutput = Wifi,
        )

        assertPrefers(
            input = listOf(Cell, Wifi, BT),
            prefer = listOf(BT),
            expectedOutput = BT,
        )
    }

    @Test
    fun `prefer returns first if no match`() {
        assertPrefers(
            input = listOf(Wifi, BT),
            prefer = listOf(Cell),
            expectedOutput = Wifi,
        )

        assertPrefers(
            input = listOf(BT, Cell),
            prefer = listOf(Wifi),
            expectedOutput = BT,
        )

        assertPrefers(
            input = listOf(Cell, Wifi),
            prefer = listOf(BT),
            expectedOutput = Cell,
        )
    }

    @Test
    fun `prefer returns second if no match on first`() {
        assertPrefers(
            input = listOf(Wifi, BT),
            prefer = listOf(Cell, BT),
            expectedOutput = BT,
        )

        assertPrefers(
            input = listOf(BT, Cell),
            prefer = listOf(Wifi, Cell),
            expectedOutput = Cell,
        )

        assertPrefers(
            input = listOf(Cell, Wifi),
            prefer = listOf(BT, Cell),
            expectedOutput = Cell,
        )
    }

    @Test
    fun `downloads prefer wifi`() {
        assertPreferredNetworkForRequestType(
            requestType = RequestType.MediaRequest.DownloadRequest,
            availableNetworkTypes = listOf(BT, Cell, Wifi),
            expectedNetworkType = Wifi,
        )
    }

    @Test
    fun `downloads prefer cell if no wifi`() {
        assertPreferredNetworkForRequestType(
            requestType = RequestType.MediaRequest.DownloadRequest,
            availableNetworkTypes = listOf(BT, Cell),
            expectedNetworkType = Cell,
        )
    }

    @Test
    fun `downloads will use bluetooth if only option`() {
        assertPreferredNetworkForRequestType(
            requestType = RequestType.MediaRequest.DownloadRequest,
            availableNetworkTypes = listOf(BT),
            expectedNetworkType = BT,
        )
    }

    @Test
    fun `streaming will use bluetooth if available`() {
        assertPreferredNetworkForRequestType(
            requestType = RequestType.MediaRequest.StreamRequest,
            availableNetworkTypes = listOf(Wifi, Cell, BT),
            expectedNetworkType = BT,
        )
    }

    @Test
    fun `streaming will prefer wifi over cell`() {
        assertPreferredNetworkForRequestType(
            requestType = RequestType.MediaRequest.StreamRequest,
            availableNetworkTypes = listOf(Cell, Wifi),
            expectedNetworkType = Wifi,
        )
    }

    @Test
    fun `streaming will use cell if only option`() {
        assertPreferredNetworkForRequestType(
            requestType = RequestType.MediaRequest.StreamRequest,
            availableNetworkTypes = listOf(Cell),
            expectedNetworkType = Cell,
        )
    }

    private fun assertPreferredNetworkForRequestType(
        requestType: RequestType,
        availableNetworkTypes: List<NetworkType>,
        expectedNetworkType: NetworkType,
    ) {
        val networks = buildNetworksWithTypes(availableNetworkTypes)
        val resultNetworkStatus = PocketCastsNetworkingRules.getPreferredNetwork(networks, requestType)
        assertEquals(expectedNetworkType, resultNetworkStatus!!.networkInfo.type)
    }

    private fun buildNetworksWithTypes(networkTypes: List<NetworkType>) = Networks(
        activeNetwork = null,
        networks = networkTypes.map { buildNetworkStatus(it) },
    )

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
        Wifi -> NetworkInfo.Wifi(name = "")
        Cell -> NetworkInfo.Cellular(name = "")
        BT -> NetworkInfo.Bluetooth(name = "")
        else -> NetworkInfo.Unknown()
    }

    private fun assertPrefers(
        input: List<NetworkType>,
        prefer: List<NetworkType>,
        expectedOutput: NetworkType,
    ) {
        assertEquals(
            expectedOutput,
            buildNetworksWithTypes(input).networks
                .prefer(*prefer.toTypedArray())
                ?.networkInfo
                ?.type,
        )
    }
}
