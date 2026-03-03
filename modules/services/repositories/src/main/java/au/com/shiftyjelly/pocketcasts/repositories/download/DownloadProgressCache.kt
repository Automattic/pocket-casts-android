package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.coroutines.flow.mapState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Singleton
class DownloadProgressCache @Inject constructor() {
    private val _cache = MutableStateFlow(emptyMap<String, DownloadProgress>())

    val progressFlow: StateFlow<Map<String, DownloadProgress>> get() = _cache

    fun progressFlow(episodeUuid: String): StateFlow<DownloadProgress?> {
        return _cache.mapState { data -> data[episodeUuid] }
    }

    fun updateProgress(episodeUuid: String, downloadedByteCount: Long, contentLength: Long?) {
        val progress = DownloadProgress(
            downloadedByteCount = downloadedByteCount,
            contentLength = contentLength,
        )
        _cache.update { data -> data + (episodeUuid to progress) }
    }

    fun clearProgress(episodeUuid: String) {
        _cache.update { data -> data - episodeUuid }
    }

    fun clearProgress(episodeUuids: Set<String>) {
        _cache.update { data -> data - episodeUuids }
    }
}

data class DownloadProgress(
    val downloadedByteCount: Long,
    val contentLength: Long?,
) {
    val percentage = if (contentLength != null && contentLength > 0L && downloadedByteCount >= 0L) {
        val percentage = downloadedByteCount.toDouble() / contentLength
        (percentage * 100).roundToInt().coerceAtMost(100)
    } else {
        null
    }
}
