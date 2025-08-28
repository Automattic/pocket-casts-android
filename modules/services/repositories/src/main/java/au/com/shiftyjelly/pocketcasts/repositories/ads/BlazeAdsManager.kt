package au.com.shiftyjelly.pocketcasts.repositories.ads

import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import kotlinx.coroutines.flow.Flow

interface BlazeAdsManager {

    suspend fun updateAds()
    fun findPodcastListAd(): Flow<BlazeAd?>
    fun findPlayerAd(): Flow<BlazeAd?>
}
