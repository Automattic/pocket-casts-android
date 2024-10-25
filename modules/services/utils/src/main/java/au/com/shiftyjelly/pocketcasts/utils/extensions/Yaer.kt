package au.com.shiftyjelly.pocketcasts.utils.extensions

import java.time.LocalDate
import java.time.Year
import java.time.ZoneId

fun Year.toEpochMillis(zone: ZoneId) = LocalDate.of(value, 1, 1).atStartOfDay().atZone(zone).toInstant().toEpochMilli()
