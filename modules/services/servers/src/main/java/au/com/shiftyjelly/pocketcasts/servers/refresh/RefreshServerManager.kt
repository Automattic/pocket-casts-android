package au.com.shiftyjelly.pocketcasts.servers.refresh

import retrofit2.Response

interface RefreshServerManager {

    suspend fun importOpml(urls: List<String>): Response<StatusResponse<ImportOpmlResponse>>

    suspend fun pollImportOpml(pollUuids: List<String>): Response<StatusResponse<ImportOpmlResponse>>

    suspend fun refreshPodcastFeed(podcastUuid: String): Response<StatusResponse<BasicResponse>>
}
