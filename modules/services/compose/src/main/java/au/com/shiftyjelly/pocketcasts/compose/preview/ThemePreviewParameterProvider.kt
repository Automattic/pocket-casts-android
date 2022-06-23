package au.com.shiftyjelly.pocketcasts.compose.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class ThemePreviewParameterProvider : PreviewParameterProvider<Theme.ThemeType> {
    override val values = sequenceOf(
        Theme.ThemeType.LIGHT,
        Theme.ThemeType.DARK,
        Theme.ThemeType.ROSE,
        Theme.ThemeType.INDIGO,

//        Theme.ThemeType.EXTRA_DARK,
//        Theme.ThemeType.DARK_CONTRAST,
//        Theme.ThemeType.LIGHT_CONTRAST,
//        Theme.ThemeType.ELECTRIC,
//        Theme.ThemeType.CLASSIC_LIGHT,
//        Theme.ThemeType.RADIOACTIVE,
    )
}
