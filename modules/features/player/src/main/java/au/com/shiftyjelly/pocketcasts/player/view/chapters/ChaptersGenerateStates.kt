package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBannerColors
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChaptersNoChaptersYet(
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChaptersBannerContainer(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NoContentBanner(
                title = stringResource(LR.string.chapters_generated_none_yet_title),
                body = stringResource(LR.string.chapters_generated_none_yet_message),
                iconResourceId = IR.drawable.ic_book_open,
                primaryButtonText = stringResource(LR.string.chapters_generated_generate),
                onPrimaryButtonClick = onGenerateClick,
                colors = chaptersBannerColors(),
            )
            TextP60(
                text = stringResource(LR.string.chapters_generated_ai_note),
                color = LocalChaptersTheme.current.headerTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(top = 12.dp)
                    .widthIn(max = 328.dp),
            )
        }
    }
}

@Composable
fun ChaptersNoGeneratedChapters(
    modifier: Modifier = Modifier,
) {
    ChaptersBannerContainer(modifier) {
        NoContentBanner(
            title = stringResource(LR.string.chapters_generated_none_title),
            body = "",
            iconResourceId = IR.drawable.ic_book_open,
            colors = chaptersBannerColors(),
        )
    }
}

@Composable
private fun ChaptersBannerContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(LocalChaptersTheme.current.background)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 32.dp),
    ) {
        content()
    }
}

@Composable
private fun chaptersBannerColors(): NoContentBannerColors {
    val chaptersTheme = LocalChaptersTheme.current
    return NoContentBannerColors.default(MaterialTheme.theme.colors).copy(
        primaryText = chaptersTheme.headerToggle,
        secondaryText = chaptersTheme.headerTitle,
        icon = chaptersTheme.headerToggle,
        button = chaptersTheme.headerToggle,
        buttonText = chaptersTheme.background,
    )
}

@Preview(name = "Light")
@Composable
private fun ChaptersNoChaptersYetLightPreview() = ChaptersNoChaptersYetPreview(Theme.ThemeType.LIGHT)

@Preview(name = "Dark")
@Composable
private fun ChaptersNoChaptersYetDarkPreview() = ChaptersNoChaptersYetPreview(Theme.ThemeType.DARK)

@Composable
private fun ChaptersNoChaptersYetPreview(theme: Theme.ThemeType) {
    AppThemeWithBackground(theme) {
        ChaptersTheme {
            ChaptersNoChaptersYet(onGenerateClick = {})
        }
    }
}

@Preview(name = "Light")
@Composable
private fun ChaptersNoGeneratedChaptersLightPreview() = ChaptersNoGeneratedChaptersPreview(Theme.ThemeType.LIGHT)

@Preview(name = "Dark")
@Composable
private fun ChaptersNoGeneratedChaptersDarkPreview() = ChaptersNoGeneratedChaptersPreview(Theme.ThemeType.DARK)

@Composable
private fun ChaptersNoGeneratedChaptersPreview(theme: Theme.ThemeType) {
    AppThemeWithBackground(theme) {
        ChaptersTheme {
            ChaptersNoGeneratedChapters()
        }
    }
}
