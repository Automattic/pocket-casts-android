package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import jakarta.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

class SuggestedFoldersManager @Inject constructor(
    private val podcastCacheService: PodcastCacheServiceManager,
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    suspend fun suggestedFolders() {
        val result = podcastCacheService.suggestedFolders()

        Timber.i("@@@@@@ suggestedFolders result: $result")
    }
}
