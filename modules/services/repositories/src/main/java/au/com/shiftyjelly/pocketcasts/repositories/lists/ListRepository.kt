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
        val version = if (FeatureFlag.isEnabled(Feature.RECOMMENDATIONS)) 3 else 2
        return listWebService.getDiscoverFeed(platform = platform, version = version)
    }

    suspend fun getListFeed(url: String, authenticated: Boolean? = false): ListFeed? {
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
            .onFailure { exception ->
                Timber.e(exception, "Failed to fetch list feed $url")
            }
            .getOrNull()
    }

    suspend fun getCategoriesList(url: String): List<DiscoverCategory> {
        return listWebService.getCategoriesList(url)
    }

    suspend fun getPodcastRecommendations(podcastUuid: String, countryCode: String?): ListFeed? {
        return getListFeed(url = "${Settings.SERVER_API_URL}/recommendations/podcast/$podcastUuid?country=${countryCode ?: "global"}")
    }
}
