package au.com.shiftyjelly.pocketcasts.widget.data

import androidx.compose.runtime.staticCompositionLocalOf
import au.com.shiftyjelly.pocketcasts.analytics.SourceView

internal val LocalSource = staticCompositionLocalOf { SourceView.UNKNOWN }
