package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlin.io.encoding.Base64
import com.squareup.moshi.Moshi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import timber.log.Timber

@JsonClass(generateAdapter = true)
data class ReferenceFingerprint(
    val format: String,
    @Json(name = "total_duration") val totalDuration: Double,
    @Json(name = "checkpoint_interval") val checkpointInterval: Int,
    @Json(name = "checkpoint_duration") val checkpointDuration: Int,
    @Json(name = "timestamp_quantum") val timestampQuantum: Int,
    val checkpoints: List<List<Any>>,
) {
    data class LibraryCheckpoint(
        val timestampSeconds: Float,
        val hashes: IntArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LibraryCheckpoint) return false
            return timestampSeconds == other.timestampSeconds && hashes.contentEquals(other.hashes)
        }

        override fun hashCode(): Int {
            return 31 * timestampSeconds.hashCode() + hashes.contentHashCode()
        }
    }

    val checkpointDurationSeconds: Float get() = checkpointDuration.toFloat()

    fun libraryCheckpoints(): List<LibraryCheckpoint> {
        var accumulated = 0
        return checkpoints.mapNotNull { checkpoint ->
            if (checkpoint.size < 2) return@mapNotNull null
            val delta = (checkpoint[0] as? Number)?.toInt() ?: return@mapNotNull null
            val data = checkpoint[1] as? String ?: return@mapNotNull null

            accumulated += delta

            val payload = try {
                Base64.Default.decode(data)
            } catch (e: IllegalArgumentException) {
                return@mapNotNull null
            }
            if (payload.size % 4 != 0) return@mapNotNull null

            val count = payload.size / 4
            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            val hashes = IntArray(count) { buffer.getInt() }

            val timestamp = accumulated.toFloat() * timestampQuantum.toFloat()
            LibraryCheckpoint(timestampSeconds = timestamp, hashes = hashes)
        }
    }

    companion object {
        const val SUPPORTED_FORMAT = "fingerprint-compact-v2"

        private val moshi = Moshi.Builder().build()
        private val adapter = moshi.adapter(ReferenceFingerprint::class.java)

        fun decode(data: ByteArray): ReferenceFingerprint? {
            return try {
                val fingerprint = adapter.fromJson(String(data, Charsets.UTF_8)) ?: return null

                if (fingerprint.format != SUPPORTED_FORMAT) {
                    Timber.w("ReferenceFingerprint: unknown format '${fingerprint.format}', expected '$SUPPORTED_FORMAT'")
                    return null
                }

                fingerprint
            } catch (e: Exception) {
                Timber.w(e, "ReferenceFingerprint: failed to decode JSON")
                null
            }
        }
    }
}
