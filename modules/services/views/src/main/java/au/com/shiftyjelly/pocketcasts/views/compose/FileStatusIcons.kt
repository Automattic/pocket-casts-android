package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.res.ColorStateList
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.ProgressBar
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeRowData
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import androidx.appcompat.R as AppCompatR
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun FileStatusIcons(
    data: UserEpisodeRowData,
    tint: Color,
    modifier: Modifier = Modifier,
    hideErrorDetails: Boolean = false,
) {
    val status = FileStatus.from(data, hideErrorDetails)
    val episode = data.episode
    val isGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived
    val imageAlpha = if (isGreyedOut) 0.5f else 1f
    val colors = MaterialTheme.theme.colors
    val captionColor = colors.primaryText02
    val textSizePx = with(LocalDensity.current) { StatusTextSize.roundToPx() }
    val lineMetrics = remember(textSizePx) {
        Paint().apply {
            textSize = textSizePx.toFloat()
            typeface = Typeface.SANS_SERIF
        }.fontMetricsInt
    }
    Row(
        verticalAlignment = TruncatedCenterVertically,
        modifier = modifier.padding(bottom = with(LocalDensity.current) { 12.dp.toPx().toInt().toDp() }),
    ) {
        if (data.isInUpNext) {
            StatusImage(IR.drawable.ic_upnext, colors.support01)
        }
        status.icon?.let { icon ->
            val iconTint = when (icon) {
                FileStatusIcon.PlaybackFailed -> null
                FileStatusIcon.Downloaded -> colors.support02
                else -> colors.primaryIcon02
            }
            StatusImage(icon.drawableId, iconTint, Modifier.imageViewAlpha(imageAlpha))
        }
        if (status.isUploaded) {
            GradientDrawableIcon(
                drawableId = IR.drawable.ic_cloud,
                startColor = colors.gradient01A,
                endColor = colors.gradient01E,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp)
                    .alpha(imageAlpha),
            )
        }
        if (data.hasBookmarks) {
            val bookmarksDescription = stringResource(LR.string.bookmarks)
            StatusImage(
                drawableId = IR.drawable.ic_bookmark,
                tint = tint,
                modifier = Modifier
                    .imageViewAlpha(imageAlpha)
                    .semantics { contentDescription = bookmarksDescription },
            )
        }
        status.progressCircle?.let { state ->
            ProgressCircle(state, captionColor, Modifier.padding(end = 4.dp).size(16.dp))
        }
        if (status.isBuffering) {
            val captionArgb = captionColor.toArgb()
            AndroidView(
                factory = { context -> ProgressBar(context, null, 0, AppCompatR.style.Widget_AppCompat_ProgressBar) },
                update = { progressBar -> progressBar.indeterminateTintList = ColorStateList.valueOf(captionArgb) },
                modifier = Modifier.padding(end = 4.dp).size(16.dp),
            )
        }
        Text(
            text = status.text.resolve(episode),
            color = if (isGreyedOut) captionColor.copy(alpha = 128 / 255f) else captionColor,
            style = TextStyle(fontSize = with(LocalDensity.current) { textSizePx.toSp() }, fontFamily = FontFamily.SansSerif, letterSpacing = 0.02.em),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .align(Alignment.Top)
                .textViewLineBox(lineMetrics),
        )
    }
}

private val StatusTextSize = 13.sp

private fun Modifier.textViewLineBox(metrics: Paint.FontMetricsInt): Modifier {
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, metrics.bottom - metrics.top) {
            placeable.place(0, -metrics.top - placeable[FirstBaseline])
        }
    }
}

private fun Modifier.imageViewAlpha(alpha: Float) = graphicsLayer(alpha = alpha, compositingStrategy = CompositingStrategy.ModulateAlpha)

private val TruncatedCenterVertically = Alignment.Vertical { size, space -> (space - size) / 2 }

@Composable
private fun StatusImage(
    @DrawableRes drawableId: Int,
    tint: Color?,
    modifier: Modifier = Modifier,
) {
    DrawableIcon(
        drawableId = drawableId,
        tint = tint,
        modifier = Modifier
            .padding(end = 4.dp)
            .size(16.dp)
            .then(modifier),
    )
}

@Composable
private fun FileStatusText.resolve(episode: UserEpisode): String = when (this) {
    is FileStatusText.Raw -> text
    is FileStatusText.Resource -> if (progress == null) stringResource(id) else stringResource(id, progress)
    FileStatusText.TimeLeft -> TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, LocalContext.current).text
}

@Preview
@Composable
private fun FileStatusIconsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Column {
            listOf(
                EpisodeDownloadStatus.Downloaded to UserEpisodeServerStatus.UPLOADED,
                EpisodeDownloadStatus.Downloading to UserEpisodeServerStatus.LOCAL,
                EpisodeDownloadStatus.DownloadFailed to UserEpisodeServerStatus.LOCAL,
                EpisodeDownloadStatus.DownloadNotRequested to UserEpisodeServerStatus.UPLOADING,
                EpisodeDownloadStatus.DownloadNotRequested to UserEpisodeServerStatus.QUEUED,
            ).forEach { (downloadStatus, serverStatus) ->
                FileStatusIcons(
                    data = UserEpisodeRowData(
                        episode = UserEpisode(
                            uuid = "uuid",
                            publishedDate = Date(),
                            duration = 3600.0,
                            downloadStatus = downloadStatus,
                            serverStatus = serverStatus,
                        ),
                        downloadProgress = 40,
                        uploadProgress = 60,
                        playbackState = PlaybackState(),
                        isInUpNext = true,
                        hasBookmarks = true,
                    ),
                    tint = MaterialTheme.theme.colors.primaryInteractive01,
                )
            }
        }
    }
}
