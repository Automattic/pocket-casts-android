package au.com.shiftyjelly.pocketcasts.servers.refresh

import android.os.Build
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.RefreshServiceRetrofit
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import retrofit2.Response
import retrofit2.Retrofit

class RefreshServiceManagerImpl @Inject constructor(
    @RefreshServiceRetrofit retrofit: Retrofit,
    private val settings: Settings,
) : RefreshServiceManager {

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    }

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

    override suspend fun refreshPodcastFeed(podcastUuid: String): Response<StatusResponse<BasicResponse>> {
        val request = RefreshPodcastFeedRequest(podcastUuid = podcastUuid)
        addDeviceParameters(request)
        return service.refreshPodcastFeed(request)
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
