package au.com.shiftyjelly.pocketcasts.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp

val Int.nonScaledSp
    @Composable
    get() = (this / LocalDensity.current.fontScale).sp
