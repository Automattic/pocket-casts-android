package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import androidx.media3.common.C
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.BuildConfig
import au.com.shiftyjelly.pocketcasts.repositories.playback.ExoPlayerDataSourceFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SyncedTranscriptsPreparationCompletedEvent
import com.automattic.eventhorizon.SyncedTranscriptsPreparationFailedEvent
import com.automattic.eventhorizon.SyncedTranscriptsPreparationStartedEvent
import com.automattic.eventhorizon.SyncedTranscriptsUnavailableEvent
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import uniffi.fingerprint_uniffi.CheckpointMatcher
import uniffi.fingerprint_uniffi.StreamingWindowedFingerprinter
import uniffi.fingerprint_uniffi.WindowedFingerprint

@Singleton
class FingerprintTimingManager @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val referenceRetriever: FingerprintReferenceRetriever,
    private val eventHorizon: EventHorizon,
    @ApplicationContext private val context: Context,
    private val chapterManager: Lazy<ChapterManager>,
    private val settings: Settings,
    private val dataSourceFactory: Lazy<ExoPlayerDataSourceFactory>,
    private val pcmTap: FingerprintPcmTap,
) {

    /** Who asked for preparation; decides whether streaming over a metered network is acceptable. */
    enum class PrepareTrigger {
        PLAYBACK,
        TRANSCRIPT_VIEW,
        BOOKMARK,
    }

    sealed interface State {
        data object Idle : State
        data object Preparing : State
        data class Active(val coverage: Int) : State
        data class Failed(val error: Throwable, val episodeUuid: String?) : State
        data class Unavailable(val episodeUuid: String?) : State
    }

    data class TimeMappingEntry(
        val playbackTime: Double,
        val referenceTime: Double,
        val score: Float = 0f,
    )

    data class DebugRejection(
        val playbackTime: Double,
        val score: Float?,
    )

    internal data class SearchWindow(
        val startSec: Double,
        val endSec: Double,
    )

    private val _stateFlow = MutableStateFlow<State>(State.Idle)
    val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()
    val state: State get() = _stateFlow.value
    val mappingSnapshot: List<TimeMappingEntry> get() = snapshotPlaybackToReference

    // Monotonic counter bumped on every snapshot publish, so consumers can react to mapping growth.
    private val _mappingVersion = MutableStateFlow(0L)
    val mappingVersion: StateFlow<Long> = _mappingVersion.asStateFlow()

    /** Episode the current mapping belongs to, or null when idle. Safe to read off the UI thread. */
    @Volatile
    var activeEpisodeUuid: String? = null
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val decodeDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val mutex = Mutex()
    private val activeResolves = AtomicInteger(0)

    // Mapping state: writers mutate the accumulator under mutex, then atomically publish via @Volatile.
    // Readers access the snapshot without locking, safe for use from the UI thread.
    private val mapping = MappingAccumulator()

    @Volatile
    private var snapshotPlaybackToReference: List<TimeMappingEntry> = emptyList()

    @Volatile
    private var snapshotReferenceToPlayback: List<TimeMappingEntry> = emptyList()

    // Debug rejections: accumulated across seek-restarts, cleared on stop() or episode change.
    private var debugRejections = mutableListOf<DebugRejection>()

    @Volatile
    var debugRejectionsSnapshot: List<DebugRejection> = emptyList()
        private set

    private var generationJob: Job? = null
    private var downloadObserverJob: Job? = null

    @Volatile
    private var generation = 0L

    @VisibleForTesting
    internal var debugTrackingEnabled = false

    // Context for current preparation
    private var currentTrigger = PrepareTrigger.PLAYBACK

    @Volatile
    private var currentEpisodeUuid: String? = null
    private var currentAudioFilePath: String? = null
    private var currentSharedCacheKey: String? = null
    private var currentDurationSec: Double = 0.0
    private var currentReferenceData: ByteArray? = null
    private var currentReferenceFilePath: String? = null
    private var currentReference: ReferenceFingerprint? = null
    private var currentMatcher: CheckpointMatcher? = null
    private var hasReachedActive = false
    private var hasTrackedFailure = false

    // Analytics context for the current preparation.
    private var preparationStartMs: Long = 0

    @Volatile
    private var currentIsStreaming: Boolean = false

    // When true, decode the whole file locally from the start instead of following the PCM tap,
    // so the full reference<->playback map is available for chapter alignment.
    @Volatile
    private var currentEager: Boolean = false

    init {
        observePlaybackForProactivePreparation()
    }

    private fun observePlaybackForProactivePreparation() {
        scope.launch {
            var lastEpisodeUuid: String? = null
            playbackManager.playbackStateFlow.collect { state ->
                if (!FeatureFlag.isEnabled(Feature.SYNCED_TRANSCRIPTS)) return@collect
                val uuid = state.episodeUuid
                if (state.isPlaying && !uuid.isNullOrEmpty() && uuid != lastEpisodeUuid) {
                    lastEpisodeUuid = uuid
                    prepareForCurrentEpisode(PrepareTrigger.PLAYBACK)
                } else if (lastEpisodeUuid != null && (state.isEmpty || state.isStopped)) {
                    lastEpisodeUuid = null
                    stop()
                }
            }
        }
    }

    fun prepareForCurrentEpisode(trigger: PrepareTrigger) {
        val episode = playbackManager.getCurrentEpisode() ?: run {
            // No episode is currently loaded, so there is no episode UUID to attribute this to.
            markUnavailable(reason = "no_episode", episodeUuid = null)
            return
        }

        val episodeUuid = episode.uuid
        val podcastUuid = episode.podcastOrSubstituteUuid
        // Fingerprint the progressive enclosure, not the HLS rendition; timings may drift if the renditions differ.
        val audioSource = episode.downloadedFilePath ?: episode.downloadUrl
        // Reuse the player's on-disk cache (same UUID key) instead of a second download when it applies.
        val sharedCacheKey = episodeUuid.takeIf {
            !episode.isDownloaded && !episode.isDownloading && !episode.isStreamUrlHls && settings.cacheEntirePlayingEpisode.value
        }

        scope.launch {
            val gen: Long
            mutex.withLock {
                // Already prepared for this episode — reuse existing state. A failed preparation may
                // retry when the user opens the transcript, since the failure could be transient.
                val retryableFailure = trigger == PrepareTrigger.TRANSCRIPT_VIEW && state is State.Failed
                if (currentEpisodeUuid == episodeUuid && state !is State.Idle && !retryableFailure) {
                    if (trigger == PrepareTrigger.TRANSCRIPT_VIEW) {
                        currentTrigger = trigger
                    }
                    return@launch
                }
                // The tap builds the map progressively; keep whatever the last episode accumulated.
                persistMappingCacheIfFull()
                resetState()
                generation++
                gen = generation
            }
            // Abort if a newer stop()/prepare() has started since we acquired the lock.
            if (gen != generation) return@launch
            startDownloadCompletionObserver(episodeUuid)
            prepareForEpisode(gen, episodeUuid, podcastUuid, audioSource, sharedCacheKey, episode.isDownloaded, episode.duration, trigger)
        }
    }

    private fun startDownloadCompletionObserver(episodeUuid: String) {
        downloadObserverJob?.cancel()
        downloadObserverJob = scope.launch {
            playbackManager.playbackStateFlow.collect { state ->
                if (state.episodeUuid == episodeUuid && state.isPlaying) {
                    maybeAdoptDownloadedCopy(episodeUuid)
                }
            }
        }
    }

    /** A finished download supersedes streaming: local decode is cheaper and unlocks the eager pass. */
    private fun maybeAdoptDownloadedCopy(episodeUuid: String) {
        if (!currentIsStreaming) return
        val episode = playbackManager.getCurrentEpisode() ?: return
        if (episode.uuid != episodeUuid || !episode.isDownloaded) return
        scope.launch {
            val trigger = currentTrigger
            mutex.withLock {
                if (currentEpisodeUuid != episodeUuid || !currentIsStreaming) return@launch
                generation++
                resetState()
                _stateFlow.value = State.Idle
            }
            Timber.d("FingerprintTimingManager: episode finished downloading — switching to local file")
            prepareForCurrentEpisode(trigger)
        }
    }

    fun stop() {
        scope.launch {
            mutex.withLock {
                generation++
                persistMappingCacheIfFull()
                resetState()
                _stateFlow.value = State.Idle
            }
        }
        Timber.d("FingerprintTimingManager: stopped")
    }

    fun onTranscriptShown(episodeUuid: String) {
        if (playbackManager.getCurrentEpisode()?.uuid != episodeUuid) return
        prepareForCurrentEpisode(PrepareTrigger.TRANSCRIPT_VIEW)
    }

    fun onTranscriptDismissed(episodeUuid: String) {
        scope.launch {
            mutex.withLock {
                if (currentEpisodeUuid == episodeUuid && currentTrigger == PrepareTrigger.TRANSCRIPT_VIEW) {
                    currentTrigger = PrepareTrigger.PLAYBACK
                }
            }
        }
    }

    /**
     * Playback time for [referenceTime] when the active mapping is dense around it, so a full
     * on-demand resolve is unnecessary. Null when the mapping is missing, sparse, or another episode's.
     */
    fun densePlaybackTime(episodeUuid: String, referenceTime: Duration): Duration? {
        if (activeEpisodeUuid != episodeUuid) return null
        val playback = densePlaybackSec(referenceTime.toDouble(DurationUnit.SECONDS), snapshotReferenceToPlayback) ?: return null
        return playback.seconds
    }

    /**
     * One-shot bounded resolve of a generated chapter's reference time to the playback timeline.
     * Decodes only a small search window around the expected location, matches into a scratch
     * accumulator, and never touches the continuous mapping or the public state.
     */
    suspend fun resolvePlaybackTime(episode: BaseEpisode, referenceTime: Duration): ChapterSeekResult {
        val audioSource = episode.downloadedFilePath
            ?: episode.downloadUrl
            ?: return ChapterSeekResult.Unresolved(ChapterSeekResult.REASON_NO_AUDIO_SOURCE)
        // Resolves block on codec dequeues and network reads, so keep them off the Default pool.
        return withContext(Dispatchers.IO) {
            performResolve(
                episodeUuid = episode.uuid,
                podcastUuid = episode.podcastOrSubstituteUuid,
                audioSource = audioSource,
                isDownloaded = episode.isDownloaded,
                referenceTimeSec = referenceTime.toDouble(DurationUnit.SECONDS),
            )
        }
    }

    private suspend fun performResolve(
        episodeUuid: String,
        podcastUuid: String,
        audioSource: String,
        isDownloaded: Boolean,
        referenceTimeSec: Double,
    ): ChapterSeekResult {
        val blocked = shouldBlockOnDemandResolve(
            isDownloaded = isDownloaded,
            warnOnMeteredNetwork = settings.warnOnMeteredNetwork.value,
            isUnmetered = { Network.isUnmeteredConnection(context) },
        )
        if (blocked) {
            Timber.d("FingerprintTimingManager: skipping on-demand resolve on metered network")
            return ChapterSeekResult.Unresolved(ChapterSeekResult.REASON_METERED_NETWORK)
        }
        var estimatedPlayback: Double? = null
        var warmReference: ReferenceFingerprint? = null
        var sharedCacheKey: String? = null
        mutex.withLock {
            if (currentEpisodeUuid == episodeUuid) {
                warmReference = currentReference
                sharedCacheKey = currentSharedCacheKey
                estimatedPlayback = interpolate(
                    time = referenceTimeSec,
                    entries = snapshotReferenceToPlayback,
                    keySelector = { it.referenceTime },
                    valueSelector = { it.playbackTime },
                )
            }
        }
        val usedPrior = estimatedPlayback != null

        // The fetch and the decode get separate budgets, so a slow reference download can't eat the decode time.
        val reference = warmReference ?: run {
            val referenceData = withTimeoutOrNull(FingerprintConstants.ON_DEMAND_TIMEOUT_MS) {
                loadOrFetchReferenceData(podcastUuid, episodeUuid, audioSource, isDownloaded)
            } ?: return ChapterSeekResult.Unresolved(ChapterSeekResult.REASON_NO_REFERENCE)
            ReferenceFingerprint.decode(referenceData)
                ?: return ChapterSeekResult.Unresolved(ChapterSeekResult.REASON_NO_REFERENCE)
        }
        val matcher = buildMatcher(reference)
            ?: return ChapterSeekResult.Unresolved(ChapterSeekResult.REASON_NO_REFERENCE)

        val window = searchWindow(referenceTimeSec, estimatedPlayback)
        val acc = MappingAccumulator()
        activeResolves.incrementAndGet()
        val timedOut = try {
            matcher.use {
                withTimeoutOrNull(FingerprintConstants.ON_DEMAND_TIMEOUT_MS) {
                    streamFingerprintBounded(
                        audioFilePath = audioSource,
                        matcher = it,
                        startingAt = alignToWindowGrid(window.startSec),
                        endingAt = window.endSec,
                        targetReferenceSec = referenceTimeSec,
                        acc = acc,
                        cacheKey = sharedCacheKey,
                    )
                } == null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "FingerprintTimingManager: on-demand resolve failed for $episodeUuid")
            return ChapterSeekResult.Unresolved(ChapterSeekResult.REASON_AUDIO_UNAVAILABLE)
        } finally {
            activeResolves.decrementAndGet()
        }

        // A timed-out decode may still have committed usable anchors; interpolate from what we have.
        if (acc.playbackToReference.size < FingerprintConstants.ON_DEMAND_MIN_ANCHORS) {
            return ChapterSeekResult.Unresolved(
                if (timedOut) ChapterSeekResult.REASON_TIMEOUT else ChapterSeekResult.REASON_NO_MATCH,
            )
        }
        val playback = interpolate(
            time = referenceTimeSec,
            entries = acc.referenceToPlayback,
            keySelector = { it.referenceTime },
            valueSelector = { it.playbackTime },
        ) ?: return ChapterSeekResult.Unresolved(ChapterSeekResult.REASON_NO_MATCH)

        mergeResolvedAnchors(episodeUuid, acc.playbackToReference)

        // The ad offset is non-negative, so the true position is never before the reference time.
        return ChapterSeekResult.Resolved(
            playbackTime = max(referenceTimeSec, playback).seconds,
            usedPrior = usedPrior,
        )
    }

    /** Resolved anchors passed the same drift filter, so folding them in only densifies the map. */
    private suspend fun mergeResolvedAnchors(episodeUuid: String, anchors: List<TimeMappingEntry>) {
        val tolerance = FingerprintConstants.WINDOW_INTERVAL_MS / 2000.0
        mutex.withLock {
            if (currentEpisodeUuid != episodeUuid) return
            var inserted = 0
            for (anchor in anchors) {
                if (!mapping.hasAnchorNear(anchor.playbackTime, tolerance)) {
                    mapping.insert(anchor)
                    inserted++
                }
            }
            if (inserted > 0) {
                publishSnapshot()
                Timber.d("FingerprintTimingManager: merged $inserted resolved anchors into the mapping")
            }
        }
    }

    private suspend fun loadOrFetchReferenceData(
        podcastUuid: String,
        episodeUuid: String,
        audioSource: String,
        isDownloaded: Boolean,
    ): ByteArray? {
        if (isDownloaded) {
            referenceRetriever.loadReferenceData(audioSource)?.let { return it }
            referenceRetriever.loadCachedReference(episodeUuid)?.let {
                referenceRetriever.saveReferenceData(it, audioSource)
                return it
            }
        } else {
            referenceRetriever.loadCachedReference(episodeUuid)?.let { return it }
        }
        val baseUrl = "${BuildConfig.SERVER_SHOW_NOTES_URLS}/generated_transcripts/"
        val fetched = referenceRetriever.fetchReferenceData(baseUrl, podcastUuid, episodeUuid)
        val data = (fetched as? FingerprintReferenceRetriever.FetchResult.Success)?.data ?: return null
        if (isDownloaded) {
            referenceRetriever.saveReferenceData(data, audioSource)
        } else {
            referenceRetriever.saveCachedReference(episodeUuid, data)
        }
        return data
    }

    private fun buildMatcher(reference: ReferenceFingerprint): CheckpointMatcher? {
        val checkpoints = reference.libraryCheckpoints()
        if (checkpoints.isEmpty()) return null
        val matcher = CheckpointMatcher()
        for (checkpoint in checkpoints) {
            matcher.add(
                checkpoint.timestampSeconds,
                checkpoint.hashes.map { it.toUInt() },
                reference.checkpointDurationSeconds,
            )
        }
        return matcher
    }

    private suspend fun streamFingerprintBounded(
        audioFilePath: String,
        matcher: CheckpointMatcher,
        startingAt: Double,
        endingAt: Double,
        targetReferenceSec: Double,
        acc: MappingAccumulator,
        cacheKey: String?,
    ) {
        // Capture the current generation so the decode aborts if the episode changes mid-resolve.
        val gen = generation
        val callerJob = currentCoroutineContext().job
        val isRemoteUrl = audioFilePath.startsWith("http://") || audioFilePath.startsWith("https://")
        openAudioStream(gen, audioFilePath, startingAt, cacheKey, followPlayerCache = false, isCallerActive = { callerJob.isActive }).use { stream ->
            stream.start()

            val streamer = StreamingWindowedFingerprinter(
                stream.sampleRate.toUInt(),
                stream.channelCount.toUShort(),
                FingerprintConstants.WINDOW_DURATION_MS.toUInt(),
                FingerprintConstants.WINDOW_INTERVAL_MS.toUInt(),
            )
            try {
                decodeAndFingerprint(
                    stream = stream,
                    gen = gen,
                    streamer = streamer,
                    startOffset = startingAt,
                    endingAt = endingAt,
                    stopWhen = { isResolveTargetCovered(acc, targetReferenceSec) },
                ) { windows ->
                    matchWindows(windows, matcher, startingAt, acc)
                }
                val tail = streamer.flush()
                if (tail.isNotEmpty()) {
                    matchWindows(tail, matcher, startingAt, acc)
                }
            } finally {
                streamer.close()
            }
        }
    }

    private fun elapsedPreparationMs(): Long = if (preparationStartMs == 0L) 0L else SystemClock.elapsedRealtime() - preparationStartMs

    private fun markUnavailable(
        reason: String,
        isStreaming: Boolean? = null,
        episodeUuid: String? = currentEpisodeUuid,
    ) {
        _stateFlow.value = State.Unavailable(episodeUuid)
        eventHorizon.track(
            SyncedTranscriptsUnavailableEvent(
                reason = reason,
                isStreaming = isStreaming,
                episodeUuid = episodeUuid ?: AnalyticsTracker.INVALID_OR_NULL_VALUE,
            ),
        )
    }

    private fun markFailed(error: Throwable, stage: String) {
        _stateFlow.value = State.Failed(error, currentEpisodeUuid)
        if (hasTrackedFailure) return
        hasTrackedFailure = true
        eventHorizon.track(
            SyncedTranscriptsPreparationFailedEvent(
                errorCode = 0,
                errorDomain = error.javaClass.name,
                stage = stage,
                durationMs = elapsedPreparationMs(),
                episodeUuid = currentEpisodeUuid ?: AnalyticsTracker.INVALID_OR_NULL_VALUE,
            ),
        )
    }

    private fun markActive(coverage: Int) {
        _stateFlow.value = State.Active(coverage)
        if (!hasReachedActive) {
            hasReachedActive = true
            Timber.d("FingerprintTimingManager: reached active state with $coverage mappings")
            eventHorizon.track(
                SyncedTranscriptsPreparationCompletedEvent(
                    durationMs = elapsedPreparationMs(),
                    isStreaming = currentIsStreaming,
                    episodeUuid = currentEpisodeUuid ?: AnalyticsTracker.INVALID_OR_NULL_VALUE,
                ),
            )
        }
    }

    fun referenceTime(forPlaybackTimeMs: Int): Double? {
        val playbackTimeSec = forPlaybackTimeMs / 1000.0
        return interpolate(
            time = playbackTimeSec,
            entries = snapshotPlaybackToReference,
            keySelector = { it.playbackTime },
            valueSelector = { it.referenceTime },
        )
    }

    fun playbackTimeMs(forReferenceTime: Double): Int? {
        val result = interpolate(
            time = forReferenceTime,
            entries = snapshotReferenceToPlayback,
            keySelector = { it.referenceTime },
            valueSelector = { it.playbackTime },
        ) ?: return null
        return (result * 1000).toInt()
    }

    @Volatile
    private var lastMatchedContentResult = true

    /** Reference time gated on matched content. Returns null during ads or unmapped regions. */
    fun matchedReferenceTime(forPlaybackTimeMs: Int): Double? {
        val playbackTimeSec = forPlaybackTimeMs / 1000.0
        val entries = snapshotPlaybackToReference
        if (!isWithinMatchedContent(playbackTimeSec, entries)) {
            if (lastMatchedContentResult) {
                lastMatchedContentResult = false
                logMatchedContentTransition(playbackTimeSec, entries, matched = false)
            }
            return null
        }
        if (!lastMatchedContentResult) {
            lastMatchedContentResult = true
            logMatchedContentTransition(playbackTimeSec, entries, matched = true)
        }
        return interpolate(
            time = playbackTimeSec,
            entries = entries,
            keySelector = { it.playbackTime },
            valueSelector = { it.referenceTime },
        )
    }

    private fun logMatchedContentTransition(playbackTime: Double, entries: List<TimeMappingEntry>, matched: Boolean) {
        // Binary search to find the bracketing entries for context
        var lo = 0
        var hi = entries.size
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (entries[mid].playbackTime <= playbackTime) lo = mid + 1 else hi = mid
        }
        val before = if (hi > 0) entries[hi - 1].playbackTime else null
        val after = if (hi < entries.size) entries[hi].playbackTime else null
        val gap = if (before != null && after != null) after - before else null
        Timber.d(
            "FingerprintTimingManager: highlight %s at %.1fs — before=%.1f after=%.1f gap=%.1f entries=%d",
            if (matched) "RESUMED" else "SUPPRESSED",
            playbackTime,
            before ?: -1.0,
            after ?: -1.0,
            gap ?: -1.0,
            entries.size,
        )
    }

    private fun resetState() {
        generationJob?.cancel()
        generationJob = null
        downloadObserverJob?.cancel()
        downloadObserverJob = null
        mapping.reset()
        debugRejections.clear()
        debugRejectionsSnapshot = emptyList()
        publishSnapshot()
        currentEpisodeUuid = null
        currentAudioFilePath = null
        currentSharedCacheKey = null
        currentDurationSec = 0.0
        currentReferenceData = null
        currentReferenceFilePath = null
        currentReference = null
        currentMatcher?.close()
        currentMatcher = null
        activeEpisodeUuid = null
        hasReachedActive = false
        hasTrackedFailure = false
        preparationStartMs = 0
        currentIsStreaming = false
        currentEager = false
        currentTrigger = PrepareTrigger.PLAYBACK
    }

    /**
     * Eager full-file mapping only makes sense on mobile, for episodes with generated chapters,
     * and only for local downloads. Everything else builds the map from the player's PCM tap
     * plus the bounded on-demand resolver, so nothing is ever downloaded twice.
     */
    private suspend fun shouldRunEagerPass(episodeUuid: String, isDownloaded: Boolean): Boolean {
        if (Util.getAppPlatform(context) != AppPlatform.Phone) return false
        return computeEager(
            hasGeneratedChapters = chapterManager.get().hasGeneratedChapters(episodeUuid),
            isDownloaded = isDownloaded,
        )
    }

    private suspend fun prepareForEpisode(
        gen: Long,
        episodeUuid: String,
        podcastUuid: String,
        audioSource: String?,
        sharedCacheKey: String?,
        isDownloaded: Boolean,
        durationSec: Double,
        trigger: PrepareTrigger,
    ) {
        if (!FeatureFlag.isEnabled(Feature.SYNCED_TRANSCRIPTS)) {
            markUnavailable(reason = "feature_disabled", isStreaming = !isDownloaded, episodeUuid = episodeUuid)
            Timber.d("FingerprintTimingManager: feature disabled")
            return
        }

        if (Util.getAppPlatform(context) != AppPlatform.Phone) {
            markUnavailable(reason = "unsupported_platform", isStreaming = !isDownloaded, episodeUuid = episodeUuid)
            Timber.d("FingerprintTimingManager: unsupported platform")
            return
        }

        if (durationSec <= 0) {
            markUnavailable(reason = "invalid_duration", isStreaming = !isDownloaded, episodeUuid = episodeUuid)
            Timber.d("FingerprintTimingManager: invalid duration for $episodeUuid")
            return
        }

        if (audioSource == null) {
            // Neither downloaded nor streamable, so isStreaming would be misleading here.
            markUnavailable(reason = "no_audio_source", episodeUuid = episodeUuid)
            Timber.d("FingerprintTimingManager: no audio source for $episodeUuid")
            return
        }

        val eager = shouldRunEagerPass(episodeUuid, isDownloaded)
        mutex.withLock {
            if (gen != generation) return
            currentTrigger = trigger
            currentEpisodeUuid = episodeUuid
            activeEpisodeUuid = episodeUuid
            currentAudioFilePath = audioSource
            currentSharedCacheKey = sharedCacheKey
            currentDurationSec = durationSec
            currentIsStreaming = !isDownloaded
            currentEager = eager
            preparationStartMs = SystemClock.elapsedRealtime()
        }
        eventHorizon.track(
            SyncedTranscriptsPreparationStartedEvent(
                episodeDurationSeconds = durationSec.toLong(),
                isStreaming = !isDownloaded,
                episodeUuid = episodeUuid,
            ),
        )

        // Try loading cached reference from disk. A reference cached while streaming is still valid
        // after a download; adopt it into the sidecar instead of refetching.
        val cachedData = if (isDownloaded) {
            referenceRetriever.loadReferenceData(audioSource)
                ?: referenceRetriever.loadCachedReference(episodeUuid)?.also { referenceRetriever.saveReferenceData(it, audioSource) }
        } else {
            referenceRetriever.loadCachedReference(episodeUuid)
        }
        if (cachedData != null) {
            val reference = ReferenceFingerprint.decode(cachedData)
            if (reference != null) {
                configureForReference(gen, reference, cachedData, audioSource, episodeUuid)
                return
            }
        }

        // Fetch from server
        _stateFlow.value = State.Preparing
        Timber.d("FingerprintTimingManager: fetching reference from server for $episodeUuid")

        val baseUrl = "${BuildConfig.SERVER_SHOW_NOTES_URLS}/generated_transcripts/"
        val fetchResult = referenceRetriever.fetchReferenceData(baseUrl, podcastUuid, episodeUuid)
        if (gen != generation) return

        val referenceData = when (fetchResult) {
            is FingerprintReferenceRetriever.FetchResult.Success -> fetchResult.data

            is FingerprintReferenceRetriever.FetchResult.NotFound -> {
                markUnavailable(reason = "no_reference", isStreaming = !isDownloaded, episodeUuid = episodeUuid)
                Timber.d("FingerprintTimingManager: no reference available for $episodeUuid")
                return
            }

            // A transient fetch failure marks Failed rather than Unavailable, so reopening the
            // transcript can retry it.
            is FingerprintReferenceRetriever.FetchResult.Error -> {
                markFailed(IOException("reference fetch failed"), stage = "reference_fetch")
                Timber.d("FingerprintTimingManager: reference fetch failed for $episodeUuid")
                return
            }
        }

        val reference = ReferenceFingerprint.decode(referenceData)
        if (reference == null) {
            markUnavailable(reason = "reference_decode_failed", isStreaming = !isDownloaded, episodeUuid = episodeUuid)
            Timber.d("FingerprintTimingManager: failed to decode reference for $episodeUuid")
            return
        }

        if (isDownloaded) {
            referenceRetriever.saveReferenceData(referenceData, audioSource)
        } else {
            referenceRetriever.saveCachedReference(episodeUuid, referenceData)
        }
        configureForReference(gen, reference, referenceData, audioSource, episodeUuid)
    }

    private suspend fun configureForReference(
        gen: Long,
        reference: ReferenceFingerprint,
        referenceData: ByteArray,
        audioFilePath: String,
        episodeUuid: String,
    ) {
        val matcher = buildMatcher(reference)
        if (matcher == null) {
            markUnavailable(reason = "no_checkpoints", isStreaming = currentIsStreaming, episodeUuid = episodeUuid)
            Timber.d("FingerprintTimingManager: no usable checkpoints for $episodeUuid")
            return
        }

        Timber.d(
            "FingerprintTimingManager: reference for $episodeUuid — " +
                "totalDuration=${reference.totalDuration}s, " +
                "${matcher.count()} checkpoints",
        )

        // Try loading mapping cache (only for local files)
        val refPath = FingerprintReferenceRetriever.referencePath(audioFilePath)
        val isLocalFile = !audioFilePath.startsWith("http://") && !audioFilePath.startsWith("https://")
        val cached = if (isLocalFile) FingerprintMappingCache.load(audioFilePath, refPath, referenceData) else null

        mutex.withLock {
            if (gen != generation) {
                matcher.close()
                return
            }
            currentReferenceData = referenceData
            currentReferenceFilePath = refPath
            currentReference = reference
            currentMatcher = matcher
            _stateFlow.value = State.Preparing
            if (cached != null) {
                mapping.replaceAll(cached.entries)
                publishSnapshot()
                mapping.lastTrusted = cached.entries.lastOrNull()
                markActive(cached.entries.size)
                Timber.d("FingerprintTimingManager: loaded mapping from cache for $episodeUuid (${cached.entries.size} entries)")
                return
            }
        }

        // A downloaded episode with generated chapters gets a full local decode so the whole map is
        // available for chapter alignment; everything else builds the map from the player's PCM tap.
        mutex.withLock {
            if (gen != generation) return
            if (currentEager) {
                startEagerLocalDecode(gen, audioFilePath, matcher, episodeUuid)
            } else {
                startTapCollection(gen, matcher)
            }
        }
    }

    /** Must be called under [mutex]. */
    private fun startEagerLocalDecode(gen: Long, audioFilePath: String, matcher: CheckpointMatcher, episodeUuid: String) {
        generationJob?.cancel()
        generationJob = scope.launch(decodeDispatcher) {
            try {
                streamFingerprint(gen, audioFilePath, matcher, episodeUuid, startingAt = 0.0)
                if (gen == generation) {
                    persistMappingCacheIfFull()
                }
            } catch (_: CancellationException) {
                Timber.d("FingerprintTimingManager: eager fingerprint decode cancelled")
            } catch (e: Exception) {
                Timber.w(e, "FingerprintTimingManager: eager fingerprint decode failed")
                if (gen == generation && state !is State.Active) {
                    markFailed(e, stage = "fingerprint_stream")
                }
            }
        }
    }

    /** Must be called under [mutex]. */
    private fun startTapCollection(gen: Long, matcher: CheckpointMatcher) {
        generationJob?.cancel()
        generationJob = scope.launch(decodeDispatcher) {
            var streamer: StreamingWindowedFingerprinter? = null
            var streamStartSec = 0.0
            var expectedNextSec = 0.0
            var sampleRate = 0
            var channelCount = 0
            try {
                pcmTap.chunks.collect { chunk ->
                    if (gen != generation) throw CancellationException("Fingerprint tap superseded")
                    val frames = chunkFrames(chunk)
                    if (frames == 0) return@collect
                    val isContinuous = streamer != null &&
                        chunk.sampleRate == sampleRate &&
                        chunk.channelCount == channelCount &&
                        abs(chunk.positionSec - expectedNextSec) <= FingerprintConstants.TAP_CONTINUITY_TOLERANCE_SECONDS
                    if (!isContinuous) {
                        streamer?.let { finishTapStreamer(gen, it, matcher, streamStartSec) }
                        Timber.d("FingerprintTimingManager: tap stream started at %.1fs", chunk.positionSec)
                        streamer = StreamingWindowedFingerprinter(
                            chunk.sampleRate.toUInt(),
                            chunk.channelCount.toUShort(),
                            FingerprintConstants.WINDOW_DURATION_MS.toUInt(),
                            FingerprintConstants.WINDOW_INTERVAL_MS.toUInt(),
                        )
                        streamStartSec = chunk.positionSec
                        sampleRate = chunk.sampleRate
                        channelCount = chunk.channelCount
                    }
                    val windows = streamer.pushSamplesF32(chunkToFloatSamples(chunk), chunk.channelCount.toUShort())
                    if (windows.isNotEmpty()) {
                        mutex.withLock {
                            if (gen != generation) throw CancellationException("Fingerprint tap superseded")
                            processMatches(windows, matcher, streamStartSec)
                        }
                    }
                    expectedNextSec = chunk.positionSec + frames.toDouble() / chunk.sampleRate
                }
            } catch (_: CancellationException) {
                Timber.d("FingerprintTimingManager: tap collection cancelled")
            } catch (e: Exception) {
                Timber.w(e, "FingerprintTimingManager: tap collection failed")
                if (gen == generation && state !is State.Active) {
                    markFailed(e, stage = "pcm_tap")
                }
            } finally {
                streamer?.close()
            }
        }
    }

    private suspend fun finishTapStreamer(
        gen: Long,
        streamer: StreamingWindowedFingerprinter,
        matcher: CheckpointMatcher,
        streamStartSec: Double,
    ) {
        try {
            val tail = streamer.flush()
            if (tail.isNotEmpty()) {
                mutex.withLock {
                    if (gen != generation) throw CancellationException("Fingerprint tap superseded")
                    processMatches(tail, matcher, streamStartSec)
                }
            }
        } finally {
            streamer.close()
        }
    }

    private fun chunkFrames(chunk: FingerprintPcmTap.PcmChunk): Int {
        val bytesPerSample = if (chunk.encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val bytesPerFrame = bytesPerSample * chunk.channelCount
        return if (bytesPerFrame > 0) chunk.data.size / bytesPerFrame else 0
    }

    private fun chunkToFloatSamples(chunk: FingerprintPcmTap.PcmChunk): List<Float> {
        val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.nativeOrder())
        return if (chunk.encoding == C.ENCODING_PCM_FLOAT) {
            val floatBuffer = buffer.asFloatBuffer()
            val result = FloatArray(floatBuffer.remaining())
            floatBuffer.get(result)
            FloatArrayList(result)
        } else {
            val shortBuffer = buffer.asShortBuffer()
            val shorts = ShortArray(shortBuffer.remaining())
            shortBuffer.get(shorts)
            val result = FloatArray(shorts.size)
            for (i in shorts.indices) {
                result[i] = shorts[i] / 32768f
            }
            FloatArrayList(result)
        }
    }

    private class AudioOpenException(cause: Exception) : Exception(cause)
    private class AudioUnavailableException(message: String) : Exception(message)

    private class AudioStream(
        val extractor: MediaExtractor,
        val codec: MediaCodec,
        val format: MediaFormat,
        val sampleRate: Int,
        val channelCount: Int,
        private val cacheSource: AutoCloseable? = null,
    ) : AutoCloseable {
        fun start() {
            // Request float PCM output. If the codec doesn't support it,
            // it falls back to 16-bit; we detect the actual format below.
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, android.media.AudioFormat.ENCODING_PCM_FLOAT)
            codec.configure(format, null, null, 0)
            codec.start()
        }

        override fun close() {
            runCatching { codec.stop() }
            codec.release()
            extractor.release()
            cacheSource?.close()
        }
    }

    private fun openAudioStream(
        gen: Long,
        audioFilePath: String,
        startingAt: Double,
        sharedCacheKey: String?,
        followPlayerCache: Boolean = true,
        isCallerActive: () -> Boolean = { true },
    ): AudioStream {
        val isRemoteUrl = audioFilePath.startsWith("http://") || audioFilePath.startsWith("https://")
        if (!isRemoteUrl && !File(audioFilePath).exists()) {
            throw AudioUnavailableException("audio file not found at $audioFilePath")
        }

        val factory = dataSourceFactory.get()
        // Only follow the player's on-disk cache when it actually exists; otherwise read the URL
        // through the app's HTTP stack so requests carry the player's headers and connection pool.
        val cacheKey = sharedCacheKey?.takeIf { isRemoteUrl && factory.isCacheAvailable }
        val isActive = { gen == generation && isCallerActive() }
        val extractor = MediaExtractor()
        val cacheSource = when {
            cacheKey != null && followPlayerCache -> StreamingMediaDataSource(
                dataSourceFactory = factory.blockingCacheFactory,
                uri = audioFilePath.toUri(),
                cacheKey = cacheKey,
                isActive = isActive,
                cachedLengthAt = { position, length -> factory.cachedLengthAt(cacheKey, position, length) },
            )

            // Bounded resolves target regions the player may not have cached yet; read through
            // the cache instead of waiting for the player to fill it.
            cacheKey != null -> StreamingMediaDataSource(
                dataSourceFactory = factory.cacheFactory,
                uri = audioFilePath.toUri(),
                cacheKey = cacheKey,
                isActive = isActive,
            )

            isRemoteUrl -> StreamingMediaDataSource(
                dataSourceFactory = factory.upstreamFactory,
                uri = audioFilePath.toUri(),
                isActive = isActive,
            )

            else -> null
        }
        try {
            if (cacheSource != null) {
                extractor.setDataSource(cacheSource)
            } else {
                extractor.setDataSource(audioFilePath)
            }
        } catch (e: Exception) {
            extractor.release()
            cacheSource?.close()
            throw AudioOpenException(e)
        }

        val audioTrackIndex = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
        if (audioTrackIndex == null) {
            extractor.release()
            cacheSource?.close()
            throw AudioUnavailableException("no audio track found")
        }

        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: "audio/mpeg"

        if (startingAt > 0) {
            extractor.seekTo((startingAt * 1_000_000).toLong(), MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        }

        val codec = try {
            MediaCodec.createDecoderByType(mime)
        } catch (e: Exception) {
            extractor.release()
            cacheSource?.close()
            throw AudioOpenException(e)
        }
        return AudioStream(extractor, codec, format, sampleRate, channelCount, cacheSource)
    }

    private suspend fun streamFingerprint(
        gen: Long,
        audioFilePath: String,
        matcher: CheckpointMatcher,
        episodeUuid: String,
        startingAt: Double,
    ) {
        val isRemoteUrl = audioFilePath.startsWith("http://") || audioFilePath.startsWith("https://")
        val callerJob = currentCoroutineContext().job
        val stream = try {
            openAudioStream(gen, audioFilePath, startingAt, currentSharedCacheKey, isCallerActive = { callerJob.isActive })
        } catch (e: AudioUnavailableException) {
            Timber.w("FingerprintTimingManager: ${e.message}")
            markUnavailable(reason = "audio_unavailable", isStreaming = isRemoteUrl, episodeUuid = episodeUuid)
            return
        } catch (e: AudioOpenException) {
            Timber.w(e, "FingerprintTimingManager: failed to open audio file")
            markFailed(e.cause ?: e, stage = "audio_open")
            return
        }

        stream.use {
            stream.start()

            val streamer = StreamingWindowedFingerprinter(
                stream.sampleRate.toUInt(),
                stream.channelCount.toUShort(),
                FingerprintConstants.WINDOW_DURATION_MS.toUInt(),
                FingerprintConstants.WINDOW_INTERVAL_MS.toUInt(),
            )

            try {
                decodeAndFingerprint(
                    stream = stream,
                    gen = gen,
                    streamer = streamer,
                    startOffset = startingAt,
                    endingAt = null,
                    yieldToResolves = true,
                ) { windows ->
                    mutex.withLock {
                        if (gen != generation) throw CancellationException("Fingerprint stream superseded")
                        processMatches(windows, matcher, startingAt)
                    }
                }

                // Flush remaining windows
                val tail = streamer.flush()
                if (tail.isNotEmpty()) {
                    mutex.withLock {
                        if (gen != generation) throw CancellationException("Fingerprint stream superseded")
                        processMatches(tail, matcher, startingAt)
                    }
                }
            } finally {
                streamer.close()
            }
        }
    }

    private suspend fun decodeAndFingerprint(
        stream: AudioStream,
        gen: Long,
        streamer: StreamingWindowedFingerprinter,
        startOffset: Double,
        endingAt: Double?,
        yieldToResolves: Boolean = false,
        stopWhen: (() -> Boolean)? = null,
        onWindows: suspend (List<WindowedFingerprint>) -> Unit,
    ) {
        val extractor = stream.extractor
        val codec = stream.codec
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var stopRequested = false
        val timeoutUs = FingerprintConstants.CODEC_TIMEOUT_US
        // Default to 16-bit (safest assumption). Updated when the codec
        // reports its actual format via INFO_OUTPUT_FORMAT_CHANGED.
        var isOutputFloat = false

        while (true) {
            currentCoroutineContext().ensureActive()
            if (gen != generation) throw CancellationException("Fingerprint stream superseded")

            // An on-demand resolve is on a tight user-facing budget; give it the decode headroom.
            if (yieldToResolves) {
                while (activeResolves.get() > 0) {
                    delay(FingerprintConstants.RESOLVE_YIELD_POLL_MS)
                    currentCoroutineContext().ensureActive()
                    if (gen != generation) throw CancellationException("Fingerprint stream superseded")
                }
            }

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
                    val isEos = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0

                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val samples = extractFloatSamples(outputBuffer, bufferInfo, isOutputFloat)
                        if (samples.isNotEmpty()) {
                            val windows = streamer.pushSamplesF32(samples, stream.channelCount.toUShort())
                            if (windows.isNotEmpty()) {
                                onWindows(windows)
                                if (stopWhen?.invoke() == true) {
                                    stopRequested = true
                                }
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (isEos || stopRequested) break
                    if (endingAt != null && startOffset + streamer.durationMs().toDouble() / 1000.0 >= endingAt) break
                }
            }
        }
    }

    // The uniffi binding wants List<Float>; a FloatArray view keeps samples unboxed until the
    // FFI boundary instead of materialising an ArrayList of boxed floats per decoded buffer.
    private class FloatArrayList(private val values: FloatArray) : AbstractList<Float>() {
        override val size get() = values.size
        override fun get(index: Int): Float = values[index]
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
                FloatArrayList(result)
            } else {
                // 16-bit PCM: convert to normalized float [-1.0, 1.0]
                val shortBuffer = buffer.asShortBuffer()
                val shortCount = shortBuffer.remaining()
                if (shortCount == 0) return emptyList()
                val shorts = ShortArray(shortCount)
                shortBuffer.get(shorts)
                val result = FloatArray(shortCount)
                for (i in 0 until shortCount) {
                    result[i] = shorts[i] / 32768f
                }
                FloatArrayList(result)
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
        val isDebug = debugTrackingEnabled || FeatureFlag.isEnabled(Feature.SYNCED_TRANSCRIPT_DEBUG)

        val sizeBefore = mapping.playbackToReference.size
        matchWindows(windows, matcher, startOffset, mapping) { rejected ->
            if (isDebug) recordDebugRejection(rejected.playbackTime, rejected.score)
        }

        val coverage = mapping.playbackToReference.size
        if (coverage != sizeBefore) {
            publishSnapshot()
            if (coverage >= FingerprintConstants.MINIMUM_COVERAGE_FOR_ACTIVE) {
                markActive(coverage)
            }
        }
        if (isDebug) {
            debugRejectionsSnapshot = debugRejections.toList()
        }
    }

    private fun matchWindows(
        windows: List<WindowedFingerprint>,
        matcher: CheckpointMatcher,
        startOffset: Double,
        acc: MappingAccumulator,
        onRejected: (TimeMappingEntry) -> Unit = {},
    ) {
        for (window in windows) {
            val absolutePlaybackTime = startOffset + window.timestampMs.toDouble() / 1000.0
            val matches = matcher.findTopMatches(window.hashes, 2u)
            val best = matches.firstOrNull()

            // Below floor — invisible (neither green nor red per iOS spec)
            if (best == null || best.score < FingerprintConstants.MATCH_SCORE_THRESHOLD) {
                continue
            }

            val runnerUpScore = matches.getOrNull(1)?.score ?: 0f
            val dominance = best.score - runnerUpScore
            val candidate = TimeMappingEntry(
                playbackTime = absolutePlaybackTime,
                referenceTime = best.timestamp.toDouble(),
                score = best.score,
            )

            // Passes floor but fails anchor threshold or dominance → rejection (red)
            if (best.score < FingerprintConstants.DRIFT_ANCHOR_SCORE_THRESHOLD ||
                dominance < FingerprintConstants.DRIFT_SCORE_DOMINANCE_GAP
            ) {
                onRejected(candidate)
                continue
            }

            acc.consider(candidate, onRejected)
        }
    }

    private fun persistMappingCacheIfFull() {
        val audioPath = currentAudioFilePath ?: return
        if (audioPath.startsWith("http://") || audioPath.startsWith("https://")) return
        val refPath = currentReferenceFilePath ?: return
        val refData = currentReferenceData ?: return
        val refDuration = currentReference?.totalDuration ?: return

        val entries = snapshotPlaybackToReference
        FingerprintMappingCache.save(entries, audioPath, refPath, refData, refDuration)
    }

    internal fun consider(
        candidate: TimeMappingEntry,
        isDebug: Boolean = debugTrackingEnabled || FeatureFlag.isEnabled(Feature.SYNCED_TRANSCRIPT_DEBUG),
    ): Int {
        val inserted = mapping.consider(candidate) { rejected ->
            if (isDebug) recordDebugRejection(rejected.playbackTime, rejected.score)
            Timber.d("FingerprintTimingManager: drift filter rejected candidate at playback ${rejected.playbackTime}s")
        }
        if (isDebug) debugRejectionsSnapshot = debugRejections.toList()
        return inserted
    }

    private fun recordDebugRejection(playbackTime: Double, score: Float?) {
        debugRejections.add(DebugRejection(playbackTime, score))
        if (debugRejections.size > FingerprintConstants.DEBUG_REJECTION_CAP) {
            debugRejections.removeAt(0)
        }
    }

    internal fun insertMapping(entry: TimeMappingEntry) {
        mapping.insert(entry)
    }

    /** Publish an immutable snapshot of the mapping lists for lock-free reads. */
    private fun publishSnapshot() {
        snapshotPlaybackToReference = mapping.playbackToReference.toList()
        snapshotReferenceToPlayback = mapping.referenceToPlayback.toList()
        _mappingVersion.value++
    }

    /** Test seam: insert a mapping directly. Must only be called from single-threaded tests. */
    @VisibleForTesting
    fun insert(mapping: TimeMappingEntry) {
        insertMapping(mapping)
        publishSnapshot()
    }

    /** Test seam: route candidates through the drift filter. Must only be called from single-threaded tests. */
    @VisibleForTesting
    fun stubMatches(entries: List<TimeMappingEntry>) {
        for (entry in entries) {
            consider(entry)
        }
        publishSnapshot()
        debugRejectionsSnapshot = debugRejections.toList()
    }

    companion object {
        /** Core eager-pass gate (assumes the platform check already passed). */
        internal fun computeEager(
            hasGeneratedChapters: Boolean,
            isDownloaded: Boolean,
        ): Boolean = hasGeneratedChapters && isDownloaded

        /** A chapter tap is user-initiated, so metered data is allowed unless the user asked to be warned. */
        internal fun shouldBlockOnDemandResolve(
            isDownloaded: Boolean,
            warnOnMeteredNetwork: Boolean,
            isUnmetered: () -> Boolean,
        ): Boolean = !isDownloaded && warnOnMeteredNetwork && !isUnmetered()

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

        /**
         * Audio region to decode for an on-demand resolve. Ad offsets are non-negative and
         * non-decreasing, so the true position is at or after the reference time; cold resolves
         * only search forward, warm ones bracket the mapping-derived estimate.
         */
        internal fun searchWindow(referenceTimeSec: Double, estimatedPlaybackSec: Double?): SearchWindow {
            if (estimatedPlaybackSec == null) {
                return SearchWindow(
                    startSec = referenceTimeSec,
                    endSec = referenceTimeSec + FingerprintConstants.ON_DEMAND_COLD_BUDGET_SECONDS,
                )
            }
            val startSec = max(referenceTimeSec, estimatedPlaybackSec - FingerprintConstants.ON_DEMAND_BACKWARD_MAX_SECONDS)
            val endSec = max(estimatedPlaybackSec, startSec) + FingerprintConstants.ON_DEMAND_FORWARD_BUDGET_SECONDS
            return SearchWindow(startSec, endSec)
        }

        /** True once a bounded resolve has committed anchors past the target, so further decoding adds nothing. */
        internal fun isResolveTargetCovered(acc: MappingAccumulator, targetReferenceSec: Double): Boolean {
            if (acc.playbackToReference.size < FingerprintConstants.ON_DEMAND_MIN_ANCHORS) return false
            val lastReference = acc.referenceToPlayback.last().referenceTime
            return lastReference >= targetReferenceSec + FingerprintConstants.ON_DEMAND_EARLY_EXIT_MARGIN_SECONDS
        }

        /**
         * Interpolated playback time when the mapping is dense around [referenceTimeSec], in both
         * timelines: anchors bracketing an ad boundary sit close in reference time but far apart in
         * playback time, and interpolating across the boundary would land inside the ad.
         */
        internal fun densePlaybackSec(referenceTimeSec: Double, entries: List<TimeMappingEntry>): Double? {
            var lo = 0
            var hi = entries.size
            while (lo < hi) {
                val mid = (lo + hi) / 2
                if (entries[mid].referenceTime <= referenceTimeSec) lo = mid + 1 else hi = mid
            }
            if (hi - 1 < 0 || hi >= entries.size) return null
            val prev = entries[hi - 1]
            val next = entries[hi]
            if (next.referenceTime - prev.referenceTime > FingerprintConstants.HIGHLIGHT_MAX_GAP_SECONDS) return null
            if (next.playbackTime - prev.playbackTime > FingerprintConstants.HIGHLIGHT_MAX_GAP_SECONDS) return null
            return interpolate(
                time = referenceTimeSec,
                entries = entries,
                keySelector = { it.referenceTime },
                valueSelector = { it.playbackTime },
            )
        }

        /** True when [playbackTime] is bracketed by two anchors ≤ [FingerprintConstants.HIGHLIGHT_MAX_GAP_SECONDS] apart. */
        fun isWithinMatchedContent(playbackTime: Double, entries: List<TimeMappingEntry>): Boolean {
            var lo = 0
            var hi = entries.size
            while (lo < hi) {
                val mid = (lo + hi) / 2
                if (entries[mid].playbackTime <= playbackTime) {
                    lo = mid + 1
                } else {
                    hi = mid
                }
            }
            if (hi - 1 < 0 || hi >= entries.size) return false
            return (entries[hi].playbackTime - entries[hi - 1].playbackTime) <= FingerprintConstants.HIGHLIGHT_MAX_GAP_SECONDS
        }

        fun alignToWindowGrid(time: Double): Double {
            val stride = FingerprintConstants.WINDOW_INTERVAL_MS / 1000.0
            if (stride <= 0) return max(0.0, time)
            return max(0.0, floor(time / stride) * stride)
        }
    }
}
