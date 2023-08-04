package au.com.shiftyjelly.pocketcasts.servers.extensions

import com.google.protobuf.Timestamp
import timber.log.Timber
import java.time.Instant
import java.util.Date

fun Timestamp.toDate(): Date? {
    return try {
        Date.from(Instant.ofEpochSecond(seconds, nanos.toLong()))
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}
