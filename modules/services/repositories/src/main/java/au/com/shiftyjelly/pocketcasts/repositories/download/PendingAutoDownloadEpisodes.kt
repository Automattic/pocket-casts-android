package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import javax.inject.Inject

class PendingAutoDownloadEpisodes @Inject constructor(
    private val settings: Settings,
) {
    fun record(episodeUuids: Collection<String>) {
        if (episodeUuids.isEmpty()) {
            return
        }
        val retained = (settings.pendingAutoDownloadEpisodeUuids.value + episodeUuids)
            .distinct()
            .takeLast(MAX_RETAINED_UUIDS)
        settings.pendingAutoDownloadEpisodeUuids.set(retained, updateModifiedAt = false, commit = true)
    }

    fun all(): List<String> = settings.pendingAutoDownloadEpisodeUuids.value

    fun clear() {
        if (settings.pendingAutoDownloadEpisodeUuids.value.isEmpty()) {
            return
        }
        settings.pendingAutoDownloadEpisodeUuids.set(emptyList(), updateModifiedAt = false, commit = true)
    }

    private companion object {
        const val MAX_RETAINED_UUIDS = 500
    }
}
