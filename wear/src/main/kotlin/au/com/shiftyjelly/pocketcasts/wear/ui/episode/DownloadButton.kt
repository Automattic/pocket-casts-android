package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.DownloadButtonState
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme

@Composable
fun DownloadButton(
    tint: Color,
    onClick: () -> Unit,
    downloadButtonState: DownloadButtonState,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clickable(
            onClick = onClick,
        ),
    ) {

        when (downloadButtonState) {
            is DownloadButtonState.Downloading -> downloadButtonState.progressPercent
            is DownloadButtonState.Queued -> 0f
            is DownloadButtonState.Downloaded,
            is DownloadButtonState.NotDownloaded,
            DownloadButtonState.Errored -> null
        }?.let { progressPercent ->
            CircularProgressIndicator(
                progress = progressPercent,
                strokeWidth = 2.dp,
                indicatorColor = tint,
                trackColor = tint.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize()
            )
        }

        Icon(
            painter = painterResource(
                when (downloadButtonState) {
                    is DownloadButtonState.Downloaded -> R.drawable.ic_downloaded
                    DownloadButtonState.Queued,
                    is DownloadButtonState.Downloading -> R.drawable.ic_downloading

                    DownloadButtonState.Errored -> R.drawable.ic_retry
                    is DownloadButtonState.NotDownloaded -> R.drawable.ic_download
                }
            ),
            tint = tint,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview
@Composable
private fun DownloadingPreview() {
    WearAppTheme {
        Column {
            listOf(
                DownloadButtonState.NotDownloaded("3 MB"),
                DownloadButtonState.Queued,
                DownloadButtonState.Downloading(0.4f),
                DownloadButtonState.Downloaded("3 MB"),
                DownloadButtonState.Errored,
            ).forEach { state ->
                DownloadButton(
                    tint = Color.White,
                    onClick = {},
                    downloadButtonState = state,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
