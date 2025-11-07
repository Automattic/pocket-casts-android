package au.com.shiftyjelly.pocketcasts.compose

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

val Color.Companion.pocketRed get() = PocketCastsColors.pocketRed
val Color.Companion.plusGold get() = PocketCastsColors.plusGold
val Color.Companion.plusGoldLight get() = PocketCastsColors.plusGoldLight
val Color.Companion.plusGoldDark get() = PocketCastsColors.plusGoldDark
val Color.Companion.patronPurple get() = PocketCastsColors.patronPurple
val Color.Companion.patronPurpleLight get() = PocketCastsColors.patronPurpleLight
val Color.Companion.patronPurpleDark get() = PocketCastsColors.patronPurpleDark
val Color.Companion.radioactiveGreen get() = PocketCastsColors.radioactiveGreen
val Brush.Companion.plusGradientBrush get() = PocketCastsColors.plusGradientBrush
val Brush.Companion.patronGradientBrush get() = PocketCastsColors.patronGradientBrush

private object PocketCastsColors {
    val pocketRed = Color(0xFFF43E37)
    val plusGold = Color(0xFFFFD846)
    val plusGoldLight = plusGold
    val plusGoldDark = Color(0xFFFEB525)
    val patronPurple = Color(0xFF6046F5)
    val patronPurpleLight = Color(0xFFAFA2FA)
    val patronPurpleDark = patronPurple
    val plusGradientBrush = Brush.horizontalGradient(
        0f to Color.plusGoldLight,
        1f to Color.plusGoldDark,
    )
    val patronGradientBrush = SolidColor(Color.patronPurpleLight)
    val radioactiveGreen = Color(0xFF78D549)
}
