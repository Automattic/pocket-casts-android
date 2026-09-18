package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingAutoDownloadEpisodes @Inject constructor(
    private val settings: Settings,
) {
    private val lock = Any()

    fun record(episodeUuids: Collection<String>) {
        if (episodeUuids.isEmpty()) {
            return
        }
        synchronized(lock) {
            val current = settings.pendingAutoDownloadEpisodeUuids.value
            val retained = (current + episodeUuids).distinct().takeLast(MAX_RETAINED_UUIDS)
            if (retained != current) {
                settings.pendingAutoDownloadEpisodeUuids.set(retained, updateModifiedAt = false, commit = true)
            }
        }
    }

    fun all(): List<String> = settings.pendingAutoDownloadEpisodeUuids.value

    fun remove(episodeUuids: Collection<String>) {
        if (episodeUuids.isEmpty()) {
            return
        }
        synchronized(lock) {
            val current = settings.pendingAutoDownloadEpisodeUuids.value
            val remaining = current - episodeUuids.toSet()
            if (remaining.size != current.size) {
                settings.pendingAutoDownloadEpisodeUuids.set(remaining, updateModifiedAt = false, commit = true)
            }
        }
    }

    private companion object {
        const val MAX_RETAINED_UUIDS = 500
    }
}
