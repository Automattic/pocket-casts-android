package au.com.shiftyjelly.pocketcasts.servers.cdn

import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.utils.Optional
import io.reactivex.Single
import javax.inject.Inject
import kotlinx.coroutines.rx2.rxSingle

class StaticServiceManagerImpl @Inject constructor(
    private val service: StaticService,
) : StaticServiceManager {
    override fun getColorsSingle(podcastUuid: String): Single<Optional<ArtworkColors>> {
        return rxSingle { Optional.of(getColors(podcastUuid)) }
    }

    override suspend fun getColors(podcastUuid: String): ArtworkColors? {
        return service.getColors(podcastUuid)?.toArtworkColors()
    }

    override suspend fun getBlazeAds(): List<BlazeAd> {
        return service.getBlazePromotions().toBlazeAds()
    }
}
