package au.com.shiftyjelly.pocketcasts.utils.config

import au.com.shiftyjelly.pocketcasts.helper.BuildConfig
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature

object FirebaseConfig {
    const val PERIODIC_SAVE_TIME_MS = "periodic_playback_save_ms"
    const val PLAYER_RELEASE_TIME_OUT_MS = "player_release_time_out_ms"
    const val PODCAST_SEARCH_DEBOUNCE_MS = "podcast_search_debounce_ms"
    const val EPISODE_SEARCH_DEBOUNCE_MS = "episode_search_debounce_ms"
    const val CLOUD_STORAGE_LIMIT = "custom_storage_limit_gb"
    const val REPORT_VIOLATION_URL = "report_violation_url"
    const val SLUMBER_STUDIOS_YEARLY_PROMO_CODE = "slumber_studios_yearly_promo_code"
    const val SLEEP_TIMER_DEVICE_SHAKE_THRESHOLD = "sleep_timer_device_shake_threshold"
    const val REFRESH_PODCASTS_BATCH_SIZE = "refresh_podcasts_batch_size"
    const val EXOPLAYER_CACHE_SIZE_IN_MB = "exoplayer_cache_size_in_mb"
    const val EXOPLAYER_CACHE_ENTIRE_PLAYING_EPISODE_SIZE_IN_MB = "exoplayer_cache_entire_playing_episode_size_in_mb"
    const val EXOPLAYER_CACHE_ENTIRE_PLAYING_EPISODE_SETTING_DEFAULT = "exoplayer_cache_entire_playing_episode_setting_default"
    const val PLAYBACK_EPISODE_POSITION_CHANGED_ON_SYNC_THRESHOLD_SECS = "playback_episode_position_changed_on_sync_threshold_secs"

    val defaults = mapOf(
        PERIODIC_SAVE_TIME_MS to 60000L,
        PLAYER_RELEASE_TIME_OUT_MS to 500L,
        PODCAST_SEARCH_DEBOUNCE_MS to 2000L,
        EPISODE_SEARCH_DEBOUNCE_MS to 2000L,
        CLOUD_STORAGE_LIMIT to 10L,
        SLEEP_TIMER_DEVICE_SHAKE_THRESHOLD to 30L,
        REFRESH_PODCASTS_BATCH_SIZE to 200L,
        EXOPLAYER_CACHE_SIZE_IN_MB to if (BuildConfig.DEBUG) 100L else 0L,
        EXOPLAYER_CACHE_ENTIRE_PLAYING_EPISODE_SIZE_IN_MB to if (BuildConfig.DEBUG) 500L else 0L,
        EXOPLAYER_CACHE_ENTIRE_PLAYING_EPISODE_SETTING_DEFAULT to true,
        PLAYBACK_EPISODE_POSITION_CHANGED_ON_SYNC_THRESHOLD_SECS to 5L,
    ) + Feature.values()
        .filter { it.hasFirebaseRemoteFlag }
        .associate { it.key to it.defaultValue }
}
