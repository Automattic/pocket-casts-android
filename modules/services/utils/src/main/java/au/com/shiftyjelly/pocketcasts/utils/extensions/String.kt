package au.com.shiftyjelly.pocketcasts.utils.extensions

import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import timber.log.Timber
import java.security.MessageDigest
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
    Timber.w("Parsing ISO date failed [${toString()}]")
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
        Timber.w(e)
    }

    return null
}

fun CharSequence.splitIgnoreEmpty(delimiter: String): List<String> {
    return if (isEmpty()) emptyList() else split(delimiter)
}

fun String.removeNewLines(): String {
    return this.replace("[\n\r]".toRegex(), "")
}

fun String.sha1(): String? = hashString("SHA-1")
fun String.sha256(): String? = hashString("SHA-256")

/**
 * For information on permitted algorithms, see
 * https://developer.android.com/reference/kotlin/java/security/MessageDigest
 */
private fun String.hashString(algorithm: String) =
    try {
        MessageDigest.getInstance(algorithm)
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Error applying $algorithm to $this: ${e.message}")
        null
    }
