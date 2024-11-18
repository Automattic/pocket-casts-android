package au.com.shiftyjelly.pocketcasts.compose.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class ThemePreviewParameterProvider : PreviewParameterProvider<Theme.ThemeType> {
    override val values = Theme.ThemeType.entries.asSequence()
}
