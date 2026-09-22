package au.com.shiftyjelly.pocketcasts.servers.whatsnew

interface WhatsNewServiceManager {
    fun catalogLocale(): String

    suspend fun getCatalog(): WhatsNewCatalogResponse
}
