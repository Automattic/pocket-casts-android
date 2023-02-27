package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val PodcastItemIconSize = 64.dp

@Composable
fun PodcastItem(
    podcast: Podcast,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    iconSize: Dp = PodcastItemIconSize,
    subscribed: Boolean = false,
    showSubscribed: Boolean = false,
    showPlusIfUnsubscribed: Boolean = false,
    showDivider: Boolean = true,
    onSubscribeClick: (() -> Unit)? = null,
    maxLines: Int = 1,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .then(if (onClick == null) Modifier else Modifier.clickable { onClick() })
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            PodcastImage(
                uuid = podcast.uuid,
                modifier = modifier.size(iconSize)
                    .padding(top = 4.dp, end = 12.dp, bottom = 4.dp)
            )
            Column(
                modifier = modifier
                    .padding(end = 16.dp)
                    .weight(1f)
            ) {
                TextH40(
                    text = podcast.title,
                    maxLines = maxLines,
                    color = MaterialTheme.theme.colors.primaryText01
                )
                TextH50(
                    text = podcast.author,
                    maxLines = 1,
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (subscribed && showSubscribed) {
                Icon(
                    painter = painterResource(IR.drawable.ic_tick),
                    contentDescription = stringResource(LR.string.podcast_subscribed),
                    tint = MaterialTheme.theme.colors.support02
                )
            } else if (showPlusIfUnsubscribed) {
                Icon(
                    painter = painterResource(IR.drawable.plus_simple),
                    contentDescription = stringResource(LR.string.subscribe),
                    tint = MaterialTheme.theme.colors.primaryIcon02,
                    modifier = modifier
                        .then(if (onSubscribeClick == null) Modifier else Modifier.clickable { onSubscribeClick() })

                )
            }
        }
        if (showDivider) {
            HorizontalDivider(startIndent = 16.dp)
        }
    }
}
