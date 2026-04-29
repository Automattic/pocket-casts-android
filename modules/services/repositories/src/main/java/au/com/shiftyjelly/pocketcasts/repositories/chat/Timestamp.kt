package au.com.shiftyjelly.pocketcasts.repositories.chat

// Parses "HH:MM:SS" or "HH:MM:SS,mmm" / "HH:MM:SS.mmm" into milliseconds; null if malformed.
internal fun parseTimestampMs(value: String): Int? {
    val (clock, fractionRaw) = value.split(',', '.', limit = 2).let {
        if (it.size == 2) it[0] to it[1] else it[0] to null
    }
    val parts = clock.split(':')
    if (parts.size != 3) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    val seconds = parts[2].toIntOrNull() ?: return null
    val millis = fractionRaw?.padEnd(3, '0')?.take(3)?.toIntOrNull() ?: 0
    if (hours < 0 || minutes < 0 || seconds < 0 || millis < 0) return null
    return ((hours * 3600 + minutes * 60 + seconds) * 1000 + millis)
}
