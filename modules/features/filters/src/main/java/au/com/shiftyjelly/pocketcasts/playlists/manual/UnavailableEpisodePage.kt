package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun UnavailableEpisodePage(
    onClickRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(IR.drawable.ic_exclamation_circle),
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon01),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
        Spacer(
            modifier = Modifier.height(24.dp),
        )
        TextH20(
            text = "Episode unavailable",
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        TextP40(
            text = "The podcast creator deleted this episode. It will stay in your playlist until you remove it.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.theme.colors.primaryText02,
        )
        Spacer(
            modifier = Modifier.height(40.dp),
        )
        RowButton(
            text = "Remove from playlist",
            onClick = onClickRemove,
            includePadding = false,
        )
    }
}

@Preview
@Composable
private fun UnavailableEpisodePagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        UnavailableEpisodePage(
            onClickRemove = {},
        )
    }
}
