package au.com.shiftyjelly.pocketcasts.wear.networking

import com.google.android.horologist.networks.data.NetworkInfo
import com.google.android.horologist.networks.data.NetworkStatus
import com.google.android.horologist.networks.data.NetworkType
import com.google.android.horologist.networks.data.NetworkType.BT
import com.google.android.horologist.networks.data.NetworkType.Cell
import com.google.android.horologist.networks.data.NetworkType.Wifi
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PocketCastsNetworkingRulesTest {

    @Test
    fun `prefer returns match`() {
        assertPrefers(
            input = listOf(Cell, Wifi, BT),
            prefer = listOf(Cell),
            expectedOutput = Cell
        )

        assertPrefers(
            input = listOf(Cell, Wifi, BT),
            prefer = listOf(Wifi),
            expectedOutput = Wifi
        )

        assertPrefers(
            input = listOf(Cell, Wifi, BT),
            prefer = listOf(BT),
            expectedOutput = BT
        )
    }

    @Test
    fun `prefer returns first if no match`() {
        assertPrefers(
            input = listOf(Wifi, BT),
            prefer = listOf(Cell),
            expectedOutput = Wifi
        )

        assertPrefers(
            input = listOf(BT, Cell),
            prefer = listOf(Wifi),
            expectedOutput = BT
        )

        assertPrefers(
            input = listOf(Cell, Wifi),
            prefer = listOf(BT),
            expectedOutput = Cell
        )
    }

    @Test
    fun `prefer returns second if no match on first`() {
        assertPrefers(
            input = listOf(Wifi, BT),
            prefer = listOf(Cell, BT),
            expectedOutput = BT
        )

        assertPrefers(
            input = listOf(BT, Cell),
            prefer = listOf(Wifi, Cell),
            expectedOutput = Cell
        )

        assertPrefers(
            input = listOf(Cell, Wifi),
            prefer = listOf(BT, Cell),
            expectedOutput = Cell
        )
    }

    private fun assertPrefers(
        input: List<NetworkType>,
        prefer: List<NetworkType>,
        expectedOutput: NetworkType,
    ) {
        assertEquals(
            expectedOutput,
            input.map {
                val networkInfo = mock<NetworkInfo>()
                whenever(networkInfo.type).thenReturn(it)
                val networkStatus = mock<NetworkStatus>()
                whenever(networkStatus.networkInfo).thenReturn(networkInfo)
                networkStatus
            }
                .prefer(*prefer.toTypedArray())
                ?.networkInfo
                ?.type
        )
    }
}
