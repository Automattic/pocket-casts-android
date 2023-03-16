package au.com.shiftyjelly.pocketcasts.compose.podcast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PodcastSubscribeImage(
    podcastUuid: String,
    podcastTitle: String,
    podcastSubscribed: Boolean,
    onSubscribeClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subscribeButtonSize: Dp = 20.dp,
    shadowSize: Dp = 1.dp,
    subscribeOnPodcastTap: Boolean = true,
) {
    val onClickLabel = if (podcastSubscribed) {
        stringResource(LR.string.unsubscribe)
    } else {
        stringResource(LR.string.subscribe)
    }
    var rootModifier = modifier
        .aspectRatio(1f)
        .semantics(mergeDescendants = true) {}
    if (subscribeOnPodcastTap) {
        rootModifier = if (onSubscribeClick != null) {
            rootModifier
                .clickable(
                    onClickLabel = onClickLabel,
                    onClick = onSubscribeClick
                )
        } else {
            rootModifier
        }
    }
    BoxWithConstraints(
        modifier = rootModifier,
        contentAlignment = Alignment.Center
    ) {
        PodcastImage(
            uuid = podcastUuid,
            title = podcastTitle,
            showTitle = true,
            modifier = Modifier.fillMaxSize()
        )

        val buttonBackgroundColor = Color.Black.copy(alpha = 0.4f)

        val iconSize = subscribeButtonSize / 1.25f
        var iconModifier = Modifier.size(iconSize)
        if (!subscribeOnPodcastTap) {
            iconModifier = if (onSubscribeClick != null) {
                iconModifier.clickable(
                    onClickLabel = onClickLabel,
                    onClick = onSubscribeClick
                )
            } else {
                iconModifier
            }
        }
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (podcastSubscribed) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .shadow(shadowSize, shape = CircleShape)
                        .size(subscribeButtonSize)
                        .clip(CircleShape)
                        .background(buttonBackgroundColor)
                ) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_tick),
                        contentDescription = stringResource(LR.string.podcast_subscribed),
                        tint = Color.White,
                        modifier = iconModifier
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .shadow(shadowSize, shape = CircleShape)
                        .size(subscribeButtonSize)
                        .clip(CircleShape)
                        .background(buttonBackgroundColor)
                ) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_add_black_24dp),
                        contentDescription = stringResource(LR.string.podcast_not_subscribed),
                        tint = Color.White,
                        modifier = iconModifier
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PodcastSelectImagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        PodcastSubscribeImage(
            podcastUuid = "e7a6f7d0-02f2-0133-1c51-059c869cc4eb",
            podcastTitle = "A Great Podcast Title",
            podcastSubscribed = false,
            onSubscribeClick = {},
            modifier = Modifier
        )
    }
}
