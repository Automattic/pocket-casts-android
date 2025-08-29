package au.com.shiftyjelly.pocketcasts.compose.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImageDeprecated
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ListPodcastSubscribeRow(
    uuid: String,
    title: String,
    author: String,
    subscribed: Boolean,
    onRowClick: () -> Unit,
    onSubscribeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { onRowClick() },
    ) {
        @Suppress("DEPRECATION")
        PodcastImageDeprecated(
            uuid = uuid,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .size(56.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
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
                    .padding(16.dp)
                    .size(24.dp),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_add_black_24dp),
                contentDescription = stringResource(LR.string.subscribe),
                tint = MaterialTheme.theme.colors.primaryIcon02,
                modifier = Modifier
                    .padding(8.dp)
                    .clip(CircleShape)
                    .clickable { onSubscribeClick() }
                    .padding(8.dp)
                    .size(24.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Preview
@Composable
private fun ListPodcastSubscribeRowPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppThemeWithBackground(theme) {
        Column {
            ListPodcastSubscribeRow(
                uuid = "uuid",
                title = "Podcast Title",
                author = "Podcast Author",
                subscribed = true,
                onRowClick = {},
                onSubscribeClick = {},
            )
            ListPodcastSubscribeRow(
                uuid = "uuid",
                title = "Podcast Title",
                author = "Podcast Author",
                subscribed = false,
                onRowClick = {},
                onSubscribeClick = {},
            )
        }
    }
}
