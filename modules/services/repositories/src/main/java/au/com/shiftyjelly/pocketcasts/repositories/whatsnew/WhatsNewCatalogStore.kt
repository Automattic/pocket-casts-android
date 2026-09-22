package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import android.content.Context
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewCatalogResponse
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class WhatsNewCatalogStore @Inject constructor(
    @ApplicationContext private val context: Context,
    moshi: Moshi,
) {
    private val adapter = moshi.adapter(WhatsNewCatalogResponse::class.java)

    fun read(locale: String): WhatsNewCatalogResponse? {
        val file = catalogFile(locale)
        if (!file.exists()) return null

        return try {
            adapter.fromJson(file.readText())
        } catch (e: Exception) {
            Timber.w(e, "Could not read the cached What's New catalog")
            null
        }
    }

    fun write(locale: String, response: WhatsNewCatalogResponse) {
        try {
            val file = catalogFile(locale)
            file.parentFile?.mkdirs()
            file.writeText(adapter.toJson(response))
        } catch (e: Exception) {
            Timber.w(e, "Could not cache the What's New catalog")
        }
    }

    fun writtenAt(locale: String): Instant? {
        val file = catalogFile(locale)
        return if (file.exists()) Instant.ofEpochMilli(file.lastModified()) else null
    }

    private fun catalogFile(locale: String) = File(File(context.cacheDir, DIRECTORY), "catalog-$locale.json")

    private companion object {
        const val DIRECTORY = "whats-new"
    }
}
