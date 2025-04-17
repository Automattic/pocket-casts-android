package au.com.shiftyjelly.pocketcasts.repositories.lists

import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.server.ListWebService
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

class ListRepository(
    private val listWebService: ListWebService,
    private val syncManager: SyncManager,
    private val platform: String,
) {

    suspend fun getDiscoverFeed(): Discover {
        val version = if (FeatureFlag.isEnabled(Feature.RECOMMENDATIONS)) 3 else 2
        return listWebService.getDiscoverFeed(platform = platform, version = version)
    }

    suspend fun getListFeed(url: String, authenticated: Boolean?): ListFeed {
        return if (authenticated == true) {
            syncManager.getCacheTokenOrLogin { token ->
                listWebService.getListFeedAuthenticated(url, "Bearer ${token.value}")
            }
        } else {
            listWebService.getListFeed(url)
        }
    }

    suspend fun getCategoriesList(url: String): List<DiscoverCategory> {
        return listWebService.getCategoriesList(url)
    }
}
