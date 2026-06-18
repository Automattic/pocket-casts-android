package au.com.shiftyjelly.pocketcasts.transcripts.ui

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry

internal sealed interface HighlightOutcome {
    data class Show(val entryIndex: Int) : HighlightOutcome

    data object Clear : HighlightOutcome

    data object Keep : HighlightOutcome
}

internal object TranscriptCueHelper {

    /**
     * Resolves what the highlight should do for a given reference time. Pure so it can be
     * shared between the playing frame loop and the paused recompute, and unit tested.
     */
    fun resolveHighlight(
        entries: List<TranscriptEntry>,
        refTimeMs: Long,
        cachedIndex: Int,
    ): HighlightOutcome {
        val idx = findCueIndex(entries, refTimeMs, cachedIndex)
        if (idx != null) {
            return HighlightOutcome.Show(entryIndex = idx)
        }
        // No cue contains the time — we're in a gap between sentences. Mirror iOS: keep the
        // previous highlight rather than clearing, unless playback is before the first cue.
        return if (isBeforeFirstCue(entries, refTimeMs)) HighlightOutcome.Clear else HighlightOutcome.Keep
    }

    private fun isBeforeFirstCue(
        entries: List<TranscriptEntry>,
        refTimeMs: Long,
    ): Boolean {
        val firstCue = entries.firstOrNull { it is TranscriptEntry.Text && it.startTimeMs >= 0 } as? TranscriptEntry.Text
        return firstCue != null && refTimeMs < firstCue.startTimeMs
    }

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
        // Strict containment: no cue's [startTimeMs, endTimeMs] range contains the time.
        // Mirrors iOS `currentCue()` returning nil so the caller can hold the previous highlight.
        return null
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
}
