/**
 * Kotlin wrapper for the audio fingerprinting library.
 *
 * Example usage:
 * ```kotlin
 * val fingerprinter = Fingerprinter()
 *
 * // Generate windowed fingerprints from audio data
 * val windows = fingerprinter.fingerprintBytesWindowed(audioBytes)
 * println("Generated ${windows.size} fingerprint windows")
 *
 * // Compare fingerprints with drift compensation
 * val hashes1 = windows[0].hashes.toLongArray()
 * val hashes2 = windows[1].hashes.toLongArray()
 * val score = Fingerprinter.compareHashesWithDrift(hashes1, hashes2, maxDrift = 10)
 * println("Similarity: $score")
 *
 * // Don't forget to close resources
 * fingerprinter.close()
 * ```
 */
package com.pocketcasts.fingerprint

import java.io.Closeable

/**
 * A windowed fingerprint with timestamp information.
 */
data class WindowedFingerprint(
    /** Timestamp in milliseconds from the start of the audio. */
    val timestampMs: Long,
    /** Duration of the fingerprinted segment in milliseconds. */
    val durationMs: Int,
    /** Raw hash values as unsigned 32-bit integers stored in Longs. */
    val hashes: List<Long>
) {
    /** Convert hashes to a comma-separated string (for JSON output). */
    fun hashesToString(): String = hashes.joinToString(",")
}

/**
 * A compact audio fingerprint.
 */
class FingerprintResult internal constructor(private var handle: Long) : Closeable {
    init {
        require(handle != 0L) { "Invalid fingerprint handle" }
    }

    /**
     * Duration of the audio in milliseconds.
     */
    val durationMs: Int
        get() {
            checkNotClosed()
            return nativeDurationMs(handle)
        }

    /**
     * Number of hash values in the fingerprint.
     */
    val hashCount: Int
        get() {
            checkNotClosed()
            return nativeHashCount(handle)
        }

    /**
     * Get the raw fingerprint data as bytes.
     */
    fun toByteArray(): ByteArray {
        checkNotClosed()
        return nativeGetData(handle)
    }

    /**
     * Get the raw hash values.
     */
    fun getHashes(): IntArray {
        checkNotClosed()
        return nativeGetHashes(handle)
    }

    /**
     * Close and release native resources.
     */
    override fun close() {
        if (handle != 0L) {
            nativeFree(handle)
            handle = 0L
        }
    }

    private fun checkNotClosed() {
        check(handle != 0L) { "FingerprintResult has been closed" }
    }

    protected fun finalize() {
        close()
    }

    companion object {
        /**
         * Create a fingerprint from raw data bytes.
         */
        @JvmStatic
        fun fromByteArray(data: ByteArray): FingerprintResult? {
            val handle = nativeFromData(data)
            return if (handle != 0L) FingerprintResult(handle) else null
        }

        @JvmStatic
        private external fun nativeDurationMs(handle: Long): Int

        @JvmStatic
        private external fun nativeHashCount(handle: Long): Int

        @JvmStatic
        private external fun nativeGetData(handle: Long): ByteArray

        @JvmStatic
        private external fun nativeGetHashes(handle: Long): IntArray

        @JvmStatic
        private external fun nativeFree(handle: Long)

        @JvmStatic
        private external fun nativeFromData(data: ByteArray): Long
    }
}

/**
 * Audio fingerprinter for generating fingerprints from audio data.
 */
class Fingerprinter : Closeable {
    private var handle: Long = nativeNew()

    init {
        require(handle != 0L) { "Failed to create Fingerprinter" }
    }

    /**
     * Generate windowed fingerprints from raw audio data.
     *
     * @param data Raw audio file data (WAV or MP3 format).
     * @param windowDurationMs Duration of each window in milliseconds (default: 8000).
     * @param windowIntervalMs Interval between windows in milliseconds (default: 2000).
     * @return List of WindowedFingerprint, or empty list if generation failed.
     */
    fun fingerprintBytesWindowed(
        data: ByteArray,
        windowDurationMs: Int = 8000,
        windowIntervalMs: Int = 2000
    ): List<WindowedFingerprint> {
        checkNotClosed()
        val result = nativeFromBytesWindowed(handle, data, windowDurationMs, windowIntervalMs)
            ?: return emptyList()

        return result.map { entry ->
            val timestampMs = entry[0]
            val durationMs = entry[1].toInt()
            val hashes = entry.drop(2).map { it.toUInt().toLong() }
            WindowedFingerprint(timestampMs, durationMs, hashes)
        }
    }

    /**
     * Close and release native resources.
     */
    override fun close() {
        if (handle != 0L) {
            nativeFree(handle)
            handle = 0L
        }
    }

    private fun checkNotClosed() {
        check(handle != 0L) { "Fingerprinter has been closed" }
    }

    protected fun finalize() {
        close()
    }

    companion object {
        init {
            System.loadLibrary("fingerprint_ffi")
        }

        /**
         * Get the library version.
         */
        @JvmStatic
        val version: String
            external get

        /**
         * Compare two fingerprint hash arrays with drift compensation.
         * This is the unified comparison method implemented in native Rust code.
         *
         * @param hashes1 First fingerprint hash array (as unsigned 32-bit values stored in Long).
         * @param hashes2 Second fingerprint hash array (as unsigned 32-bit values stored in Long).
         * @param maxDrift Maximum number of hash positions to shift for alignment (0 = no drift).
         * @return Similarity score between 0.0 (completely different) and 1.0 (identical).
         */
        @JvmStatic
        fun compareHashesWithDrift(hashes1: LongArray, hashes2: LongArray, maxDrift: Int): Float {
            return nativeCompareHashesWithDrift(hashes1, hashes2, maxDrift)
        }

        @JvmStatic
        private external fun nativeNew(): Long

        @JvmStatic
        private external fun nativeFree(handle: Long)

        @JvmStatic
        private external fun nativeFromBytesWindowed(handle: Long, data: ByteArray, windowDurationMs: Int, windowIntervalMs: Int): Array<LongArray>?

        @JvmStatic
        private external fun nativeCompareHashesWithDrift(hashes1: LongArray, hashes2: LongArray, maxDrift: Int): Float
    }
}
