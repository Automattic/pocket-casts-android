package au.com.shiftyjelly.pocketcasts.compose.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable

@get:Composable
val WindowInsets.Companion.verticalNavigationBars get() = navigationBars.only(WindowInsetsSides.Vertical)
