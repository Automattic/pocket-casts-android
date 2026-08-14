package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvModal
import au.com.shiftyjelly.pocketcasts.component.TvModalSurface
import au.com.shiftyjelly.pocketcasts.qr.rememberQrPainter
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvDownloadAppModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvModal(
        onDismissRequest = onDismissRequest,
        width = 600.dp,
        modifier = modifier,
    ) {
        TvDownloadAppModalContent(onDone = onDismissRequest)
    }
}

@Composable
private fun ColumnScope.TvDownloadAppModalContent(
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Text(
        text = stringResource(LR.string.tv_playlists_download_title),
        color = MaterialTheme.tvColors.textPrimary,
        style = MaterialTheme.tvTypography.headline.copy(textAlign = TextAlign.Center),
    )
    Text(
        text = stringResource(LR.string.tv_playlists_download_subtitle),
        color = MaterialTheme.tvColors.textSecondary,
        style = MaterialTheme.tvTypography.body.copy(textAlign = TextAlign.Center),
    )
    Image(
        painter = rememberQrPainter(content = DOWNLOAD_URL, size = QrCodeSize),
        contentDescription = null,
        modifier = Modifier
            .padding(vertical = 16.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(10.dp)
            .size(QrCodeSize),
    )
    Text(
        text = DOWNLOAD_URL_LABEL,
        color = MaterialTheme.tvColors.textSecondary,
        style = MaterialTheme.tvTypography.body.copy(textAlign = TextAlign.Center),
    )
    Button(
        onClick = onDone,
        colors = TvButtonDefaults.filledButtonColors(),
        modifier = Modifier
            .padding(top = 16.dp)
            .focusRequester(focusRequester),
    ) {
        Text(stringResource(LR.string.done), style = MaterialTheme.tvTypography.caption1)
    }
}

private const val DOWNLOAD_URL = "https://pocketcasts.com/downloads"
private val DOWNLOAD_URL_LABEL = DOWNLOAD_URL.removePrefix("https://")
private val QrCodeSize = 144.dp

@Preview
@Composable
private fun TvDownloadAppModalPreview() {
    TvTheme {
        TvModalSurface(width = 600.dp) {
            TvDownloadAppModalContent(onDone = {})
        }
    }
}
