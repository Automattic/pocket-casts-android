package au.com.shiftyjelly.pocketcasts.servers.refresh

import retrofit2.Response

interface RefreshServiceManager {

    suspend fun importOpml(urls: List<String>): Response<StatusResponse<ImportOpmlResponse>>

    suspend fun pollImportOpml(pollUuids: List<String>): Response<StatusResponse<ImportOpmlResponse>>

    suspend fun updatePodcast(podcastUuid: String, lastEpisodeUuid: String?): Response<Unit>

    suspend fun pollUpdatePodcast(url: String): Response<Unit>
}
