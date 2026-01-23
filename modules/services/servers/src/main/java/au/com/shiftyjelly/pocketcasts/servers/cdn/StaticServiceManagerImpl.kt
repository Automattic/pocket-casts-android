package au.com.shiftyjelly.pocketcasts.servers.cdn

import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.utils.Optional
import io.reactivex.Single
import javax.inject.Inject

class StaticServiceManagerImpl @Inject constructor(
    private val service: StaticService,
) : StaticServiceManager {
    override fun getColorsSingle(podcastUuid: String): Single<Optional<ArtworkColors>> {
        return service.getColorsMaybe(podcastUuid)
            // convert response to artwork colors
            .map(ColorsResponse::toArtworkColors)
            // convert to optional so we can handle if the value is missing
            .map { Optional.of(it) }
            .defaultIfEmpty(Optional.empty())
            .toSingle()
    }

    override suspend fun getColors(podcastUuid: String): ArtworkColors? {
        return service.getColors(podcastUuid)?.toArtworkColors()
    }

    override suspend fun getBlazeAds(): List<BlazeAd> {
        return service.getBlazePromotions().toBlazeAds()
    }
}
