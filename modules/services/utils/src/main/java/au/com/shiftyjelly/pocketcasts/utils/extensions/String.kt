package au.com.shiftyjelly.pocketcasts.utils.extensions

import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val ISO_DATE_FORMATS = object : ThreadLocal<List<SimpleDateFormat>>() {
    override fun initialValue(): List<SimpleDateFormat> {
        return listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            // ISO dates can have milliseconds
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        )
    }
}

fun String.parseIsoDate(): Date? {
    val formats = ISO_DATE_FORMATS.get() ?: return null
    for (format in formats) {
        try {
            return format.parse(this)
        } catch (e: Exception) {
            // try next format
        }
    }
    Timber.e("Parsing ISO date failed [${toString()}]")
    return null
}

fun String.toSecondsFromColonFormattedString(): Int? {
    if (this.isBlank()) {
        return null
    }

    var time = 0.0
    val parts = this.split(":").toTypedArray()
    var multiplier = 1
    try {
        for (i in parts.indices.reversed()) {
            time += (Integer.parseInt(parts[i]) * multiplier).toDouble()
            multiplier *= 60
        }

        return time.toInt()
    } catch (e: NumberFormatException) {
        Timber.e(e)
    }

    return null
}

fun CharSequence.splitIgnoreEmpty(delimiter: String): List<String> {
    return if (isEmpty()) emptyList() else split(delimiter)
}

fun String.removeNewLines(): String {
    return this.replace("[\n\r]".toRegex(), "")
}

fun String.sha1(): String? {
    return try {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(this.toByteArray(charset("iso-8859-1")), 0, this.length)
        val hash = digest.digest()
        hash.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        null
    }
}

/* https://en.gravatar.com/site/implement/images/java/ */
fun String.md5Hex(): String? {
    try {
        val md = MessageDigest.getInstance("MD5")
        return hex(md.digest(this.toByteArray(charset("CP1252"))))
    } catch (e: NoSuchAlgorithmException) {
        Timber.e(e.message)
    } catch (e: UnsupportedEncodingException) {
        Timber.e(e.message)
    }
    return null
}

private fun hex(array: ByteArray): String {
    val sb = StringBuffer()
    for (i in array.indices) {
        sb.append(Integer.toHexString((array[i].toInt() and 0xFF) or 0x100).substring(1, 3))
    }
    return sb.toString()
}
