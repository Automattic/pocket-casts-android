package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.folder.FolderImage
import au.com.shiftyjelly.pocketcasts.compose.images.CountBadge
import au.com.shiftyjelly.pocketcasts.compose.images.CountBadgeStyle
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.size.Precision
import kotlin.math.abs
import kotlin.math.min

internal class PodcastsContent(
    val items: List<FolderItem>,
    val layout: PodcastGridLayoutType,
    val badgeType: BadgeType,
    val podcastUuidToBadge: Map<String, Int>,
    val activeAd: BlazeAd?,
)

@Composable
internal fun FolderItemCell(
    item: FolderItem,
    content: PodcastsContent,
    isLifted: Boolean,
    imageRequestFactory: PocketCastsImageRequestFactory,
    onPodcastClick: (Podcast) -> Unit,
    onFolderClick: (Folder) -> Unit,
) {
    val isList = content.layout == PodcastGridLayoutType.LIST_VIEW
    when (item) {
        is FolderItem.Podcast -> {
            val unplayedCount = content.podcastUuidToBadge[item.podcast.uuid] ?: 0
            val badgeCount = when (content.badgeType) {
                BadgeType.OFF -> 0
                BadgeType.ALL_UNFINISHED -> unplayedCount
                BadgeType.LATEST_EPISODE -> min(1, unplayedCount)
            }.coerceAtMost(99)
            val badgeMessage = if (content.badgeType == BadgeType.OFF) "" else "$unplayedCount new episodes. "
            val description = "${item.podcast.title}. $badgeMessage Open podcast."
            val onClick = { onPodcastClick(item.podcast) }
            if (isList) {
                PodcastListRow(item.podcast, badgeCount, content.badgeType, description, isLifted, onClick)
            } else {
                PodcastGridCell(item.podcast, badgeCount, content.badgeType, content.layout, description, imageRequestFactory, onClick)
            }
        }

        is FolderItem.Folder -> {
            val color = MaterialTheme.theme.colors.getFolderColor(item.folder.color)
            val podcastUuids = item.podcasts.map { it.uuid }
            val badgeCount = folderBadgeCount(item.podcasts, content.badgeType, content.podcastUuidToBadge)
            val onClick = { onFolderClick(item.folder) }
            if (isList) {
                Column(modifier = Modifier.liftShadow(isLifted)) {
                    FolderListRow(color, item.folder.name, podcastUuids, onClick, badgeCount = badgeCount, badgeType = content.badgeType)
                    HorizontalDivider()
                }
            } else {
                FolderImage(
                    name = item.folder.name,
                    color = color,
                    podcastUuids = podcastUuids,
                    badgeCount = badgeCount,
                    badgeType = content.badgeType,
                    textSpacing = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onClick() },
                )
            }
        }
    }
}

private fun folderBadgeCount(podcasts: List<Podcast>, badgeType: BadgeType, podcastUuidToBadge: Map<String, Int>): Int {
    val episodeCount = podcasts.sumOf { podcast -> podcastUuidToBadge[podcast.uuid] ?: 0 }
    return when (badgeType) {
        BadgeType.OFF -> 0
        BadgeType.ALL_UNFINISHED -> min(99, episodeCount)
        BadgeType.LATEST_EPISODE -> min(1, episodeCount)
    }
}

private fun Modifier.liftShadow(isLifted: Boolean) = if (isLifted) graphicsLayer { shadowElevation = 1f } else this

@Composable
private fun PodcastGridCell(
    podcast: Podcast,
    badgeCount: Int,
    badgeType: BadgeType,
    layout: PodcastGridLayoutType,
    description: String,
    imageRequestFactory: PocketCastsImageRequestFactory,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(4.dp)
    BoxWithConstraints(modifier = Modifier.aspectRatio(1f)) {
        val sizePx = constraints.maxWidth
        val request = remember(podcast.uuid, imageRequestFactory, sizePx) {
            imageRequestFactory.copy(cornerRadius = 4, showErrorPlaceholder = false).create(podcast)
                .newBuilder()
                .size(sizePx)
                .precision(Precision.INEXACT)
                .build()
        }
        val painter = rememberAsyncImagePainter(request, contentScale = ContentScale.Crop)
        val state by painter.state.collectAsState()
        val isError = state is AsyncImagePainter.State.Error
        val errorColor = remember(podcast.uuid, context) {
            val index = abs(podcast.uuid.hashCode()) % Theme.folderColors.size
            Color(context.getThemeColor(Theme.folderColors.getOrElse(index) { Theme.folderColors.first() }))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(1.dp, shape)
                .clip(shape)
                .background(if (isError) errorColor else Color.Transparent)
                .clickable(onClick = onClick)
                .semantics { contentDescription = description },
        ) {
            if (isError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.2f)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))),
                )
                TextH50(
                    text = podcast.title,
                    color = Color.White,
                    fontSize = if (layout == PodcastGridLayoutType.SMALL_ARTWORK) 10.sp else 14.sp,
                    maxLines = 4,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                )
            }
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        CountBadge(
            count = badgeCount,
            style = if (badgeType == BadgeType.LATEST_EPISODE) CountBadgeStyle.Small else CountBadgeStyle.Medium,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp),
        )
    }
}

@Composable
private fun PodcastListRow(
    podcast: Podcast,
    badgeCount: Int,
    badgeType: BadgeType,
    description: String,
    isLifted: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.theme.colors
    val naturalLineHeight = LocalTextStyle.current.copy(lineHeight = TextUnit.Unspecified)
    Box(
        modifier = Modifier
            .liftShadow(isLifted)
            .height(80.dp)
            .fillMaxWidth()
            .background(colors.primaryUi01),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .semantics { contentDescription = description },
        ) {
            PodcastImage(
                uuid = podcast.uuid,
                imageSize = 64.dp,
                cornerSize = 4.dp,
                contentDescription = null,
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
            ) {
                Text(
                    text = podcast.title,
                    fontSize = 14.sp,
                    letterSpacing = 0.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.primaryText01,
                    style = naturalLineHeight,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                Text(
                    text = podcast.author,
                    fontSize = 13.sp,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.primaryText02,
                    style = naturalLineHeight,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            CountBadge(
                count = badgeCount,
                style = if (badgeType == BadgeType.LATEST_EPISODE) CountBadgeStyle.Small else CountBadgeStyle.Big,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
    }
}
