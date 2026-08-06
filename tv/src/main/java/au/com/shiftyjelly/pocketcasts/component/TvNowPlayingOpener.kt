package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.runtime.staticCompositionLocalOf

val LocalOpenNowPlaying = staticCompositionLocalOf<() -> Unit> { {} }
