package au.com.shiftyjelly.pocketcasts.repositories.ads

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.type.BlazeAdLocation
import au.com.shiftyjelly.pocketcasts.models.type.MembershipFeature
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServiceManager
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Singleton
class BlazeAdsManagerImpl @Inject constructor(
    private val settings: Settings,
    private val staticServiceManager: StaticServiceManager,
    private val crashLogging: CrashLogging,
    appDatabase: AppDatabase,
    @ApplicationContext private val context: Context,
) : BlazeAdsManager {

    private val blazeAdDao = appDatabase.blazeAdDao()

    override suspend fun updateAds() {
        if (settings.cachedMembership.value.hasFeature(MembershipFeature.NoBannerAds) || Util.isAutomotive(context) || Util.isWearOs(context)) {
            // don't fetch the ads if the user has a subscription, or on Automotive or Wear OS
            return
        }
        try {
            val ads = staticServiceManager.getBlazeAds()
            blazeAdDao.replaceAll(ads)
        } catch (e: Exception) {
            Timber.e(e)
            crashLogging.sendReport(e)
        }
    }

    override fun findPodcastListAd(): Flow<BlazeAd?> {
        return findBlazeAdByLocation(BlazeAdLocation.PodcastList)
    }

    override fun findPlayerAd(): Flow<BlazeAd?> {
        return findBlazeAdByLocation(BlazeAdLocation.Player)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun findBlazeAdByLocation(location: BlazeAdLocation): Flow<BlazeAd?> {
        val featureFlag = location.feature
        if (location == BlazeAdLocation.Unknown || featureFlag == null) {
            return flowOf(null)
        }
        return combine(
            settings.cachedMembership.flow,
            FeatureFlag.isEnabledFlow(featureFlag),
            ::Pair,
        ).flatMapLatest { (membership, isEnabled) ->
            if (isEnabled && !membership.hasFeature(MembershipFeature.NoBannerAds)) {
                blazeAdDao.findByLocationFlow(location)
                    .map { promotions -> promotions.firstOrNull() }
            } else {
                flowOf(null)
            }
        }
    }
}
