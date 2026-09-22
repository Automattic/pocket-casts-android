package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import android.content.Context
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewCatalog
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

    fun decode(body: String): WhatsNewCatalog? {
        return try {
            adapter.fromJson(body)?.toCatalog()
        } catch (e: Exception) {
            Timber.w(e, "Could not read a What's New catalog")
            null
        }
    }

    fun read(locale: String): WhatsNewCatalog? {
        val file = catalogFile(locale)
        if (!file.exists()) return null

        return try {
            decode(file.readText())
        } catch (e: Exception) {
            Timber.w(e, "Could not read the cached What's New catalog")
            null
        }
    }

    fun write(locale: String, body: String) {
        val file = catalogFile(locale)
        try {
            file.parentFile?.mkdirs()
            val temporaryFile = File(file.parentFile, "${file.name}.tmp")
            temporaryFile.writeText(body)
            if (!temporaryFile.renameTo(file)) {
                temporaryFile.delete()
                Timber.w("Could not replace the cached What's New catalog")
            }
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
