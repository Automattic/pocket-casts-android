package au.com.shiftyjelly.pocketcasts.compose.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ListPodcastSubscribeRow(
    uuid: String,
    title: String,
    author: String,
    subscribed: Boolean,
    onRowClick: (uuid: String) -> Unit,
    onSubscribeClick: (uuid: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { onRowClick(uuid) },
    ) {
        Box(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            PodcastImage(
                uuid = uuid,
                modifier = Modifier.size(56.dp),
            )
        }
        Column(
            modifier = Modifier
                .padding(end = 8.dp)
                .weight(1f),
        ) {
            TextP40(
                text = title,
                maxLines = 1,
            )
            TextP50(
                text = author,
                maxLines = 1,
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
        if (subscribed) {
            Icon(
                painter = painterResource(R.drawable.ic_check_black_24dp),
                contentDescription = stringResource(LR.string.podcast_subscribed),
                tint = MaterialTheme.theme.colors.support02,
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_add_black_24dp),
                contentDescription = stringResource(LR.string.subscribe),
                tint = MaterialTheme.theme.colors.primaryIcon02,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onSubscribeClick(uuid) }
                    .size(24.dp),
            )
        }
    }
}
