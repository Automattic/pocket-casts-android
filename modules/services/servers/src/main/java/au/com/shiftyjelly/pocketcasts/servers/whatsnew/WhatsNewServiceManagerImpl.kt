package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WhatsNewServiceManagerImpl @Inject constructor(
    private val service: WhatsNewCatalogService,
    @ApplicationContext private val context: Context,
) : WhatsNewServiceManager {
    override suspend fun getCatalog(): WhatsNewCatalog {
        return service.getCatalog(catalogLocale()).toCatalog()
    }

    private fun catalogLocale(): String {
        val locale = context.resources.configuration.locales[0] ?: return WhatsNewCatalogLocale.FALLBACK
        return WhatsNewCatalogLocale.catalogName(locale)
    }
}
