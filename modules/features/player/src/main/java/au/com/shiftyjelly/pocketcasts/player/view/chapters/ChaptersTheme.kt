package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.compose.material.CheckboxColors
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor

val LocalChaptersTheme = staticCompositionLocalOf<ChaptersTheme> { error("No default chapters theme") }

@Composable
fun ChaptersTheme(
    content: @Composable () -> Unit,
) {
    val chaptersTheme = ChaptersTheme(
        background = MaterialTheme.theme.colors.primaryUi01,
        divider = MaterialTheme.theme.colors.playerContrast06,
        headerTitle = MaterialTheme.theme.colors.primaryText02,
        headerToggle = MaterialTheme.theme.colors.primaryText01,
        chapterPlayed = MaterialTheme.theme.colors.primaryText02.copy(alpha = 0.4f),
        chapterNotPlayed = MaterialTheme.theme.colors.primaryText01,
        chapterTogglingSelected = MaterialTheme.theme.colors.primaryText01,
        chapterTogglingDeselected = MaterialTheme.theme.colors.primaryText02.copy(alpha = 0.4f),
        checkbox = CheckboxDefaults.colors(
            checkedColor = MaterialTheme.colors.secondary,
            uncheckedColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            disabledColor = MaterialTheme.colors.secondary.copy(alpha = 0.6f),
        ),
        progress = MaterialTheme.colors.primary.copy(alpha = 0.4f),
        progressBackground = MaterialTheme.colors.secondary.copy(alpha = 0.2f),
        linkIconBackground = MaterialTheme.colors.secondary.copy(alpha = 0.2f),
    )
    ChaptersTheme(chaptersTheme, content)
}

@Composable
fun ChaptersThemeForPlayer(
    theme: Theme,
    podcast: Podcast?,
    content: @Composable () -> Unit,
) {
    ChaptersThemeForPlayer(Color(theme.playerBackgroundColor(podcast)), content)
}

@Composable
private fun ChaptersThemeForPlayer(
    background: Color,
    content: @Composable () -> Unit,
) {
    val chaptersTheme = ChaptersTheme(
        background = background,
        divider = MaterialTheme.theme.colors.playerContrast06,
        headerTitle = MaterialTheme.theme.colors.playerContrast02,
        headerToggle = MaterialTheme.theme.colors.playerContrast01,
        chapterPlayed = MaterialTheme.theme.colors.playerContrast04,
        chapterNotPlayed = MaterialTheme.theme.colors.playerContrast01,
        chapterTogglingSelected = MaterialTheme.theme.colors.playerContrast01,
        chapterTogglingDeselected = MaterialTheme.theme.colors.playerContrast02,
        checkbox = CheckboxDefaults.colors(
            checkedColor = MaterialTheme.theme.colors.playerContrast01,
            uncheckedColor = MaterialTheme.theme.colors.playerContrast02,
            disabledColor = MaterialTheme.theme.colors.playerContrast02,
        ),
        progress = MaterialTheme.theme.colors.playerContrast05,
        progressBackground = MaterialTheme.theme.colors.playerContrast06,
        linkIconBackground = MaterialTheme.theme.colors.playerContrast05,
    )
    ChaptersTheme(chaptersTheme, content)
}

@Composable
private fun ChaptersTheme(
    chaptersTheme: ChaptersTheme,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalChaptersTheme provides chaptersTheme) {
        content()
    }
}

data class ChaptersTheme(
    val background: Color,
    val divider: Color,
    val headerTitle: Color,
    val headerToggle: Color,
    val chapterPlayed: Color,
    val chapterNotPlayed: Color,
    val chapterTogglingSelected: Color,
    val chapterTogglingDeselected: Color,
    val checkbox: CheckboxColors,
    val progress: Color,
    val progressBackground: Color,
    val linkIconBackground: Color,
)

internal enum class ChaptersPreviewTheme {
    Light,
    Dark,
    Player,
}

@Composable
internal fun ChaptersPreview(
    theme: ChaptersPreviewTheme,
    content: @Composable () -> Unit,
) {
    when (theme) {
        ChaptersPreviewTheme.Light -> AppThemeWithBackground(Theme.ThemeType.LIGHT) { ChaptersTheme(content) }

        ChaptersPreviewTheme.Dark -> AppThemeWithBackground(Theme.ThemeType.DARK) { ChaptersTheme(content) }

        ChaptersPreviewTheme.Player -> AppThemeWithBackground(Theme.ThemeType.DARK) {
            ChaptersThemeForPlayer(Color(ThemeColor.playerBackground01(Theme.ThemeType.DARK, PREVIEW_PODCAST_COLOR)), content)
        }
    }
}

private const val PREVIEW_PODCAST_COLOR = 0xFF1F5C99.toInt()
