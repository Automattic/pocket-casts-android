package au.com.shiftyjelly.pocketcasts.repositories.lists

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.server.ListWebService
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import timber.log.Timber

class ListRepository(
    private val listWebService: ListWebService,
    private val syncManager: SyncManager?,
    private val platform: String,
) {

    suspend fun getDiscoverFeed(): Discover {
        return listWebService.getDiscoverFeed(platform = platform, version = discoverFeedVersion())
    }

    suspend fun getSearchDiscoverFeed(): Discover {
        return listWebService.getSearchDiscoverFeed(platform = platform, version = discoverFeedVersion())
    }

    /** Auth-specific home discover feed. Only served for the TV platform. */
    suspend fun getHomeDiscoverFeed(isLoggedIn: Boolean): Discover {
        val version = discoverFeedVersion()
        return if (isLoggedIn) {
            listWebService.getLoggedInDiscoverFeed(platform = platform, version = version)
        } else {
            listWebService.getLoggedOutDiscoverFeed(platform = platform, version = version)
        }
    }

    suspend fun getListFeed(url: String, authenticated: Boolean? = false): ListFeed? {
        return getListFeedResult(url, authenticated)
            .onFailure { exception ->
                Timber.e(exception, "Failed to fetch list feed $url")
            }
            .getOrNull()
    }

    suspend fun getListFeedResult(url: String, authenticated: Boolean? = false): Result<ListFeed?> {
        return runCatching {
            if (authenticated == true) {
                checkNotNull(syncManager) { "Sync Manager is null" }
                check(syncManager.isLoggedIn()) { "User is not logged in" }
                syncManager.getCacheTokenOrLogin { token ->
                    listWebService.getListFeedAuthenticated(url, "Bearer ${token.value}")
                }
            } else {
                listWebService.getListFeed(url)
            }
        }
    }

    suspend fun getCategoriesList(url: String): List<DiscoverCategory> {
        return listWebService.getCategoriesList(url)
    }

    suspend fun getPodcastRecommendations(podcastUuid: String, countryCode: String?): ListFeed? {
        return getListFeed(url = "${Settings.SERVER_API_URL}/recommendations/podcast/$podcastUuid?country=${countryCode ?: "global"}")
    }

    /** v4 is the first layout to carry `lists_list` rows, so it is only requested once networks are enabled. */
    private fun discoverFeedVersion() = if (FeatureFlag.isEnabled(Feature.NETWORK_DISCOVERY)) {
        DISCOVER_FEED_VERSION_NETWORKS
    } else {
        DISCOVER_FEED_VERSION
    }

    companion object {
        const val PLATFORM_ANDROID = "android"
        const val PLATFORM_AUTOMOTIVE = "automotive"
        const val PLATFORM_TV = "tv"

        private const val DISCOVER_FEED_VERSION = 3
        private const val DISCOVER_FEED_VERSION_NETWORKS = 4
    }
}
