package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.lfm

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClassifierHeadTest {
    @Test
    fun loadFixture_andArgmaxKnownVector() {
        val classifier = resourceBytes("lfm_test_classifier.bin")
        val head = LfmcClassifier.load(classifier, expectedHiddenSize = 3)
        val label = head.classify(floatArrayOf(1f, 0f, 0f))
        assertEquals("playback:pause", label)
    }

    @Test
    fun wrongMagic_failsClosed() {
        val bad = ByteArray(12) { index -> (index + 1).toByte() }
        assertNull(LfmcClassifier.loadOrNull(bad, expectedHiddenSize = 3))
    }

    @Test
    fun hiddenSizeMismatch_failsClosed() {
        val classifier = resourceBytes("lfm_test_classifier.bin")
        assertNull(LfmcClassifier.loadOrNull(classifier, expectedHiddenSize = 4))
    }

    @Test
    fun embeddingSizeMismatch_doesNotClassify() {
        val classifier = resourceBytes("lfm_test_classifier.bin")
        val head = LfmcClassifier.load(classifier, expectedHiddenSize = 3)
        assertNull(head.classifyOrNull(floatArrayOf(1f, 0f)))
    }

    private fun resourceBytes(name: String): ByteArray {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) {
            "missing test resource $name"
        }
        return stream.use { it.readBytes() }
    }
}

/**
 * JVM mirror of [ClassifierHead.cpp] LFMC parsing and argmax for unit tests.
 * Production inference uses [LfmNative] / C++.
 */
internal class LfmcClassifier private constructor(
    private val hiddenSize: Int,
    private val weight: FloatArray,
    private val bias: FloatArray,
    private val labels: List<String>,
) {
    fun classify(embedding: FloatArray): String {
        return classifyOrNull(embedding) ?: error("embedding hidden_size mismatch")
    }

    fun classifyOrNull(embedding: FloatArray): String? {
        if (embedding.size != hiddenSize) return null
        val normalized = embedding.copyOf()
        l2Normalize(normalized)
        var bestIndex = 0
        var bestScore = Float.NEGATIVE_INFINITY
        for (label in bias.indices) {
            var score = bias[label]
            val offset = label * hiddenSize
            for (dim in 0 until hiddenSize) {
                score += weight[offset + dim] * normalized[dim]
            }
            if (score > bestScore) {
                bestScore = score
                bestIndex = label
            }
        }
        return labels[bestIndex]
    }

    companion object {
        private val magic = byteArrayOf('L'.code.toByte(), 'F'.code.toByte(), 'M'.code.toByte(), 'C'.code.toByte())

        fun load(bytes: ByteArray, expectedHiddenSize: Int = -1): LfmcClassifier {
            return loadOrNull(bytes, expectedHiddenSize) ?: error("invalid classifier.bin")
        }

        fun loadOrNull(bytes: ByteArray, expectedHiddenSize: Int = -1): LfmcClassifier? {
            if (bytes.size < 12 || !bytes.copyOfRange(0, 4).contentEquals(magic)) return null
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(4)
            val numLabels = buffer.int
            val hiddenSize = buffer.int
            if (numLabels <= 0 || hiddenSize <= 0) return null
            if (expectedHiddenSize > 0 && hiddenSize != expectedHiddenSize) return null
            val expectedBytes = 12 + (numLabels * hiddenSize * 4) + (numLabels * 4)
            if (bytes.size != expectedBytes) return null
            val weight = FloatArray(numLabels * hiddenSize)
            val bias = FloatArray(numLabels)
            for (index in weight.indices) weight[index] = buffer.float
            for (index in bias.indices) bias[index] = buffer.float
            val labels = defaultLabels(numLabels)
            return LfmcClassifier(hiddenSize, weight, bias, labels)
        }

        private fun defaultLabels(numLabels: Int): List<String> = when (numLabels) {
            2 -> listOf("playback:pause", "no_match:")
            else -> List(numLabels) { "label:$it" }
        }

        private fun l2Normalize(vector: FloatArray, epsilon: Float = 1e-6f) {
            var sumSquares = 0.0
            for (value in vector) sumSquares += value * value
            val scale = (1.0 / maxOf(Math.sqrt(sumSquares), epsilon.toDouble())).toFloat()
            for (index in vector.indices) vector[index] *= scale
        }
    }
}
