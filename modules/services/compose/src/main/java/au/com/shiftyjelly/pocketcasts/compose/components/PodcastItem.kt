package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
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
    tintColor: Color? = null,
    iconSize: Dp = PodcastItemIconSize,
    subscribed: Boolean = false,
    showSubscribed: Boolean = false,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .then(if (onClick == null) Modifier else Modifier.clickable { onClick() })
        ) {
            Box(modifier = Modifier.padding(top = 4.dp, end = 12.dp, bottom = 4.dp)) {
                PodcastImage(
                    uuid = podcast.uuid,
                    modifier = Modifier.size(iconSize)
                )
            }
            Column(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .weight(1f)
            ) {
                TextP40(
                    text = podcast.title,
                    maxLines = 1,
                    color = tintColor ?: MaterialTheme.theme.colors.primaryText01
                )
                TextP50(
                    text = podcast.author,
                    maxLines = 1,
                    color = tintColor ?: MaterialTheme.theme.colors.primaryText02
                )
            }
            if (subscribed && showSubscribed) {
                Icon(
                    painter = painterResource(IR.drawable.ic_tick),
                    contentDescription = stringResource(LR.string.podcast_subscribed),
                    tint = tintColor ?: MaterialTheme.theme.colors.support02
                )
            }
        }
        if (showDivider) {
            HorizontalDivider()
        }
    }
}
