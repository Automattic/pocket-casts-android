package au.com.shiftyjelly.pocketcasts.servers.whatsnew

interface WhatsNewServiceManager {
    suspend fun getCatalog(): WhatsNewCatalog
}
