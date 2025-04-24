package au.com.shiftyjelly.pocketcasts.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentResultTest {
    @Test
    fun `getOrNull for success`() {
        val result = PaymentResult.Success(10)

        assertEquals(10, result.getOrNull())
    }

    @Test
    fun `getOrNull for failure`() {
        val result: PaymentResult<Int> = PaymentResult.Failure("Error")

        assertNull(result.getOrNull())
    }
}
