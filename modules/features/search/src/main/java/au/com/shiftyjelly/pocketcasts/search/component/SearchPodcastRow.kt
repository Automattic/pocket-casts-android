package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SearchPodcastRow(podcast: Podcast, subscribed: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.theme.colors.primaryUi01)
                .padding(horizontal = 16.dp)
                .then(if (onClick == null) Modifier else Modifier.clickable { onClick() })
        ) {
            Box(modifier = Modifier.padding(top = 4.dp, end = 12.dp, bottom = 4.dp)) {
                PodcastImage(
                    uuid = podcast.uuid,
                    modifier = Modifier.size(64.dp)
                )
            }
            Column(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .weight(1f)
            ) {
                TextP40(
                    text = podcast.title,
                    maxLines = 1
                )
                TextP50(
                    text = podcast.author,
                    maxLines = 1,
                    color = MaterialTheme.theme.colors.primaryText02
                )
            }
            if (subscribed) {
                Icon(
                    painter = painterResource(IR.drawable.ic_tick),
                    contentDescription = stringResource(LR.string.podcast_subscribed),
                    tint = MaterialTheme.theme.colors.support02
                )
            }
        }
        HorizontalDivider()
    }
}
