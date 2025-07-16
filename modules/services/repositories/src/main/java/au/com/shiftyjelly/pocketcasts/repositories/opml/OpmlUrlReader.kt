package au.com.shiftyjelly.pocketcasts.repositories.opml

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.Source
import okio.buffer

internal class OpmlUrlReader {
    fun readUrls(input: Source): List<String> {
        val urls = mutableSetOf<String>()

        input.buffer().use { source ->
            while (true) {
                val line = source.readUtf8Line()
                if (line == null) {
                    break
                }
                line.splitOutlineTags().mapNotNullTo(urls, transform = { it.readXmlUrlAttribute() })
            }
        }

        return urls.toList()
    }

    private fun String.splitOutlineTags() = split("<outline")

    private fun String.readXmlUrlAttribute(): String? {
        val attributeString = substringAfter(XML_URL_ATTRIBUTE, missingDelimiterValue = NO_VALUE_TOKEN)
        if (attributeString === NO_VALUE_TOKEN) {
            return null
        }

        val valueStartToken = attributeString.getOrNull(0)
        if (valueStartToken != '"' && valueStartToken != '\'') {
            return null
        }

        val value = attributeString.drop(1).takeWhile { it != valueStartToken }
        val valueEndToken = attributeString.substringAfter(value).getOrNull(0)
        if (valueStartToken != valueEndToken) {
            return null
        }

        return value.unescapeXmlEntities().takeIf { it.toHttpUrlOrNull() != null }
    }

    private fun String.unescapeXmlEntities(): String {
        return entitiesToChars.entries.fold(this) { text, (entitiy, replacement) ->
            text.replace(entitiy, replacement)
        }
    }

    private companion object {
        const val XML_URL_ATTRIBUTE = "xmlUrl="
        const val NO_VALUE_TOKEN = "NoValue"

        val entitiesToChars = mapOf(
            "&lt;" to "<",
            "&gt;" to ">",
            "&amp;" to "&",
            "&apos;" to "'",
            "&quot;" to "\"",
        )
    }
}
