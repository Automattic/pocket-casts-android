package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bookmark.BookmarkRowColors
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun BookmarkDetailPage(
    displayTitle: String,
    aiSummary: String?,
    episodeTitle: String,
    podcastUuid: String,
    podcastTitle: String,
    timeSecs: Int,
    createdAtText: String,
    onPlayClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = MaterialTheme.theme
    val playerColors = theme.rememberPlayerColors()
    val colors = remember(theme.type, playerColors) {
        if (playerColors != null) {
            BookmarkRowColors.player(playerColors)
        } else {
            BookmarkRowColors.default(theme.colors)
        }
    }
    val playButtonBackground = if (playerColors != null) {
        playerColors.contrast01
    } else {
        theme.colors.primaryInteractive01
    }
    val playButtonText = if (playerColors != null) {
        playerColors.background01
    } else {
        theme.colors.primaryInteractive02
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        DragHandle(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp),
        )

        Header(
            buttonColor = colors.primaryText,
            onClose = onClose,
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            PodcastImage(
                uuid = podcastUuid,
                imageSize = 48.dp,
                elevation = null,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (podcastTitle.isNotEmpty()) {
                TextH70(
                    text = podcastTitle.uppercase(),
                    color = colors.secondaryText,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (episodeTitle.isNotEmpty()) {
                TextH70(
                    text = episodeTitle,
                    color = colors.secondaryText,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            TextH30(
                text = displayTitle,
                color = colors.primaryText,
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextH70(
                text = TimeHelper.formattedSeconds(timeSecs.toDouble()),
                color = colors.secondaryText,
            )

            if (!aiSummary.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                TextP40(
                    text = aiSummary,
                    color = colors.secondaryText,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextH70(
                text = createdAtText,
                color = colors.secondaryText,
            )

            Spacer(modifier = Modifier.height(20.dp))

            val formattedTime = TimeHelper.formattedSeconds(timeSecs.toDouble())
            RowButton(
                text = stringResource(LR.string.bookmark_play_from, formattedTime),
                onClick = onPlayClick,
                includePadding = false,
                leadingIcon = IR.drawable.ic_play,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = playButtonBackground,
                ),
                textColor = playButtonText,
            )
        }
    }
}

@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(36.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.theme.colors.primaryText01.copy(alpha = 0.3f)),
    )
}

@Composable
private fun Header(
    buttonColor: Color,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.offset(x = (-4).dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(LR.string.close),
                tint = buttonColor,
            )
        }
    }
}
