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
        val result: PaymentResult<Int> = PaymentResult.Failure(PaymentResultCode.Error, "Whoops!")

        assertNull(result.getOrNull())
    }

    @Test
    fun `map for success`() {
        val result = PaymentResult.Success(10)

        val mapped = result.map { "${it * 2}" }

        assertEquals("20", mapped.getOrNull())
    }

    @Test
    fun `map for failure`() {
        val result: PaymentResult<Int> = PaymentResult.Failure(PaymentResultCode.Error, "Whoops!")

        val mapped = result.map { "${it * 2}" }

        assertNull(mapped.getOrNull())
    }

    @Test
    fun `flatMap for success`() {
        val result = PaymentResult.Success(10)

        val mapped = result.flatMap { PaymentResult.Success("${it * 2}") }

        assertEquals("20", mapped.getOrNull())
    }

    @Test
    fun `flatMap for failure`() {
        val result: PaymentResult<Int> = PaymentResult.Failure(PaymentResultCode.Error, "Whoops!")

        val mapped = result.flatMap { PaymentResult.Success("${it * 2}") }

        assertNull(mapped.getOrNull())
    }

    @Test
    fun `onSuccess for success`() {
        val result = PaymentResult.Success(10)
        var value = "Hello"

        result.onSuccess { value = "${it * 2}" }

        assertEquals("20", value)
    }

    @Test
    fun `onSuccess for failure`() {
        val result: PaymentResult<Int> = PaymentResult.Failure(PaymentResultCode.Error, "Whoops!")
        var value = "Hello"

        result.onSuccess { value = "${it * 2}" }

        assertEquals("Hello", value)
    }

    @Test
    fun `onFailure for success`() {
        val result = PaymentResult.Success(10)
        var value = "Hello"

        result.onFailure { code, message -> value = "$code $message" }

        assertEquals("Hello", value)
    }

    @Test
    fun `onFailure for failure`() {
        val result: PaymentResult<Int> = PaymentResult.Failure(PaymentResultCode.Error, "Whoops!")
        var value = "Hello"

        result.onFailure { code, message -> value = "$code $message" }

        assertEquals("Error Whoops!", value)
    }

    @Test
    fun `recover for success`() {
        val result = PaymentResult.Success(10)

        val recovered = result.recover { _, _ -> PaymentResult.Success(55) }

        assertEquals(10, recovered.getOrNull())
    }

    @Test
    fun `recover for failure`() {
        val result: PaymentResult<Int> = PaymentResult.Failure(PaymentResultCode.Error, "Whoops!")

        val recovered = result.recover { _, _ -> PaymentResult.Success(55) }

        assertEquals(55, recovered.getOrNull())
    }
}
