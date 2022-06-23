package au.com.shiftyjelly.pocketcasts.utils

object TimeConstants {
    const val MILLISECONDS_IN_ONE_DAY = 24L * 60L * 60L * 1000L
    const val MILLISECONDS_IN_ONE_HOUR = 60L * 60L * 1000L
    const val MILLISECONDS_IN_ONE_MINUTE = 60L * 1000L
    const val MILLISECONDS_IN_ONE_SECOND = 1000L
}

fun Number.days(): Long {
    return this.toLong() * TimeConstants.MILLISECONDS_IN_ONE_DAY
}

fun Number.hours(): Long {
    return this.toLong() * TimeConstants.MILLISECONDS_IN_ONE_HOUR
}

fun Number.minutes(): Long {
    return this.toLong() * TimeConstants.MILLISECONDS_IN_ONE_MINUTE
}

fun Number.seconds(): Long {
    return this.toLong() * TimeConstants.MILLISECONDS_IN_ONE_SECOND
}
