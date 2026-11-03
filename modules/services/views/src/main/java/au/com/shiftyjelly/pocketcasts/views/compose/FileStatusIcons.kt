package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.res.ColorStateList
import android.widget.ProgressBar
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
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
    FileStatusIcons(data, EpisodeRowDefaults.colors(tint = tint), hideErrorDetails, modifier)
}

@Composable
internal fun FileStatusIcons(
    data: UserEpisodeRowData,
    colors: EpisodeRowColors,
    hideErrorDetails: Boolean,
    modifier: Modifier = Modifier,
) {
    val status = FileStatus.from(data, hideErrorDetails)
    val episode = data.episode
    StatusLine(
        text = status.text.resolve(episode),
        bottomPadding = with(LocalDensity.current) { 12.dp.toPx().toInt().toDp() },
        isGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived,
        colors = colors,
        modifier = modifier,
        isInUpNext = data.isInUpNext,
        iconId = status.icon?.drawableId,
        iconTint = when (status.icon) {
            FileStatusIcon.PlaybackFailed, null -> null
            FileStatusIcon.Downloaded -> colors.theme.support02
            else -> colors.theme.primaryIcon02
        },
        isUploaded = status.isUploaded,
        hasBookmarks = data.hasBookmarks,
        progressCircle = status.progressCircle,
        isBuffering = status.isBuffering,
    )
}

@Composable
internal fun StatusLine(
    text: String,
    bottomPadding: Dp,
    isGreyedOut: Boolean,
    colors: EpisodeRowColors,
    modifier: Modifier = Modifier,
    isInUpNext: Boolean = false,
    @DrawableRes iconId: Int? = null,
    iconTint: Color? = null,
    isUploaded: Boolean = false,
    hasBookmarks: Boolean = false,
    progressCircle: ProgressCircleState? = null,
    isBuffering: Boolean = false,
) {
    val imageAlpha = if (isGreyedOut) 0.5f else 1f
    val captionColor = colors.theme.primaryText02
    Row(
        verticalAlignment = TruncatedCenterVertically,
        modifier = modifier.padding(bottom = bottomPadding),
    ) {
        if (isInUpNext) {
            StatusImage(IR.drawable.ic_upnext, colors.theme.support01)
        }
        if (iconId != null) {
            StatusImage(iconId, iconTint, Modifier.imageViewAlpha(imageAlpha))
        }
        if (isUploaded) {
            GradientDrawableIcon(
                drawableId = IR.drawable.ic_cloud,
                startColor = colors.theme.gradient01A,
                endColor = colors.theme.gradient01E,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp)
                    .alpha(imageAlpha),
            )
        }
        if (hasBookmarks) {
            val bookmarksDescription = stringResource(LR.string.bookmarks)
            StatusImage(
                drawableId = IR.drawable.ic_bookmark,
                tint = colors.tint,
                modifier = Modifier
                    .imageViewAlpha(imageAlpha)
                    .semantics { contentDescription = bookmarksDescription },
            )
        }
        progressCircle?.let { state ->
            ProgressCircle(state, captionColor, Modifier.padding(end = 4.dp).size(16.dp))
        }
        if (isBuffering) {
            val captionArgb = captionColor.toArgb()
            AndroidView(
                factory = { context -> ProgressBar(context, null, 0, AppCompatR.style.Widget_AppCompat_ProgressBar) },
                update = { progressBar -> progressBar.indeterminateTintList = ColorStateList.valueOf(captionArgb) },
                modifier = Modifier.padding(end = 4.dp).size(16.dp),
            )
        }
        TextViewLine(
            text = AnnotatedString(text),
            size = StatusTextSize,
            color = colors.secondaryText(isGreyedOut),
            letterSpacing = 0.02.em,
            modifier = Modifier
                .weight(1f, fill = false)
                .align(Alignment.Top),
        )
    }
}

private val StatusTextSize = 13.sp

internal fun Modifier.imageViewAlpha(alpha: Float) = graphicsLayer(alpha = alpha, compositingStrategy = CompositingStrategy.ModulateAlpha)

internal val TruncatedCenterVertically = Alignment.Vertical { size, space -> (space - size) / 2 }

@Composable
internal fun StatusImage(
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
fun fileStatusText(data: UserEpisodeRowData, hideErrorDetails: Boolean = false): String {
    return FileStatus.from(data, hideErrorDetails).text.resolve(data.episode)
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
