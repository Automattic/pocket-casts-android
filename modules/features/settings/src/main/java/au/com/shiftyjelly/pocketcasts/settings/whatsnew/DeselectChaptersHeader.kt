package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowCloseButton
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

private const val PaddingTop = 80
private const val ImageHeight = 150

@Composable
fun DeselectChaptersHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    fullModal: Boolean = true,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .clipToBounds()
                .height((ImageHeight * 2 + PaddingTop).dp)
                .then(if (fullModal) Modifier else Modifier.padding(top = PaddingTop.dp)),
        ) {
            Column {
                Image(
                    painter = painterResource(IR.drawable.whats_new_deselect_chapters_unselected),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = painterResource(IR.drawable.whats_new_deselect_chapters_selected),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryInteractive01),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = painterResource(IR.drawable.whats_new_deselect_chapters_unselected),
                    contentDescription = null,
                )
            }
        }

        if (!fullModal) {
            RowCloseButton(
                onClose = onClose,
                tintColor = if (MaterialTheme.theme.isLight) Color.Black else Color.White,
            )
        }
    }
}

@Preview
@Composable
fun DeselectChaptersHeadersPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        DeselectChaptersHeader(
            onClose = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
