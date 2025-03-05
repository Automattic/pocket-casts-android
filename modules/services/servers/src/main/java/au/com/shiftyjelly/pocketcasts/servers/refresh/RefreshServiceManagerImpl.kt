package au.com.shiftyjelly.pocketcasts.servers.refresh

import android.os.Build
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.RefreshServiceRetrofit
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import retrofit2.Response
import retrofit2.Retrofit

class RefreshServiceManagerImpl @Inject constructor(
    @RefreshServiceRetrofit retrofit: Retrofit,
    private val settings: Settings,
) : RefreshServiceManager {
    private val service: RefreshService = retrofit.create(RefreshService::class.java)

    override suspend fun importOpml(urls: List<String>): Response<StatusResponse<ImportOpmlResponse>> {
        val request = ImportOpmlRequest(urls = urls)
        addDeviceParameters(request)
        return service.importOpml(request)
    }

    override suspend fun pollImportOpml(pollUuids: List<String>): Response<StatusResponse<ImportOpmlResponse>> {
        val request = ImportOpmlRequest(pollUuids = pollUuids)
        addDeviceParameters(request)
        return service.importOpml(request)
    }

    override suspend fun updatePodcast(podcastUuid: String, lastEpisodeUuid: String?): UpdatePodcastResponse {
        return tryHandlePodcastUpdateResponse {
            service.updatePodcast(podcastUuid = podcastUuid, lastEpisodeUuid = lastEpisodeUuid)
        }
    }

    override suspend fun pollUpdatePodcast(url: String): UpdatePodcastResponse {
        return tryHandlePodcastUpdateResponse {
            service.pollUpdatePodcast(url)
        }
    }

    private suspend fun tryHandlePodcastUpdateResponse(block: suspend () -> Response<Unit>): UpdatePodcastResponse {
        return try {
            val response = block()
            when (response.code()) {
                202 -> {
                    val location = response.headers()["Location"]
                    val retryAfter = response.headers()["Retry-After"]?.toIntOrNull()
                    if (location != null && retryAfter != null) {
                        UpdatePodcastResponse.Retry(location = location, retryAfter = retryAfter)
                    } else {
                        UpdatePodcastResponse.NoEpisodeFound
                    }
                }
                200 -> UpdatePodcastResponse.EpisodeFound
                else -> UpdatePodcastResponse.NoEpisodeFound
            }
        } catch (e: IOException) {
            LogBuffer.e(LogBuffer.TAG_CRASH, e, "Failed to update podcast feed")
            UpdatePodcastResponse.Failure
        }
    }

    private fun addDeviceParameters(request: BaseRequest) {
        request.version = Settings.PARSER_VERSION
        request.appVersion = settings.getVersion()
        request.appVersionCode = settings.getVersionCode().toString()
        request.deviceType = Settings.SERVER_DEVICE_TYPE
        request.country = Locale.getDefault().country
        request.language = Locale.getDefault().language
        request.model = Build.MODEL
    }
}

sealed class UpdatePodcastResponse {
    data class Retry(val location: String, val retryAfter: Int) : UpdatePodcastResponse()
    data object EpisodeFound : UpdatePodcastResponse()
    data object NoEpisodeFound : UpdatePodcastResponse()
    data object Failure : UpdatePodcastResponse()
}
