package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.util.Locale
import okhttp3.CacheControl
import retrofit2.HttpException
import timber.log.Timber

class WhatsNewServiceManagerImpl(
    private val service: WhatsNewCatalogService,
    private val provideLocale: () -> Locale,
) : WhatsNewServiceManager {
    override suspend fun getCatalog(cacheControl: CacheControl?): WhatsNewCatalog {
        val locale = WhatsNewCatalogLocale.catalogName(provideLocale())
        val response = try {
            service.getCatalog(locale, cacheControl)
        } catch (e: HttpException) {
            if (!e.isFallbackCode(cacheControl) || locale == WhatsNewCatalogLocale.FALLBACK) throw e
            Timber.i("No What's New catalog available for $locale, falling back to ${WhatsNewCatalogLocale.FALLBACK}")
            service.getCatalog(WhatsNewCatalogLocale.FALLBACK, cacheControl)
        }
        return response.toCatalog()
    }

    private fun HttpException.isFallbackCode(cacheControl: CacheControl?): Boolean {
        return code() in unpublishedCodes || (cacheControl?.onlyIfCached == true && code() == HTTP_GATEWAY_TIMEOUT)
    }

    private companion object {
        val unpublishedCodes = setOf(HTTP_NOT_FOUND, HTTP_FORBIDDEN)
    }
}
