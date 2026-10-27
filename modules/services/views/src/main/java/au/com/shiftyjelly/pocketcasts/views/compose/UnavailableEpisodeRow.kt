package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.swipe.rememberSwipeRowState
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.trailingSwipeRowActions
import java.util.Date
import java.util.Locale
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun UnavailableEpisodeRow(
    episode: ManualPlaylistEpisode,
    imageRequestFactory: PocketCastsImageRequestFactory,
    swipeActions: SwipeRowActions,
    onClick: () -> Unit,
    onSwipeAction: (SwipeAction) -> Unit,
    modifier: Modifier = Modifier,
    isMultiSelecting: Boolean = false,
    colors: EpisodeRowColors = EpisodeRowDefaults.colors(),
) {
    val context = LocalContext.current
    val date = remember(context, episode.publishedAt) { RelativeDateFormatter(context).format(Date.from(episode.publishedAt)) }
    val request = remember(episode.podcastUuid, imageRequestFactory) { imageRequestFactory.createForPodcast(episode.podcastUuid) }
    EpisodeRowContainer(
        swipeRowState = rememberSwipeRowState(episode.episodeUuid),
        leadingActions = emptyList(),
        trailingActions = swipeActions.trailingSwipeRowActions(context, onSwipeAction),
        isMultiSelecting = isMultiSelecting,
        isSelected = false,
        colors = colors,
        contentDescription = null,
        onClick = onClick,
        onLongClick = null,
        modifier = modifier,
    ) {
        Layout(
            contents = listOf(
                { EpisodeRowArtwork(request, alpha = 1f, elevation = 0f, cardAlpha = UNAVAILABLE_ALPHA) },
                {
                    TextViewLine(
                        text = AnnotatedString(date.uppercase(Locale.getDefault())),
                        size = 11.sp,
                        color = textAppearanceColor(UR.attr.textOverline),
                        isMedium = true,
                        letterSpacing = 0.07.em,
                        modifier = Modifier
                            .imageViewAlpha(UNAVAILABLE_ALPHA)
                            .clearAndSetSemantics { text = AnnotatedString(date) },
                    )
                },
                {
                    TextViewLine(
                        text = AnnotatedString(episode.title),
                        size = 16.sp,
                        color = textAppearanceColor(UR.attr.textSubtitle1),
                        maxLines = 2,
                        modifier = Modifier.imageViewAlpha(UNAVAILABLE_ALPHA),
                    )
                },
                { UnavailableStatus(colors) },
            ),
        ) { (artworks, dates, titles, statuses), constraints ->
            val margin = 16.dp.roundToPx()
            val verticalMargin = 12.dp.roundToPx()
            val artworkSize = 56.dp.roundToPx()
            val textStart = margin + artworkSize + margin
            val textWidth = (constraints.maxWidth - textStart - margin).coerceAtLeast(0)
            val artwork = artworks.single().measure(Constraints.fixed(artworkSize, artworkSize))
            val dateLabel = dates.single().measure(Constraints(minWidth = textWidth, maxWidth = textWidth))
            val title = titles.single().measure(Constraints(minWidth = textWidth, maxWidth = textWidth))
            val status = statuses.single().measure(Constraints(maxWidth = textWidth))
            val textHeight = dateLabel.height + title.height + status.height
            val height = maxOf(artworkSize, textHeight) + verticalMargin * 2
            val gap = (height - verticalMargin * 2 - textHeight) / 4f
            layout(constraints.maxWidth, height) {
                artwork.place(margin, verticalMargin + ((height - verticalMargin * 2 - artworkSize) / 2f + 0.5f).toInt())
                val dateTop = verticalMargin + gap
                val titleTop = dateTop + dateLabel.height + gap
                val statusTop = titleTop + title.height + gap
                dateLabel.place(textStart, (dateTop + 0.5f).toInt())
                title.place(textStart, (titleTop + 0.5f).toInt())
                status.place(textStart, (statusTop + 0.5f).toInt())
            }
        }
    }
}

@Composable
private fun UnavailableStatus(colors: EpisodeRowColors) {
    Row(
        verticalAlignment = TruncatedCenterVertically,
        modifier = Modifier.graphicsLayer { alpha = UNAVAILABLE_ALPHA },
    ) {
        FitCenterDrawableIcon(
            drawableId = IR.drawable.ic_episode_unavailable,
            tint = colors.theme.primaryIcon02,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(16.dp),
        )
        TextViewLine(
            text = AnnotatedString(stringResource(LR.string.unavailable)),
            size = 13.sp,
            color = textAppearanceColor(UR.attr.textCaption),
            letterSpacing = 0.02.em,
        )
    }
}

private const val UNAVAILABLE_ALPHA = 0.4f

@Composable
private fun textAppearanceColor(@AttrRes styleAttr: Int): Color {
    val context = LocalContext.current
    return remember(context, styleAttr) { Color(context.textAppearanceColor(styleAttr)) }
}

private fun Context.textAppearanceColor(@AttrRes styleAttr: Int): Int {
    val style = TypedValue().also { theme.resolveAttribute(styleAttr, it, true) }.resourceId
    val attributes = obtainStyledAttributes(style, intArrayOf(android.R.attr.textColor))
    return attributes.getColor(0, 0).also { attributes.recycle() }
}
