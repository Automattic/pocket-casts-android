package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import au.com.shiftyjelly.pocketcasts.repositories.BuildConfig
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import uniffi.fingerprint_uniffi.CheckpointMatcher
import uniffi.fingerprint_uniffi.StreamingWindowedFingerprinter
import uniffi.fingerprint_uniffi.WindowedFingerprint

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
    private var progressObserverJob: Job? = null

    // Drift filter state
    private var filterLastTrusted: TimeMappingEntry? = null
    private var filterCandidatePool = mutableListOf<TimeMappingEntry>()

    // Context for current preparation
    private var currentEpisodeUuid: String? = null
    private var currentAudioFilePath: String? = null
    private var currentReferenceData: ByteArray? = null
    private var currentReferenceFilePath: String? = null
    private var currentReferenceDuration: Double = 0.0
    private var currentMatcher: CheckpointMatcher? = null
    private var hasReachedActive = false

    // endregion

    // region Public API

    fun prepareForCurrentEpisode() {
        val episode = playbackManager.getCurrentEpisode() ?: run {
            _stateFlow.value = State.Unavailable
            return
        }

        val episodeUuid = episode.uuid
        val podcastUuid = episode.podcastOrSubstituteUuid

        val audioSource = episode.downloadedFilePath ?: episode.downloadUrl

        scope.launch {
            mutex.withLock { resetState() }
            prepareForEpisode(episodeUuid, podcastUuid, audioSource, episode.isDownloaded, episode.duration)
        }

        startPlaybackProgressObserver(episodeUuid)
    }

    private fun startPlaybackProgressObserver(episodeUuid: String) {
        progressObserverJob?.cancel()
        progressObserverJob = scope.launch {
            playbackManager.playbackStateFlow.collect { state ->
                if (state.episodeUuid == episodeUuid && state.isPlaying) {
                    onPlaybackProgress(state.positionMs, state.episodeUuid)
                }
            }
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
        progressObserverJob?.cancel()
        progressObserverJob = null
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
        currentMatcher?.close()
        currentMatcher = null
        hasReachedActive = false
    }

    // endregion

    // region Preparation

    private suspend fun prepareForEpisode(
        episodeUuid: String,
        podcastUuid: String,
        audioSource: String?,
        isDownloaded: Boolean,
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

        if (audioSource == null) {
            _stateFlow.value = State.Unavailable
            Timber.d("FingerprintTimingManager: no audio source for $episodeUuid")
            return
        }

        currentEpisodeUuid = episodeUuid
        currentAudioFilePath = audioSource

        // Try loading cached reference from disk (only for downloaded episodes)
        if (isDownloaded) {
            val referenceData = referenceRetriever.loadReferenceData(audioSource)
            if (referenceData != null) {
                val reference = ReferenceFingerprint.decode(referenceData)
                if (reference != null) {
                    configureForReference(reference, referenceData, audioSource, episodeUuid)
                    return
                }
            }
        }

        // Fetch from server
        _stateFlow.value = State.Preparing
        Timber.d("FingerprintTimingManager: fetching reference from server for $episodeUuid")

        val baseUrl = "${BuildConfig.SERVER_SHOW_NOTES_URLS}/generated_transcripts/"
        val referenceData = referenceRetriever.fetchReferenceData(baseUrl, podcastUuid, episodeUuid)

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

        if (isDownloaded) {
            referenceRetriever.saveReferenceData(referenceData, audioSource)
        }
        configureForReference(reference, referenceData, audioSource, episodeUuid)
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

        // Create matcher and populate with reference checkpoints
        val matcher = CheckpointMatcher()
        for (checkpoint in libraryCheckpoints) {
            matcher.add(
                checkpoint.timestampSeconds,
                checkpoint.hashes.map { it.toUInt() },
                reference.checkpointDurationSeconds,
            )
        }
        currentMatcher = matcher

        // Try loading mapping cache (only for local files)
        val isLocalFile = !audioFilePath.startsWith("http://") && !audioFilePath.startsWith("https://")
        val cached = if (isLocalFile) FingerprintMappingCache.load(audioFilePath, refPath, referenceData) else null
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

        // Start streaming fingerprint generation
        val currentTime = playbackManager.playbackStateRelay.blockingFirst().positionMs / 1000.0
        startStream(audioFilePath, matcher, episodeUuid, startPosition = currentTime)
    }

    // endregion

    // region Playback Progress Handling

    private fun onPlaybackProgress(positionMs: Int, episodeUuid: String?) {
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
                Timber.d("FingerprintTimingManager: playback jumped ${deltaMs}ms — restarting")
                val matcher = currentMatcher
                val audioPath = currentAudioFilePath
                val uuid = currentEpisodeUuid
                if (matcher != null && audioPath != null && uuid != null) {
                    filterLastTrusted = null
                    filterCandidatePool.clear()
                    startStream(audioPath, matcher, uuid, startPosition = positionMs / 1000.0)
                }
                lastProgressPositionMs = positionMs
                return
            }
        }
        lastProgressPositionMs = positionMs

        if (!isWithinMappedRange(positionMs / 1000.0) && playbackToReference.isNotEmpty()) {
            Timber.d("FingerprintTimingManager: playback outside mapped range — restarting")
            val matcher = currentMatcher
            val audioPath = currentAudioFilePath
            val uuid = currentEpisodeUuid
            if (matcher != null && audioPath != null && uuid != null) {
                filterLastTrusted = null
                filterCandidatePool.clear()
                startStream(audioPath, matcher, uuid, startPosition = positionMs / 1000.0)
            }
        }
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

    // region Streaming Fingerprint Generation

    private fun startStream(audioFilePath: String, matcher: CheckpointMatcher, episodeUuid: String, startPosition: Double) {
        generationJob?.cancel()
        generationJob = scope.launch {
            val aligned = alignToWindowGrid(startPosition)
            try {
                streamFingerprint(audioFilePath, matcher, episodeUuid, startingAt = aligned)
                persistMappingCacheIfFull()
            } catch (_: CancellationException) {
                Timber.d("FingerprintTimingManager: streaming fingerprint cancelled")
            } catch (e: Exception) {
                Timber.w(e, "FingerprintTimingManager: streaming fingerprint failed")
                if (state !is State.Active) {
                    _stateFlow.value = State.Failed(e)
                }
            }
        }
    }

    private suspend fun streamFingerprint(
        audioFilePath: String,
        matcher: CheckpointMatcher,
        episodeUuid: String,
        startingAt: Double,
    ) {
        val isRemoteUrl = audioFilePath.startsWith("http://") || audioFilePath.startsWith("https://")
        if (!isRemoteUrl && !File(audioFilePath).exists()) {
            Timber.w("FingerprintTimingManager: audio file not found at $audioFilePath")
            _stateFlow.value = State.Unavailable
            return
        }

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(audioFilePath)
        } catch (e: Exception) {
            Timber.w(e, "FingerprintTimingManager: failed to open audio file")
            _stateFlow.value = State.Failed(e)
            return
        }

        // Find audio track
        val audioTrackIndex = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
        if (audioTrackIndex == null) {
            extractor.release()
            _stateFlow.value = State.Unavailable
            Timber.w("FingerprintTimingManager: no audio track found")
            return
        }

        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: "audio/mpeg"

        // Seek to start position
        if (startingAt > 0) {
            extractor.seekTo((startingAt * 1_000_000).toLong(), MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        }

        val codec = MediaCodec.createDecoderByType(mime)

        // Request float PCM output. If the codec doesn't support it,
        // it falls back to 16-bit; we detect the actual format below.
        format.setInteger(MediaFormat.KEY_PCM_ENCODING, android.media.AudioFormat.ENCODING_PCM_FLOAT)
        codec.configure(format, null, null, 0)
        codec.start()

        val streamer = StreamingWindowedFingerprinter(
            sampleRate.toUInt(),
            channelCount.toUShort(),
            FingerprintConstants.WINDOW_DURATION_MS.toUInt(),
            FingerprintConstants.WINDOW_INTERVAL_MS.toUInt(),
        )

        try {
            decodeAndFingerprint(extractor, codec, streamer, matcher, episodeUuid, startingAt, sampleRate, channelCount)

            // Flush remaining windows
            val tail = streamer.flush()
            if (tail.isNotEmpty()) {
                mutex.withLock {
                    processMatches(tail, matcher, startingAt)
                }
            }
        } finally {
            streamer.close()
            codec.stop()
            codec.release()
            extractor.release()
        }
    }

    private suspend fun decodeAndFingerprint(
        extractor: MediaExtractor,
        codec: MediaCodec,
        streamer: StreamingWindowedFingerprinter,
        matcher: CheckpointMatcher,
        episodeUuid: String,
        startOffset: Double,
        sampleRate: Int,
        channelCount: Int,
    ) {
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        val timeoutUs = 10_000L
        // Assume float output (what we requested). Updated if the codec
        // reports a different format via INFO_OUTPUT_FORMAT_CHANGED.
        var isOutputFloat = true

        while (true) {
            currentCoroutineContext().ensureActive()

            // Feed input
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)!!
                    val bytesRead = extractor.readSampleData(inputBuffer, 0)
                    if (bytesRead < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, bytesRead, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            // Read decoded output
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val actualFormat = codec.outputFormat
                    val encoding = if (actualFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        actualFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    } else {
                        android.media.AudioFormat.ENCODING_PCM_16BIT
                    }
                    isOutputFloat = encoding == android.media.AudioFormat.ENCODING_PCM_FLOAT
                    Timber.d("FingerprintTimingManager: codec output encoding=${if (isOutputFloat) "float" else "int16"}")
                }

                outputIndex >= 0 -> {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        codec.releaseOutputBuffer(outputIndex, false)
                        break
                    }

                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val samples = extractFloatSamples(outputBuffer, bufferInfo, isOutputFloat)
                        if (samples.isNotEmpty()) {
                            val windows = streamer.pushSamplesF32(samples, channelCount.toUShort())
                            if (windows.isNotEmpty()) {
                                mutex.withLock {
                                    processMatches(windows, matcher, startOffset)
                                }
                            }
                        }

                        // Throttle if we're far ahead of playback
                        val decodedSeconds = startOffset + streamer.durationMs().toDouble() / 1000.0
                        val currentPlayback = playbackManager.playbackStateRelay.blockingFirst().positionMs / 1000.0
                        if (decodedSeconds - currentPlayback > FingerprintConstants.LOOKAHEAD_SECONDS) {
                            delay((FingerprintConstants.OUTSIDE_LOOKAHEAD_SLEEP_SECONDS * 1000).toLong())
                        }
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                }
            }
        }
    }

    private fun extractFloatSamples(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        isFloatPcm: Boolean,
    ): List<Float> {
        buffer.position(info.offset)
        buffer.limit(info.offset + info.size)

        val byteOrder = buffer.order()
        buffer.order(ByteOrder.nativeOrder())

        return try {
            if (isFloatPcm) {
                val floatBuffer = buffer.asFloatBuffer()
                val floatCount = floatBuffer.remaining()
                if (floatCount == 0) return emptyList()
                val result = FloatArray(floatCount)
                floatBuffer.get(result)
                result.toList()
            } else {
                // 16-bit PCM: convert to normalized float [-1.0, 1.0]
                val shortBuffer = buffer.asShortBuffer()
                val shortCount = shortBuffer.remaining()
                if (shortCount == 0) return emptyList()
                val result = FloatArray(shortCount)
                for (i in 0 until shortCount) {
                    result[i] = shortBuffer.get() / 32768f
                }
                result.toList()
            }
        } finally {
            buffer.order(byteOrder)
        }
    }

    private fun processMatches(
        windows: List<WindowedFingerprint>,
        matcher: CheckpointMatcher,
        startOffset: Double,
    ) {
        var inserted = 0

        for (window in windows) {
            val matches = matcher.findTopMatches(window.hashes, 2u)
            val best = matches.firstOrNull() ?: continue

            if (best.score < FingerprintConstants.MATCH_SCORE_THRESHOLD) continue

            val absolutePlaybackTime = startOffset + window.timestampMs.toDouble() / 1000.0
            val candidate = TimeMappingEntry(
                playbackTime = absolutePlaybackTime,
                referenceTime = best.timestamp.toDouble(),
                score = best.score,
            )

            if (best.score < FingerprintConstants.DRIFT_ANCHOR_SCORE_THRESHOLD) continue

            val runnerUpScore = matches.getOrNull(1)?.score ?: 0f
            val dominance = best.score - runnerUpScore
            if (dominance < FingerprintConstants.DRIFT_SCORE_DOMINANCE_GAP) continue

            inserted += consider(candidate)
        }

        val coverage = playbackToReference.size
        if (coverage >= FingerprintConstants.MINIMUM_COVERAGE_FOR_ACTIVE) {
            _stateFlow.value = State.Active(coverage)
            if (!hasReachedActive) {
                hasReachedActive = true
                Timber.d("FingerprintTimingManager: reached active state with $coverage mappings")
            }
        }
    }

    private fun persistMappingCacheIfFull() {
        val audioPath = currentAudioFilePath ?: return
        if (audioPath.startsWith("http://") || audioPath.startsWith("https://")) return
        val refPath = currentReferenceFilePath ?: return
        val refData = currentReferenceData ?: return
        val refDuration = currentReferenceDuration

        val snapshot = playbackToReference.toList()
        FingerprintMappingCache.save(snapshot, audioPath, refPath, refData, refDuration)
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
