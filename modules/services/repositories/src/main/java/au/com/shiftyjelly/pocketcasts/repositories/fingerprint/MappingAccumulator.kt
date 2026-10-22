package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager.TimeMappingEntry
import kotlin.math.abs

/**
 * Sorted playback<->reference anchor lists plus the drift-filter state that feeds them.
 * The continuous transcript flow and one-shot chapter resolves each use their own instance,
 * so a resolve can never disturb the live mapping.
 */
internal class MappingAccumulator {
    val playbackToReference = mutableListOf<TimeMappingEntry>()
    val referenceToPlayback = mutableListOf<TimeMappingEntry>()
    var lastTrusted: TimeMappingEntry? = null
    val candidatePool = mutableListOf<TimeMappingEntry>()

    fun insert(entry: TimeMappingEntry) {
        val pbIdx = playbackToReference.sortedInsertionIndex { it.playbackTime < entry.playbackTime }
        playbackToReference.add(pbIdx, entry)

        val refIdx = referenceToPlayback.sortedInsertionIndex { it.referenceTime < entry.referenceTime }
        referenceToPlayback.add(refIdx, entry)
    }

    fun hasAnchorNear(playbackTime: Double, toleranceSec: Double): Boolean {
        val idx = playbackToReference.sortedInsertionIndex { it.playbackTime < playbackTime }
        val prev = playbackToReference.getOrNull(idx - 1)
        val next = playbackToReference.getOrNull(idx)
        return (prev != null && playbackTime - prev.playbackTime <= toleranceSec) ||
            (next != null && next.playbackTime - playbackTime <= toleranceSec)
    }

    fun replaceAll(entries: List<TimeMappingEntry>) {
        playbackToReference.clear()
        playbackToReference += entries.sortedBy { it.playbackTime }
        referenceToPlayback.clear()
        referenceToPlayback += entries.sortedBy { it.referenceTime }
    }

    fun consider(candidate: TimeMappingEntry, onRejected: (TimeMappingEntry) -> Unit): Int {
        val trusted = lastTrusted
        if (trusted != null && isInTrend(candidate, trusted)) {
            candidatePool.forEach(onRejected)
            candidatePool.clear()
            insert(candidate)
            lastTrusted = candidate
            return 1
        }

        candidatePool.add(candidate)
        val n = FingerprintConstants.DRIFT_BOOTSTRAP_COUNT
        if (candidatePool.size < n) return 0

        val recent = candidatePool.takeLast(n)
        if (formsConsistentSequence(recent)) {
            for (i in 0 until candidatePool.size - n) {
                onRejected(candidatePool[i])
            }
            recent.forEach(::insert)
            lastTrusted = recent.last()
            candidatePool.clear()
            return n
        }

        onRejected(candidatePool.removeAt(0))
        return 0
    }

    fun resetFilter() {
        lastTrusted = null
        candidatePool.clear()
    }

    fun reset() {
        playbackToReference.clear()
        referenceToPlayback.clear()
        resetFilter()
    }

    private fun isInTrend(candidate: TimeMappingEntry, anchor: TimeMappingEntry): Boolean {
        val deltaPlayback = candidate.playbackTime - anchor.playbackTime
        val deltaReference = candidate.referenceTime - anchor.referenceTime
        return abs(deltaReference - deltaPlayback) <= FingerprintConstants.DRIFT_TOLERANCE_SECONDS
    }

    private fun formsConsistentSequence(entries: List<TimeMappingEntry>): Boolean {
        if (entries.size < 2) return true
        for (i in 1 until entries.size) {
            if (!isInTrend(entries[i], entries[i - 1])) return false
        }
        return true
    }
}

internal inline fun <T> MutableList<T>.sortedInsertionIndex(crossinline predicate: (T) -> Boolean): Int {
    var lo = 0
    var hi = size
    while (lo < hi) {
        val mid = (lo + hi) / 2
        if (predicate(this[mid])) {
            lo = mid + 1
        } else {
            hi = mid
        }
    }
    return lo
}
