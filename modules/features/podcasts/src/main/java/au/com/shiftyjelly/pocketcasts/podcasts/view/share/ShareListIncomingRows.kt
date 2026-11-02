package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.text.rememberTextAppearanceColor
import au.com.shiftyjelly.pocketcasts.compose.text.rememberThemeAttributeColor
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralPodcasts
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import coil3.compose.AsyncImage
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun ShareListIncomingHeader(
    title: String?,
    description: String?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextViewText(
            text = title.orEmpty(),
            fontSize = 20.sp,
            color = rememberThemeAttributeColor(android.R.attr.textColorPrimary),
            fontWeight = FontWeight.Medium,
            includeFontPadding = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp),
        )
        if (!description.isNullOrBlank()) {
            TextViewText(
                text = description,
                fontSize = 16.sp,
                color = rememberThemeAttributeColor(android.R.attr.textColorSecondary),
                includeFontPadding = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 6.dp, end = 16.dp),
            )
        }
        Spacer(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.theme.colors.primaryUi05),
        )
    }
}

@Composable
internal fun ShareListIncomingPodcastRow(
    podcast: Podcast,
    isSubscribed: Boolean,
    onClick: () -> Unit,
    onSubscribeClick: () -> Unit,
    onUnsubscribeClick: () -> Unit,
) {
    val context = LocalContext.current
    val imageRequest = remember(podcast.uuid, context) {
        PocketCastsImageRequestFactory(context).smallSize().themed().create(podcast)
    }
    val podcastName = podcast.title.ifEmpty { "podcast" }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.theme.colors.primaryUi01),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
                .semantics { contentDescription = podcastDescription(podcast, isSubscribed) },
        )
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(56.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(start = 88.dp, end = 61.dp)
                .clearAndSetSemantics {},
        ) {
            TextViewText(
                text = podcast.title,
                fontSize = 16.sp,
                color = rememberTextAppearanceColor(UR.attr.textSubtitle1),
                maxLines = 1,
                includeFontPadding = false,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            )
            TextViewText(
                text = podcast.author,
                fontSize = 15.sp,
                color = rememberTextAppearanceColor(UR.attr.textSubtitle2),
                letterSpacing = (-0.01).em,
                maxLines = 1,
                includeFontPadding = false,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(width = 64.dp, height = 72.dp)
                .clickable(
                    interactionSource = null,
                    indication = ripple(bounded = false),
                    onClick = if (isSubscribed) onUnsubscribeClick else onSubscribeClick,
                ),
        ) {
            DrawableImage(
                drawableId = if (isSubscribed) IR.drawable.ic_tick else IR.drawable.ic_add_black_24dp,
                contentDescription = if (isSubscribed) "Unsubscribe from $podcastName button." else "Subscribe to $podcastName button.",
                tint = if (isSubscribed) MaterialTheme.theme.colors.support02 else MaterialTheme.theme.colors.primaryIcon01,
            )
        }
        Spacer(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 88.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.theme.colors.primaryUi05),
        )
    }
}

@Composable
internal fun ShareListIncomingFooter(
    podcasts: List<Podcast>?,
    isSubscribedToAll: Boolean,
    onSubscribeToAllClick: (List<Podcast>) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        if (!podcasts.isNullOrEmpty()) {
            TextViewText(
                text = LocalResources.current.getStringPluralPodcasts(podcasts.size),
                fontSize = 14.sp,
                color = rememberThemeAttributeColor(android.R.attr.textColorSecondary),
                modifier = Modifier.padding(16.dp),
            )
            if (!isSubscribedToAll) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable { onSubscribeToAllClick(podcasts) }
                        .padding(16.dp),
                ) {
                    TextViewText(
                        text = stringResource(LR.string.podcasts_share_subscribe_to_all),
                        fontSize = 14.sp,
                        color = MaterialTheme.theme.colors.support02,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun podcastDescription(podcast: Podcast, isSubscribed: Boolean): String {
    if (podcast.title.isEmpty()) {
        return ""
    }
    return podcast.title + " by " + podcast.author + if (isSubscribed) " button. You are subscribed. " else " button."
}
