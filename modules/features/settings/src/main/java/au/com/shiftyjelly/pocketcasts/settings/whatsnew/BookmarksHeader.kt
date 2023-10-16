package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowCloseButton
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun BookmarksHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(IR.drawable.whatsnew_bookmarks),
            contentDescription = stringResource(LR.string.bookmarks),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        RowCloseButton(
            onClose = onClose,
        )
    }
}

@Composable
@Preview
private fun BookmarksHeaderPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        BookmarksHeader(
            onClose = {}
        )
    }
}
