package au.com.shiftyjelly.pocketcasts.transcripts.ui

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import kotlin.math.abs

internal object TranscriptCueHelper {

    fun findCueIndex(
        entries: List<TranscriptEntry>,
        refTimeMs: Long,
        cachedIndex: Int,
    ): Int? {
        if (entries.isEmpty()) return null
        val cached = cachedIndex.coerceIn(0, entries.size - 1)

        val cachedEntry = entries[cached]
        if (cachedEntry is TranscriptEntry.Text && cachedEntry.startTimeMs >= 0 &&
            refTimeMs >= cachedEntry.startTimeMs && refTimeMs <= cachedEntry.endTimeMs
        ) {
            return cached
        }

        val scanLimit = 10
        val nearbyResult = findCueNearby(entries, refTimeMs, cached, scanLimit)
        if (nearbyResult != null) return nearbyResult

        return findCueBinarySearch(entries, refTimeMs)
    }

    fun findCueNearby(
        entries: List<TranscriptEntry>,
        refTimeMs: Long,
        cached: Int,
        scanLimit: Int,
    ): Int? {
        for (i in (cached + 1) until minOf(cached + 1 + scanLimit, entries.size)) {
            val entry = entries[i]
            if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) {
                if (entry.startTimeMs > refTimeMs) break
                if (refTimeMs >= entry.startTimeMs && refTimeMs <= entry.endTimeMs) return i
            }
        }
        for (i in (cached - 1) downTo maxOf(cached - scanLimit, 0)) {
            val entry = entries[i]
            if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) {
                if (refTimeMs >= entry.startTimeMs && refTimeMs <= entry.endTimeMs) return i
            }
        }
        return null
    }

    fun findCueBinarySearch(
        entries: List<TranscriptEntry>,
        refTimeMs: Long,
    ): Int? {
        var lo = 0
        var hi = entries.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val entry = entries[mid]
            if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) {
                when {
                    refTimeMs < entry.startTimeMs -> hi = mid - 1
                    refTimeMs > entry.endTimeMs -> lo = mid + 1
                    else -> return mid
                }
            } else {
                val timedIdx = findNearestTimedEntry(entries, mid, lo, hi)
                if (timedIdx == null) {
                    break
                } else {
                    val timed = entries[timedIdx] as TranscriptEntry.Text
                    when {
                        refTimeMs < timed.startTimeMs -> hi = timedIdx - 1
                        refTimeMs > timed.endTimeMs -> lo = timedIdx + 1
                        else -> return timedIdx
                    }
                }
            }
        }
        return findClosestTimedEntry(entries, refTimeMs, lo.coerceIn(0, entries.size - 1))
    }

    fun findClosestTimedEntry(
        entries: List<TranscriptEntry>,
        refTimeMs: Long,
        around: Int,
    ): Int? {
        var bestIndex: Int? = null
        var bestDistance = Long.MAX_VALUE
        val scanRadius = 5
        val start = maxOf(0, around - scanRadius)
        val end = minOf(entries.size - 1, around + scanRadius)
        for (i in start..end) {
            val entry = entries[i]
            if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) {
                val dist = minOf(
                    abs(refTimeMs - entry.startTimeMs),
                    abs(refTimeMs - entry.endTimeMs),
                )
                if (dist < bestDistance) {
                    bestDistance = dist
                    bestIndex = i
                }
            }
        }
        return if (bestDistance <= NEAREST_CUE_THRESHOLD_MS) bestIndex else null
    }

    fun findNearestTimedEntry(
        entries: List<TranscriptEntry>,
        mid: Int,
        lo: Int,
        hi: Int,
    ): Int? {
        var left = mid - 1
        var right = mid + 1
        while (left >= lo || right <= hi) {
            if (right <= hi) {
                val entry = entries[right]
                if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) return right
                right++
            }
            if (left >= lo) {
                val entry = entries[left]
                if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) return left
                left--
            }
        }
        return null
    }

    const val NEAREST_CUE_THRESHOLD_MS = 5000L
}
