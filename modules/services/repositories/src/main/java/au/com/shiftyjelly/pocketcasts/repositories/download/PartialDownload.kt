package au.com.shiftyjelly.pocketcasts.repositories.download

import java.io.File
import java.util.Properties
import okhttp3.Response

internal data class PartialDownload(
    val entityTag: String?,
    val lastModified: String?,
    val contentLength: Long,
) {
    val validator get() = requireNotNull(entityTag ?: lastModified)

    fun rejectionReason(response: Response, offset: Long): String? {
        val contentRange = response.header("Content-Range")?.let(CONTENT_RANGE_REGEX::matchEntire)?.destructured
        return when {
            response.code != 206 -> "status ${response.code}"
            !response.hasIdentityEncoding() -> "content encoding"
            contentRange == null -> "missing content range"
            contentRange.component1().toLongOrNull() != offset -> "range start"
            contentRange.component2().toLongOrNull() != contentLength - 1 -> "range end"
            contentRange.component3().toLongOrNull() != contentLength -> "content length"
            entityTag != null && response.header("ETag") != entityTag -> "entity tag"
            entityTag == null && response.header("Last-Modified") != lastModified -> "last modified"
            else -> null
        }
    }

    fun writeTo(tempFile: File) {
        val properties = Properties()
        entityTag?.let { properties.setProperty(ENTITY_TAG_KEY, it) }
        lastModified?.let { properties.setProperty(LAST_MODIFIED_KEY, it) }
        properties.setProperty(CONTENT_LENGTH_KEY, contentLength.toString())
        metadataFile(tempFile).outputStream().use { stream -> properties.store(stream, null) }
    }

    companion object {
        const val OVERLAP_BYTE_COUNT = 64 * 1024L

        fun from(response: Response): PartialDownload? {
            val contentLength = response.body.contentLength()
            val entityTag = response.header("ETag")?.takeUnless { it.startsWith("W/") }
            val lastModified = response.header("Last-Modified")
            return if (response.code == 200 && contentLength > 0 && response.hasIdentityEncoding() && (entityTag != null || lastModified != null)) {
                PartialDownload(entityTag, lastModified, contentLength)
            } else {
                null
            }
        }

        fun readFrom(tempFile: File): PartialDownload? {
            val metadataFile = metadataFile(tempFile)
            if (!tempFile.isFile || !metadataFile.isFile) {
                return null
            }
            val properties = runCatching {
                Properties().apply { metadataFile.inputStream().use(::load) }
            }.getOrNull() ?: return null
            val partialDownload = PartialDownload(
                entityTag = properties.getProperty(ENTITY_TAG_KEY),
                lastModified = properties.getProperty(LAST_MODIFIED_KEY),
                contentLength = properties.getProperty(CONTENT_LENGTH_KEY)?.toLongOrNull() ?: return null,
            )
            val hasValidator = partialDownload.entityTag != null || partialDownload.lastModified != null
            return partialDownload.takeIf { hasValidator && tempFile.length() in OVERLAP_BYTE_COUNT..partialDownload.contentLength }
        }

        fun delete(tempFile: File) {
            tempFile.delete()
            metadataFile(tempFile).delete()
        }

        private fun metadataFile(tempFile: File) = File("${tempFile.path}.resume")

        private fun Response.hasIdentityEncoding() = header("Content-Encoding").let { it == null || it.equals("identity", ignoreCase = true) }

        private val CONTENT_RANGE_REGEX = Regex("""bytes (\d+)-(\d+)/(\d+)""", RegexOption.IGNORE_CASE)

        private const val ENTITY_TAG_KEY = "entityTag"
        private const val LAST_MODIFIED_KEY = "lastModified"
        private const val CONTENT_LENGTH_KEY = "contentLength"
    }
}
