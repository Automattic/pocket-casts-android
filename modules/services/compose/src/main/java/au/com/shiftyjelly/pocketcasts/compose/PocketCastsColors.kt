package au.com.shiftyjelly.pocketcasts.compose

import androidx.compose.ui.graphics.Color

val Color.Companion.pocketRed get() = PocketCastsColors.pocketRed
val Color.Companion.plusGold get() = PocketCastsColors.plusGold
val Color.Companion.plusGoldLight get() = PocketCastsColors.plusGoldLight
val Color.Companion.plusGoldDark get() = PocketCastsColors.plusGoldDark
val Color.Companion.patronPurple get() = PocketCastsColors.patronPurple
val Color.Companion.patronPurpleLight get() = PocketCastsColors.patronPurpleLight
val Color.Companion.patronPurpleDark get() = PocketCastsColors.patronPurpleDark

private object PocketCastsColors {
    val pocketRed = Color(0xFFF43E37)
    val plusGold = Color(0xFFFFD846)
    val plusGoldLight = plusGold
    val plusGoldDark = Color(0xFFFEB525)
    val patronPurple = Color(0xFF6046F5)
    val patronPurpleLight = Color(0xFFAFA2FA)
    val patronPurpleDark = patronPurple
}
