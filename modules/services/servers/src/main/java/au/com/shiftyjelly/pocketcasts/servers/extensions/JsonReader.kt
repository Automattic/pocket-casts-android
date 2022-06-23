package au.com.shiftyjelly.pocketcasts.servers.extensions

import com.squareup.moshi.JsonReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val JSON_DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

fun JsonReader.nextBooleanOrNull(): Boolean? {
    return if (peek() == JsonReader.Token.NULL) nextNull() else nextBoolean()
}

fun JsonReader.nextIntOrNull(): Int? {
    return if (peek() == JsonReader.Token.NULL) nextNull() else nextInt()
}

fun JsonReader.nextDoubleOrNull(): Double? {
    return if (peek() == JsonReader.Token.NULL) nextNull() else nextDouble()
}

fun JsonReader.nextLongOrNull(): Long? {
    return if (peek() == JsonReader.Token.NULL) nextNull() else nextLong()
}

fun JsonReader.nextStringOrNull(): String? {
    return if (peek() == JsonReader.Token.NULL) nextNull() else nextString()
}

fun JsonReader.nextDateOrNull(): Date? {
    if (peek() == JsonReader.Token.NULL) {
        return nextNull()
    } else {
        try {
            return JSON_DATE_FORMAT.parse(nextString())
        } catch (e: Exception) {
            return null
        }
    }
}

fun JsonReader.nextBooleanOrDefault(default: Boolean): Boolean {
    if (peek() == JsonReader.Token.NULL) {
        nextNull<Boolean>()
        return default
    } else {
        return nextBoolean()
    }
}

fun JsonReader.nextIntOrDefault(default: Int): Int {
    if (peek() == JsonReader.Token.NULL) {
        nextNull<Boolean>()
        return default
    } else {
        return nextInt()
    }
}
