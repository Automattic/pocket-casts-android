package au.com.shiftyjelly.pocketcasts.discover.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PodcastRow(
    podcast: DiscoverPodcast,
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
) {
    Box(
        modifier = modifier,
    ) {
        val interactionSource = remember { MutableInteractionSource() }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(contentPadding),
        ) {
            @Suppress("DEPRECATION")
            PodcastImageDeprecated(
                uuid = podcast.uuid,
                cornerSize = 4.dp,
                modifier = Modifier.size(56.dp),
            )

            Labels(
                podcast = podcast,
                modifier = Modifier.weight(1f),
            )

            SubscribeIcon(
                podcast = podcast,
                interactionSource = interactionSource,
            )
        }
        if (!podcast.isSubscribed) {
            val paddingOffset = -contentPadding.calculateRightPadding(LocalLayoutDirection.current)
            SubscribeInteractionBox(
                podcast = podcast,
                interactionSource = interactionSource,
                onClick = onClickSubscribe,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = paddingOffset + (TouchTargetSize - IconSize) / 2),
            )
        }
    }
}

@Composable
internal fun PodcastRowPlaceholder(
    modifier: Modifier = Modifier,
) {
    PodcastRow(
        podcast = DiscoverPodcastPlaceholder,
        onClickSubscribe = {},
        modifier = modifier,
    )
}

@Composable
private fun Labels(
    podcast: DiscoverPodcast,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        val title = podcast.title
        if (title != null) {
            TextP40(
                text = title,
                maxLines = 1,
            )
        }

        val author = podcast.author
        if (author != null) {
            TextP50(
                text = author,
                maxLines = 1,
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
    }
}

@Composable
private fun SubscribeIcon(
    podcast: DiscoverPodcast,
    interactionSource: InteractionSource,
) {
    val rippleColor = MaterialTheme.theme.colors.primaryText01
    val ripple = remember(rippleColor) { ripple(color = rippleColor, bounded = false) }

    Image(
        painter = if (podcast.isSubscribed) {
            painterResource(R.drawable.ic_check_black_24dp)
        } else {
            painterResource(R.drawable.ic_add_black_24dp)
        },
        contentDescription = null,
        colorFilter = ColorFilter.tint(
            if (podcast.isSubscribed) {
                MaterialTheme.theme.colors.support02
            } else {
                MaterialTheme.theme.colors.primaryIcon02
            },
        ),
        modifier = Modifier
            .size(IconSize)
            .indication(interactionSource, ripple),
    )
}

@Composable
private fun SubscribeInteractionBox(
    podcast: DiscoverPodcast,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(TouchTargetSize)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(LR.string.subscribe_to, podcast.title.orEmpty()),
            ),
    )
}

private val IconSize = 24.dp
private val TouchTargetSize = 48.dp
internal val DiscoverPodcastPlaceholder = DiscoverPodcast(
    uuid = "",
    title = "",
    author = "",
    description = "",
    url = "",
    category = "",
    language = "",
    mediaType = "",
    isSubscribed = false,
)
internal val DiscoverPodcastPreview = DiscoverPodcast(
    uuid = "uuid",
    title = "Title",
    author = "Author",
    description = "",
    url = "",
    category = "",
    language = "",
    mediaType = "",
    isSubscribed = false,
)

@Preview
@Composable
private fun PodcastRowPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        Column {
            PodcastRow(
                podcast = DiscoverPodcastPreview,
                onClickSubscribe = {},
            )
            PodcastRow(
                podcast = DiscoverPodcastPreview.copy(isSubscribed = true),
                onClickSubscribe = {},
            )
            PodcastRow(
                podcast = DiscoverPodcastPreview.copy(title = "Lorem ipsum dolor sit amet consectetur adipiscing elit"),
                onClickSubscribe = {},
            )
            PodcastRow(
                podcast = DiscoverPodcastPreview.copy(author = "Lorem ipsum dolor sit amet consectetur adipiscing elit"),
                onClickSubscribe = {},
            )
            PodcastRow(
                podcast = DiscoverPodcastPreview.copy(
                    title = "Lorem ipsum dolor sit amet consectetur adipiscing elit",
                    author = "Lorem ipsum dolor sit amet consectetur adipiscing elit",
                ),
                onClickSubscribe = {},
            )
        }
    }
}

@Preview
@Composable
private fun PodcastRowThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Column {
            PodcastRow(
                podcast = DiscoverPodcastPreview,
                onClickSubscribe = {},
            )
            PodcastRow(
                podcast = DiscoverPodcastPreview.copy(isSubscribed = true),
                onClickSubscribe = {},
            )
        }
    }
}
