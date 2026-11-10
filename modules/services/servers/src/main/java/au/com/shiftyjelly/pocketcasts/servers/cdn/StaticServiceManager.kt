package au.com.shiftyjelly.pocketcasts.servers.cdn

import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd

interface StaticServiceManager {
    suspend fun getColors(podcastUuid: String): ArtworkColors?
    suspend fun getBlazeAds(): List<BlazeAd>
}
