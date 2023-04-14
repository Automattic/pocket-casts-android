package au.com.shiftyjelly.pocketcasts.utils.extensions

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.round

fun Int.dpToPx(displayMetrics: DisplayMetrics) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), displayMetrics).toInt()
fun Int.dpToPx(context: Context) = this.dpToPx(context.resources.displayMetrics)
fun Int.pxToDp(context: Context) = this / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)

@OptIn(ExperimentalContracts::class)
fun Int?.isPositive(): Boolean {
    contract {
        returns(true) implies (this@isPositive is Int)
    }
    return this != null && this > 0
}

val Int.abbreviated: String
    get() {
        val number = this.toDouble()

        return when {
            number >= 1_000_000 -> "${scaleDown(value = number, by = 1_000_000.0)}M"
            number >= 1_000 -> "${scaleDown(value = number, by = 1_000.0)}K"
            else -> "$this"
        }
    }

private fun scaleDown(value: Double, by: Double): Double {
    return round((value / by) * 10) / 10
}
