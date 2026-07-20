package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

object FingerprintConstants {
    /** If consecutive playback-progress updates differ by more than this, treat it as a seek/skip. */
    const val RESTART_DELTA_SECONDS = 10.0

    /** Slack window around the already-mapped range before playback is considered "outside". */
    const val PLAYBACK_RANGE_MARGIN_SECONDS = 30.0

    /** Minimum time between stream restarts while playback sits outside the mapped range. */
    const val STREAM_BOOTSTRAP_COOLDOWN_MS = 5_000L

    /** Position must be stable for this long before a seek restarts the stream; coalesces rapid multi-tap skips. */
    const val RESTART_DEBOUNCE_MS = 1_500L

    /** How often a running remote decode re-checks that the network still permits streaming. */
    const val METERED_RECHECK_INTERVAL_MS = 5_000L

    /** Minimum match score to accept a fingerprint match result. */
    const val MATCH_SCORE_THRESHOLD = 0.5f

    /** Duration of each windowed fingerprint during live matching, in milliseconds. */
    const val WINDOW_DURATION_MS = 8000

    /** Interval between windowed fingerprints during live matching, in milliseconds. */
    const val WINDOW_INTERVAL_MS = 1000

    /** Minimum number of mapping entries before transitioning to Active. */
    const val MINIMUM_COVERAGE_FOR_ACTIVE = 2

    /** Residual tolerance, in seconds, on the rate-≈1 drift filter. */
    const val DRIFT_TOLERANCE_SECONDS = 5.0

    /** Consecutive rate-≈1 candidates required to establish a trusted anchor. */
    const val DRIFT_BOOTSTRAP_COUNT = 3

    /** Minimum matcher score for a match to reach the drift filter. */
    const val DRIFT_ANCHOR_SCORE_THRESHOLD = 0.65f

    /** Minimum score gap between top-1 and top-2 matcher candidates. */
    const val DRIFT_SCORE_DOMINANCE_GAP = 0.05f

    /** Distance ahead of current playback within which fingerprinting runs at full speed. */
    const val LOOKAHEAD_SECONDS = 60.0

    /** Sleep between chunks when fingerprinting ahead of the lookahead window. */
    const val OUTSIDE_LOOKAHEAD_SLEEP_SECONDS = 0.5

    /** Minimum fraction of reference timeline a cached mapping must cover. */
    const val FULL_COVERAGE_THRESHOLD = 0.95

    /** Max gap between bracketing anchors to consider playback on matched content. */
    const val HIGHLIGHT_MAX_GAP_SECONDS = 8.0

    /** Persistent cache schema version. Bump when on-disk shape changes. */
    const val MAPPING_CACHE_SCHEMA_VERSION = 4

    /** Timeout for MediaCodec dequeue operations, in microseconds. */
    const val CODEC_TIMEOUT_US = 10_000L

    /** Maximum stored debug rejections (FIFO eviction). */
    const val DEBUG_REJECTION_CAP = 500
}
