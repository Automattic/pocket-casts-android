package au.com.shiftyjelly.pocketcasts.servers.cdn

import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import javax.inject.Inject

class StaticServiceManagerImpl @Inject constructor(
    private val service: StaticService,
) : StaticServiceManager {
    override suspend fun getColors(podcastUuid: String): ArtworkColors? {
        return service.getColors(podcastUuid)?.toArtworkColors()
    }

    override suspend fun getBlazeAds(): List<BlazeAd> {
        return service.getBlazePromotions().toBlazeAds()
    }
}
