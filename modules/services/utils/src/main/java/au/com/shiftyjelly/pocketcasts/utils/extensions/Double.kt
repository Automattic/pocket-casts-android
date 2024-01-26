package au.com.shiftyjelly.pocketcasts.utils.extensions

import kotlin.math.round

/**
 * This is to prevent the issue of having speed values such as 1.2000000000000002
 */
fun Double.roundedSpeed(): Double = round(this.coerceIn(0.5, 3.0) * 10.0) / 10.0
