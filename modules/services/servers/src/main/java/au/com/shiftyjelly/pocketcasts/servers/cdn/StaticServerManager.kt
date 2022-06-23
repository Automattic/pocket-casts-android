package au.com.shiftyjelly.pocketcasts.servers.cdn

import au.com.shiftyjelly.pocketcasts.utils.Optional
import io.reactivex.Single

interface StaticServerManager {
    fun getColorsSingle(podcastUuid: String): Single<Optional<ArtworkColors>>
    suspend fun getColors(podcastUuid: String): ArtworkColors?
}
