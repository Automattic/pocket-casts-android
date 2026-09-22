package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.util.Locale
import retrofit2.HttpException
import timber.log.Timber

class WhatsNewServiceManagerImpl(
    private val service: WhatsNewCatalogService,
    private val provideLocale: () -> Locale,
) : WhatsNewServiceManager {
    override fun catalogLocale() = WhatsNewCatalogLocale.catalogName(provideLocale())

    override suspend fun getCatalog(): WhatsNewCatalogResponse {
        val locale = catalogLocale()
        return try {
            service.getCatalog(locale)
        } catch (e: HttpException) {
            if (e.code() !in unpublishedCodes || locale == WhatsNewCatalogLocale.FALLBACK) throw e
            Timber.i("No What's New catalog published for $locale, falling back to ${WhatsNewCatalogLocale.FALLBACK}")
            service.getCatalog(WhatsNewCatalogLocale.FALLBACK)
        }
    }

    private companion object {
        val unpublishedCodes = setOf(HTTP_NOT_FOUND, HTTP_FORBIDDEN)
    }
}
