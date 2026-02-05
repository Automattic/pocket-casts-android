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
    private val cache = MutableStateFlow(emptyMap<String, Double?>())

    fun progressFlow(episodeUuid: String): StateFlow<Double?> {
        return cache.mapState { data ->
            when (val value = data[episodeUuid]) {
                null, 0.0, 1.0 -> value
                else -> (value * 100).roundToInt() / 100.0
            }
        }
    }

    fun updateProgress(episodeUuid: String, progress: Double) {
        val value = progress.coerceIn(0.0, 1.0)
        cache.update { data -> data + (episodeUuid to value) }
    }

    fun clearProgress(episodeUuid: String) {
        cache.update { data -> data - episodeUuid }
    }
}
