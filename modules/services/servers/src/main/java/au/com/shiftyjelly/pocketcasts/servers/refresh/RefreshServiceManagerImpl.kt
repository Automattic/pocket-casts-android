package au.com.shiftyjelly.pocketcasts.servers.refresh

import android.os.Build
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.RefreshServiceRetrofit
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

    override suspend fun updatePodcast(podcastUuid: String, lastEpisodeUuid: String?): Response<Unit> {
        return service.updatePodcast(podcastUuid = podcastUuid, lastEpisodeUuid = lastEpisodeUuid)
    }

    override suspend fun pollUpdatePodcast(url: String): Response<Unit> {
        return service.pollUpdatePodcast(url)
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
