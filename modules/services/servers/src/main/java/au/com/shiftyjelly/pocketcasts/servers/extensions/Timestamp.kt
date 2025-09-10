package au.com.shiftyjelly.pocketcasts.servers.extensions

import com.google.protobuf.Timestamp
import com.google.protobuf.timestamp
import java.time.Instant
import java.util.Date
import timber.log.Timber

fun Timestamp.toInstant(): Instant? {
    return try {
        Instant.ofEpochSecond(seconds, nanos.toLong())
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}

fun Timestamp.toDate(): Date? {
    return Date.from(toInstant())
}

fun Instant.toTimestamp(): Timestamp {
    return timestamp {
        seconds = epochSecond
        nanos = nano
    }
}

fun Date.toTimestamp(): Timestamp {
    return toInstant().toTimestamp()
}
