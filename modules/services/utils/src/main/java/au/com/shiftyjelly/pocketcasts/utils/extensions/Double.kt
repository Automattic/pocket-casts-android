package au.com.shiftyjelly.pocketcasts.utils.extensions

import kotlin.math.max
import kotlin.math.min

fun Double.clipToRange(minimum: Double, maximum: Double): Double {
    return min(max(minimum, this), maximum)
}
