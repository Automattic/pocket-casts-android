package au.com.shiftyjelly.pocketcasts.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp

val Float.nonScaledSp
    @Composable
    get() = (this / LocalDensity.current.fontScale).sp

@Composable
fun Float.fractionedSp(fraction: Float): TextUnit {
    return if (LocalDensity.current.fontScale <= 1f) {
        this.sp
    } else {
        lerp(this.nonScaledSp, this.sp, fraction)
    }
}
