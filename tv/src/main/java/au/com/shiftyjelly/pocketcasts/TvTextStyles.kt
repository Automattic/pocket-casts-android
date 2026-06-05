package au.com.shiftyjelly.pocketcasts

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

object TvTextStyles {
    val TabLabel = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight(510),
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
}
