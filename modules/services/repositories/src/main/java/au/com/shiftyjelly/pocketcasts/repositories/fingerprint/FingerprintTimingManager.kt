package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@Singleton
class FingerprintTimingManager @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val referenceRetriever: FingerprintReferenceRetriever,
) {
    // region Public Types

    sealed interface State {
        data object Idle : State
        data object Preparing : State
        data class Active(val coverage: Int) : State
        data class Failed(val error: Throwable) : State
        data object Unavailable : State
    }

    data class TimeMappingEntry(
        val playbackTime: Double,
        val referenceTime: Double,
        val score: Float = 0f,
    )

    // endregion

    // region Public State

    private val _stateFlow = MutableStateFlow<State>(State.Idle)
    val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()
    val state: State get() = _stateFlow.value

    // endregion

    // region Private State

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private var playbackToReference = mutableListOf<TimeMappingEntry>()
    private var referenceToPlayback = mutableListOf<TimeMappingEntry>()
    private var lastProgressPositionMs = -1
    private var generationJob: Job? = null
    private var preparationJob: Job? = null

    // Drift filter state
    private var filterLastTrusted: TimeMappingEntry? = null
    private var filterCandidatePool = mutableListOf<TimeMappingEntry>()

    // Context for current preparation
    private var currentEpisodeUuid: String? = null
    private var currentAudioFilePath: String? = null
    private var currentReferenceData: ByteArray? = null
    private var currentReferenceFilePath: String? = null
    private var currentReferenceDuration: Double = 0.0

    // endregion

    // region Public API

    fun prepareForCurrentEpisode() {
        val episode = playbackManager.getCurrentEpisode() ?: run {
            _stateFlow.value = State.Unavailable
            return
        }

        val episodeUuid = episode.uuid
        val podcastUuid = episode.podcastOrSubstituteUuid

        scope.launch {
            mutex.withLock { resetState() }
            prepareForEpisode(episodeUuid, podcastUuid, episode.downloadedFilePath, episode.duration)
        }
    }

    fun stop() {
        scope.launch {
            mutex.withLock {
                resetState()
                _stateFlow.value = State.Idle
            }
        }
        Timber.d("FingerprintTimingManager: stopped")
    }

    fun referenceTime(forPlaybackTimeMs: Int): Double? {
        val playbackTimeSec = forPlaybackTimeMs / 1000.0
        val entries = playbackToReference
        return interpolate(
            time = playbackTimeSec,
            entries = entries,
            keySelector = { it.playbackTime },
            valueSelector = { it.referenceTime },
        )
    }

    fun playbackTimeMs(forReferenceTime: Double): Int? {
        val entries = referenceToPlayback
        val result = interpolate(
            time = forReferenceTime,
            entries = entries,
            keySelector = { it.referenceTime },
            valueSelector = { it.playbackTime },
        ) ?: return null
        return (result * 1000).toInt()
    }

    // endregion

    // region State Management

    private fun resetState() {
        generationJob?.cancel()
        generationJob = null
        preparationJob?.cancel()
        preparationJob = null
        playbackToReference.clear()
        referenceToPlayback.clear()
        lastProgressPositionMs = -1
        filterLastTrusted = null
        filterCandidatePool.clear()
        currentEpisodeUuid = null
        currentAudioFilePath = null
        currentReferenceData = null
        currentReferenceFilePath = null
        currentReferenceDuration = 0.0
    }

    // endregion

    // region Preparation

    private suspend fun prepareForEpisode(
        episodeUuid: String,
        podcastUuid: String,
        downloadedFilePath: String?,
        durationSec: Double,
    ) {
        if (!FeatureFlag.isEnabled(Feature.SYNCED_TRANSCRIPTS)) {
            _stateFlow.value = State.Unavailable
            Timber.d("FingerprintTimingManager: feature disabled")
            return
        }

        if (durationSec <= 0) {
            _stateFlow.value = State.Unavailable
            Timber.d("FingerprintTimingManager: invalid duration for $episodeUuid")
            return
        }

        val audioFilePath = downloadedFilePath
        if (audioFilePath == null) {
            _stateFlow.value = State.Unavailable
            Timber.d("FingerprintTimingManager: no audio file for $episodeUuid")
            return
        }

        currentEpisodeUuid = episodeUuid
        currentAudioFilePath = audioFilePath

        // Try loading cached reference from disk
        var referenceData = referenceRetriever.loadReferenceData(audioFilePath)
        if (referenceData != null) {
            val reference = ReferenceFingerprint.decode(referenceData)
            if (reference != null) {
                configureForReference(reference, referenceData, audioFilePath, episodeUuid)
                return
            }
        }

        // Fetch from server
        _stateFlow.value = State.Preparing
        Timber.d("FingerprintTimingManager: fetching reference from server for $episodeUuid")

        val baseUrl = "https://shownotes.pocketcasts.com/generated_transcripts/"
        referenceData = referenceRetriever.fetchReferenceData(baseUrl, podcastUuid, episodeUuid)

        if (referenceData == null) {
            _stateFlow.value = State.Unavailable
            Timber.d("FingerprintTimingManager: no reference available for $episodeUuid")
            return
        }

        val reference = ReferenceFingerprint.decode(referenceData)
        if (reference == null) {
            _stateFlow.value = State.Unavailable
            Timber.d("FingerprintTimingManager: failed to decode reference for $episodeUuid")
            return
        }

        referenceRetriever.saveReferenceData(referenceData, audioFilePath)
        configureForReference(reference, referenceData, audioFilePath, episodeUuid)
    }

    private suspend fun configureForReference(
        reference: ReferenceFingerprint,
        referenceData: ByteArray,
        audioFilePath: String,
        episodeUuid: String,
    ) {
        val libraryCheckpoints = reference.libraryCheckpoints()
        if (libraryCheckpoints.isEmpty()) {
            _stateFlow.value = State.Unavailable
            Timber.d("FingerprintTimingManager: no usable checkpoints for $episodeUuid")
            return
        }

        val refPath = FingerprintReferenceRetriever.referencePath(audioFilePath)
        currentReferenceData = referenceData
        currentReferenceFilePath = refPath
        currentReferenceDuration = reference.totalDuration

        _stateFlow.value = State.Preparing

        Timber.d(
            "FingerprintTimingManager: reference for $episodeUuid — " +
                "totalDuration=${reference.totalDuration}s, " +
                "${libraryCheckpoints.size} checkpoints",
        )

        // Try loading mapping cache
        val cached = FingerprintMappingCache.load(audioFilePath, refPath, referenceData)
        if (cached != null) {
            mutex.withLock {
                playbackToReference = cached.entries.toMutableList()
                referenceToPlayback = cached.entries.sortedBy { it.referenceTime }.toMutableList()
                filterLastTrusted = cached.entries.lastOrNull()
            }
            val coverage = cached.entries.size
            _stateFlow.value = State.Active(coverage)
            Timber.d("FingerprintTimingManager: loaded mapping from cache for $episodeUuid ($coverage entries)")
            return
        }

        // TODO: Start streaming fingerprint generation when the fingerprint library is available.
        // For now, transition to Unavailable since the library doesn't exist yet.
        _stateFlow.value = State.Unavailable
        Timber.d("FingerprintTimingManager: fingerprint library not available — cannot generate mappings for $episodeUuid")
    }

    // endregion

    // region Playback Progress Handling

    fun onPlaybackProgress(positionMs: Int, episodeUuid: String?) {
        if (episodeUuid != currentEpisodeUuid) return

        scope.launch {
            mutex.withLock {
                processProgress(positionMs)
            }
        }
    }

    private fun processProgress(positionMs: Int) {
        if (lastProgressPositionMs >= 0) {
            val deltaMs = abs(positionMs - lastProgressPositionMs)
            if (deltaMs > FingerprintConstants.RESTART_DELTA_SECONDS * 1000) {
                Timber.d("FingerprintTimingManager: playback jumped ${deltaMs}ms — would restart")
                // TODO: restart fingerprint generation from new position
                lastProgressPositionMs = positionMs
                return
            }
        }
        lastProgressPositionMs = positionMs
    }

    private fun isWithinMappedRange(playbackTimeSec: Double): Boolean {
        val entries = playbackToReference
        val first = entries.firstOrNull() ?: return false
        val last = entries.lastOrNull() ?: return false
        val margin = FingerprintConstants.PLAYBACK_RANGE_MARGIN_SECONDS
        return playbackTimeSec >= first.playbackTime - margin &&
            playbackTimeSec <= last.playbackTime + margin
    }

    // endregion

    // region Drift Filter

    internal fun consider(candidate: TimeMappingEntry): Int {
        val trusted = filterLastTrusted
        if (trusted != null && isInTrend(candidate, trusted)) {
            flushPoolAsRejected()
            insertMapping(candidate)
            filterLastTrusted = candidate
            return 1
        }

        filterCandidatePool.add(candidate)
        val n = FingerprintConstants.DRIFT_BOOTSTRAP_COUNT

        if (filterCandidatePool.size < n) return 0

        val recent = filterCandidatePool.takeLast(n)
        if (formsConsistentSequence(recent)) {
            val keepStart = filterCandidatePool.size - n
            for (i in 0 until keepStart) {
                Timber.d("FingerprintTimingManager: drift filter evicted candidate at playback ${filterCandidatePool[i].playbackTime}s")
            }
            for (entry in recent) {
                insertMapping(entry)
            }
            filterLastTrusted = recent.last()
            filterCandidatePool.clear()
            return n
        }

        val evicted = filterCandidatePool.removeFirst()
        Timber.d("FingerprintTimingManager: drift filter evicted candidate at playback ${evicted.playbackTime}s")
        return 0
    }

    private fun flushPoolAsRejected() {
        filterCandidatePool.clear()
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

    // endregion

    // region Time Mapping

    internal fun insertMapping(entry: TimeMappingEntry) {
        val pbIdx = playbackToReference.sortedInsertionIndex { it.playbackTime < entry.playbackTime }
        playbackToReference.add(pbIdx, entry)

        val refIdx = referenceToPlayback.sortedInsertionIndex { it.referenceTime < entry.referenceTime }
        referenceToPlayback.add(refIdx, entry)
    }

    /** Test seam for inserting mappings synchronously. */
    fun insert(mapping: TimeMappingEntry) {
        insertMapping(mapping)
    }

    /** Test seam for routing candidates through the drift filter synchronously. */
    fun stubMatches(entries: List<TimeMappingEntry>) {
        for (entry in entries) {
            consider(entry)
        }
    }

    // endregion

    // region Interpolation

    companion object {
        fun interpolate(
            time: Double,
            entries: List<TimeMappingEntry>,
            keySelector: (TimeMappingEntry) -> Double,
            valueSelector: (TimeMappingEntry) -> Double,
        ): Double? {
            if (entries.isEmpty()) return null

            val last = entries.size - 1

            if (time <= keySelector(entries[0])) {
                val offset = time - keySelector(entries[0])
                return valueSelector(entries[0]) + offset
            }

            if (time >= keySelector(entries[last])) {
                val offset = time - keySelector(entries[last])
                return valueSelector(entries[last]) + offset
            }

            var lo = 0
            var hi = last
            while (lo < hi - 1) {
                val mid = (lo + hi) / 2
                if (keySelector(entries[mid]) <= time) {
                    lo = mid
                } else {
                    hi = mid
                }
            }

            val t0 = keySelector(entries[lo])
            val t1 = keySelector(entries[hi])
            val v0 = valueSelector(entries[lo])
            val v1 = valueSelector(entries[hi])

            val fraction = if (t1 > t0) (time - t0) / (t1 - t0) else 0.0
            return v0 + fraction * (v1 - v0)
        }

        fun alignToWindowGrid(time: Double): Double {
            val stride = FingerprintConstants.WINDOW_INTERVAL_MS / 1000.0
            if (stride <= 0) return max(0.0, time)
            return max(0.0, floor(time / stride) * stride)
        }
    }

    // endregion
}

private inline fun <T> MutableList<T>.sortedInsertionIndex(crossinline predicate: (T) -> Boolean): Int {
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
