package au.com.shiftyjelly.pocketcasts.utils.config

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature

object FirebaseConfig {
    const val PERIODIC_SAVE_TIME_MS = "periodic_playback_save_ms"
    const val PODCAST_SEARCH_DEBOUNCE_MS = "podcast_search_debounce_ms"
    const val EPISODE_SEARCH_DEBOUNCE_MS = "episode_search_debounce_ms"
    const val CLOUD_STORAGE_LIMIT = "custom_storage_limit_gb"
    const val FEATURE_FLAG_SEARCH_IMPROVEMENTS = "feature_flag_search_improvements"
    val defaults = mapOf(
        PERIODIC_SAVE_TIME_MS to 60000L,
        PODCAST_SEARCH_DEBOUNCE_MS to 2000L,
        EPISODE_SEARCH_DEBOUNCE_MS to 2000L,
        CLOUD_STORAGE_LIMIT to 10L,
        FEATURE_FLAG_SEARCH_IMPROVEMENTS to false
    ) + Feature.values()
        .filter { it.hasFirebaseRemoteFlag }
        .associate { it.key to it.defaultValue }
}
