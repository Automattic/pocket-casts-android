package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.Pill
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedPlayButton
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.profile.R
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeRowData
import au.com.shiftyjelly.pocketcasts.views.compose.EpisodeRowArtwork
import au.com.shiftyjelly.pocketcasts.views.compose.FileStatusIcons
import coil3.request.ImageRequest
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal interface CloudFileListener {
    fun onPlayClick(episode: UserEpisode)

    fun onPauseClick()

    fun onUpNextClick(episode: UserEpisode, inUpNext: Boolean)

    fun onPlayedClick(episode: UserEpisode)

    fun onBookmarksClick()

    fun onEditClick(episode: UserEpisode)

    fun onCloudClick(episode: UserEpisode)

    fun onLockedCloudClick()

    fun onDeleteClick(episode: UserEpisode)
}

internal enum class CloudRowVisibility(val showCloud: Boolean, val showLocked: Boolean) {
    Unknown(showCloud = true, showLocked = true),
    Cloud(showCloud = true, showLocked = false),
    Locked(showCloud = false, showLocked = true),
}

@Composable
internal fun CloudFilePage(
    state: BottomSheetState,
    rowData: UserEpisodeRowData?,
    artwork: ImageRequest,
    cloudRowVisibility: CloudRowVisibility,
    listener: CloudFileListener,
    modifier: Modifier = Modifier,
) {
    val episode = state.episode
    val colors = MaterialTheme.theme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Pill(
            modifier = Modifier
                .padding(top = 12.dp)
                .align(Alignment.CenterHorizontally),
        )
        ErrorCard(episode)
        Row(modifier = Modifier.padding(top = 24.dp, start = 16.dp)) {
            Box(modifier = Modifier.size(56.dp)) {
                EpisodeRowArtwork(artwork, alpha = 1f, elevation = 0f)
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                TextH30(
                    text = episode.title,
                    fontWeight = FontWeight.W500,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 16.dp),
                )
                FileStatusIcons(
                    data = rowData ?: UserEpisodeRowData(episode, 0, 0, PlaybackState(), isInUpNext = false, hasBookmarks = false),
                    tint = colors.primaryIcon01,
                    hideErrorDetails = true,
                    modifier = Modifier.padding(top = 8.dp, end = 88.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .zIndex(1f)
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(1.dp),
        ) {
            HorizontalDivider()
            PlayButton(
                state = state,
                listener = listener,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .requiredSize(64.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (state.inUpNext) {
            ActionRow(
                iconId = IR.drawable.ic_upnext_remove,
                text = stringResource(LR.string.profile_cloud_remove_from_up_next),
                onClick = { listener.onUpNextClick(episode, inUpNext = true) },
            )
        } else {
            ActionRow(
                iconId = IR.drawable.ic_upnext_playnext,
                text = stringResource(LR.string.profile_cloud_add_to_up_next),
                onClick = { listener.onUpNextClick(episode, inUpNext = false) },
            )
        }
        ActionRow(
            iconId = if (episode.isFinished) IR.drawable.ic_markasunplayed else IR.drawable.ic_markasplayed,
            text = stringResource(if (episode.isFinished) LR.string.mark_as_unplayed else LR.string.mark_as_played),
            onClick = { listener.onPlayedClick(episode) },
        )
        ActionRow(IR.drawable.ic_bookmark, stringResource(LR.string.bookmarks), onClick = listener::onBookmarksClick)
        ActionRow(R.drawable.ic_rename, stringResource(LR.string.profile_files_edit), onClick = { listener.onEditClick(episode) })
        if (cloudRowVisibility.showCloud) {
            CloudRow(episode, onClick = { listener.onCloudClick(episode) })
        }
        if (cloudRowVisibility.showLocked) {
            ActionRow(
                iconId = R.drawable.ic_upload_file,
                text = stringResource(LR.string.profile_cloud_upload),
                onClick = listener::onLockedCloudClick,
                modifier = Modifier.alpha(0.5f),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        ActionRow(
            iconId = IR.drawable.ic_delete,
            text = stringResource(LR.string.profile_files_delete),
            onClick = { listener.onDeleteClick(episode) },
            color = colors.support05,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun PlayButton(state: BottomSheetState, listener: CloudFileListener, modifier: Modifier = Modifier) {
    var pauseClicked by remember(state.isPlaying) { mutableStateOf(false) }
    val colors = MaterialTheme.theme.colors
    AnimatedPlayButton(
        isPlaying = state.isPlaying && !pauseClicked,
        animate = pauseClicked,
        onClick = {
            if (state.isPlaying) {
                pauseClicked = true
                listener.onPauseClick()
            } else {
                listener.onPlayClick(state.episode)
            }
        },
        iconSize = 24.dp,
        iconTint = colors.primaryInteractive02,
        circleColor = colors.primaryIcon01,
        modifier = modifier,
    )
}

@Composable
private fun ErrorCard(episode: UserEpisode) {
    val (title, detail) = when {
        episode.downloadErrorDetails != null -> LR.string.profile_cloud_download_failed to episode.downloadErrorDetails
        episode.uploadErrorDetails != null -> LR.string.profile_cloud_upload_failed to episode.uploadErrorDetails
        episode.playErrorDetails != null -> LR.string.profile_cloud_playback_failed to episode.playErrorDetails
        else -> return
    }
    val colors = MaterialTheme.theme.colors
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
            .fillMaxWidth()
            .background(colors.primaryUi06, shape)
            .border(1.dp, colors.primaryUi05, shape),
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_failedwarning),
            contentDescription = null,
            tint = colors.primaryIcon03,
            modifier = Modifier
                .padding(start = 15.dp, top = 10.dp, end = 9.dp)
                .size(24.dp),
        )
        Column(modifier = Modifier.padding(end = 48.dp, top = 10.dp, bottom = 18.dp)) {
            TextP40(text = stringResource(title))
            TextP50(
                text = detail.orEmpty(),
                color = colors.primaryText02,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun CloudRow(episode: UserEpisode, onClick: () -> Unit) {
    val text = when (episode.serverStatus) {
        UserEpisodeServerStatus.LOCAL -> stringResource(LR.string.profile_cloud_upload)

        UserEpisodeServerStatus.UPLOADING, UserEpisodeServerStatus.WAITING_FOR_WIFI, UserEpisodeServerStatus.QUEUED -> stringResource(LR.string.profile_cloud_cancel_upload)

        UserEpisodeServerStatus.UPLOADED -> stringResource(
            when {
                episode.isDownloaded -> LR.string.profile_cloud_remove
                episode.isDownloading -> LR.string.cancel_download
                else -> LR.string.download
            },
        )

        UserEpisodeServerStatus.MISSING -> ""
    }
    val iconId = when (episode.serverStatus) {
        UserEpisodeServerStatus.LOCAL, UserEpisodeServerStatus.MISSING -> R.drawable.ic_upload_file
        UserEpisodeServerStatus.UPLOADING, UserEpisodeServerStatus.WAITING_FOR_WIFI, UserEpisodeServerStatus.QUEUED -> IR.drawable.ic_downloading
        UserEpisodeServerStatus.UPLOADED -> if (episode.isDownloaded) IR.drawable.ic_remove_from_cloud else IR.drawable.ic_download
    }
    ActionRow(iconId, text, onClick = onClick)
}

@Composable
private fun ActionRow(
    @DrawableRes iconId: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
    trailing: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.theme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = null,
            tint = color ?: colors.primaryIcon01,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(24.dp))
        TextH40(text = text, color = color ?: colors.primaryText01)
        trailing()
    }
}
