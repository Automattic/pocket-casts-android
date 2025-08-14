package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.toFriendlyString
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class PlaylistHeaderData(
    val title: String,
    val totalEpisodeCount: Int,
    val displayedEpisodeCount: Int,
    val playbackDurationLeft: Duration,
    val artworkPodcasts: List<Podcast>,
    val leftButton: ActionButton,
    val rightButton: ActionButton,
    val searchState: TextFieldState,
) {
    data class ActionButton(
        val iconId: Int,
        val label: String,
        val onClick: () -> Unit,
    )
}

@Composable
internal fun PlaylistHeader(
    data: PlaylistHeaderData?,
    useBlurredArtwork: Boolean,
    onMeasureSearchTopOffset: (Float) -> Unit,
    onChangeSearchFocus: (FocusState) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val podcasts = data?.artworkPodcasts
        val contentTopPadding = AppBarDefaults.topAppBarWindowInsets
            .asPaddingValues()
            .calculateTopPadding()
            .coerceAtLeast(100.dp)
        val artworkSize = minOf(maxWidth * 0.48f, 192.dp)

        PlaylistBackgroundArtwork(
            podcasts = podcasts,
            useBlurredArtwork = useBlurredArtwork,
            maxWidth = maxWidth,
            bottomAnchor = contentTopPadding + artworkSize * 0.75f,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(
                modifier = Modifier.height(contentTopPadding),
            )
            PlaylistForegroundArtwork(
                artworkSize = artworkSize,
                podcasts = podcasts,
            )
            Spacer(
                modifier = Modifier.height(20.dp),
            )
            TextH20(
                text = data?.title.orEmpty(),
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 64.dp),
            )
            Spacer(
                modifier = Modifier.height(8.dp),
            )
            PlaylistInfoText(
                episodeCount = data?.totalEpisodeCount,
                playbackDurationLeft = data?.playbackDurationLeft,
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            if (data != null) {
                ActionButtons(
                    hasAnyEpisodes = data.displayedEpisodeCount > 0,
                    leftButton = data.leftButton,
                    rightButton = data.rightButton,
                )
                Spacer(
                    modifier = Modifier.height(24.dp),
                )
                if (data.totalEpisodeCount > 0) {
                    PlaylistSearchBar(
                        data = data,
                        contentTopPadding = contentTopPadding,
                        onChangeSearchFocus = onChangeSearchFocus,
                        onMeasureSearchTopOffset = onMeasureSearchTopOffset,
                    )
                    Spacer(
                        modifier = Modifier.height(16.dp),
                    )
                    HorizontalDivider()
                }
                AnimatedVisibility(
                    visible = data.totalEpisodeCount == 0,
                    enter = noContentEnterTransition,
                    exit = noContentExitTransition,
                ) {
                    NoContentBanner(
                        title = stringResource(LR.string.smart_playlist_no_content_title),
                        body = stringResource(LR.string.smart_playlist_no_content_body),
                        iconResourceId = IR.drawable.ic_info,
                        modifier = Modifier.padding(top = 60.dp, bottom = 24.dp),
                    )
                }
                AnimatedVisibility(
                    visible = data.totalEpisodeCount != 0 && data.displayedEpisodeCount == 0,
                    enter = noContentEnterTransition,
                    exit = noContentExitTransition,
                ) {
                    NoContentBanner(
                        title = stringResource(LR.string.search_episodes_not_found_title),
                        body = stringResource(LR.string.search_episodes_not_found_summary),
                        iconResourceId = IR.drawable.ic_exclamation_circle,
                        modifier = Modifier.padding(top = 24.dp, bottom = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistForegroundArtwork(
    artworkSize: Dp,
    podcasts: List<Podcast>?,
    modifier: Modifier = Modifier,
) {
    val showShadow by rememberUpdatedState(podcasts != null)
    val shadowBoxSize by animateDpAsState(
        targetValue = if (showShadow) artworkSize else 0.dp,
        animationSpec = artworkShadowSpec,
    )
    // Animations are smoke and mirrors. We can't simply use elevation on the PlaylistArtwork,
    // as it causes UI glitches due to the Crossfade element.
    //
    // Instead, we draw a transparent box behind it that appears after a short delay.
    // The delay comes from artworkShadowSpec.
    Box(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .shadow(16.dp, RoundedCornerShape(artworkSize / 14))
                .size(shadowBoxSize),
        )
        Crossfade(
            targetState = podcasts,
            animationSpec = if (podcasts.isNullOrEmpty()) artworkCrossfadeFastSpec else artworkCrossfadeSpec,
        ) { podcasts ->
            if (podcasts != null) {
                PlaylistArtwork(
                    podcasts = podcasts,
                    artworkSize = artworkSize,
                    elevation = 0.dp,
                )
            } else {
                Spacer(
                    modifier = Modifier.height(artworkSize),
                )
            }
        }
    }
}

@Composable
private fun PlaylistBackgroundArtwork(
    podcasts: List<Podcast>?,
    useBlurredArtwork: Boolean,
    maxWidth: Dp,
    bottomAnchor: Dp,
    modifier: Modifier = Modifier,
) {
    val artworkSize = maxWidth * if (useBlurredArtwork) 1.3f else 1f
    val artworkSizePx = LocalDensity.current.run { artworkSize.roundToPx() }

    val artworkBottomOffset = artworkSize - bottomAnchor
    val artworkBottomOffsetPx = LocalDensity.current.run { artworkBottomOffset.roundToPx() }

    Crossfade(
        targetState = podcasts?.takeIf { it.isNotEmpty() },
        animationSpec = if (podcasts.isNullOrEmpty()) artworkCrossfadeFastSpec else artworkCrossfadeSpec,
        modifier = modifier
            .layout { measurable, constraints ->
                val artworkHeightPx = if (useBlurredArtwork) {
                    artworkSizePx
                } else {
                    artworkSizePx - artworkBottomOffsetPx
                }
                val const = constraints.copy(
                    minWidth = artworkSizePx,
                    maxWidth = artworkSizePx,
                    minHeight = artworkHeightPx,
                    maxHeight = artworkHeightPx,
                )

                val placeable = measurable.measure(const)
                val width = const.constrainWidth(placeable.width)
                val height = const.constrainHeight(placeable.height)
                val offset = if (useBlurredArtwork) {
                    artworkBottomOffsetPx
                } else {
                    0
                }
                layout(width, height - offset) { placeable.place(0, -offset) }
            }
            .blurOrScrim(useBlur = useBlurredArtwork),
    ) { podcasts ->
        if (podcasts != null) {
            ArtworkOrPreview(
                podcasts = podcasts,
                artworkSize = artworkSize,
            )
        }
    }
}

@Composable
private fun ArtworkOrPreview(
    podcasts: List<Podcast>,
    artworkSize: Dp,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        PlaylistArtwork(
            podcasts = podcasts,
            artworkSize = artworkSize,
            cornerSize = 0.dp,
            elevation = 0.dp,
            modifier = modifier,
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
        ) {
            previewColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun PlaylistInfoText(
    episodeCount: Int?,
    playbackDurationLeft: Duration?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val episodeCountText = if (episodeCount != null) {
        pluralStringResource(LR.plurals.episode_count, episodeCount, episodeCount)
    } else {
        null
    }
    val durationLeftText = remember(context, playbackDurationLeft, episodeCount) {
        playbackDurationLeft
            ?.takeIf { episodeCount != null && episodeCount > 0 }
            ?.toFriendlyString(context.resources, pluralResourceId = { unit -> unit.shortResourceId })
    }
    val playlistInfoText = remember(episodeCountText, durationLeftText) {
        buildString {
            if (episodeCountText != null) {
                append(episodeCountText)
                if (durationLeftText != null) {
                    append(" • ")
                    append(durationLeftText)
                }
            }
        }
    }
    TextP60(
        text = playlistInfoText,
        color = MaterialTheme.theme.colors.primaryText02,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = 64.dp),
    )
}

@Composable
private fun ActionButtons(
    hasAnyEpisodes: Boolean,
    leftButton: PlaylistHeaderData.ActionButton,
    rightButton: PlaylistHeaderData.ActionButton,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val windowWidth = density.run { LocalWindowInfo.current.containerSize.width.toDp() }
    val buttonWidth = minOf((windowWidth - actionButtonsOuterPadding * 2) / 2, actionButtonMaxWidth)
    val targetOffset = buttonWidth / 2 + actionButtonsInnerPadding / 2
    val targetOffsetPx = density.run { targetOffset.roundToPx() }

    val transition = updateTransition(hasAnyEpisodes)
    val offset by transition.animateIntOffset(
        transitionSpec = { actionButtonsOffsetSpec },
        targetValueByState = { hasEpisodes -> if (hasEpisodes) IntOffset.Zero else IntOffset(targetOffsetPx, 0) },
    )
    val alpha by transition.animateFloat(
        transitionSpec = { actionButtonsAlphaSpec },
        targetValueByState = { hasEpisodes -> if (hasEpisodes) 1f else 0f },
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(
            space = actionButtonsInnerPadding,
            alignment = Alignment.CenterHorizontally,
        ),
        modifier = modifier
            .offset { offset }
            .height(IntrinsicSize.Max)
            .padding(horizontal = actionButtonsOuterPadding),
    ) {
        ActionButton(
            data = leftButton,
            style = ActionButtonStyle.Immersive,
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        ActionButton(
            data = rightButton,
            style = ActionButtonStyle.Solid,
            contentAlignment = Alignment.TopStart,
            isEnabled = hasAnyEpisodes,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .alpha(alpha),
        )
    }
}

@Composable
private fun PlaylistSearchBar(
    data: PlaylistHeaderData,
    contentTopPadding: Dp,
    onChangeSearchFocus: (FocusState) -> Unit,
    onMeasureSearchTopOffset: (Float) -> Unit,
) {
    val density = LocalDensity.current
    var isSearchPositionKnown by remember { mutableStateOf(false) }
    SearchBar(
        state = data.searchState,
        placeholder = stringResource(LR.string.search),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .onFocusChanged(onChangeSearchFocus)
            .then(
                if (!isSearchPositionKnown) {
                    Modifier.onGloballyPositioned { coordinates ->
                        val topPaddingPx = density.run { (contentTopPadding + 24.dp).toPx() }
                        val searchTopPx = coordinates.positionInParent().y
                        onMeasureSearchTopOffset(searchTopPx - topPaddingPx)
                        isSearchPositionKnown = true
                    }
                } else {
                    Modifier
                },
            ),
    )
}

private fun Modifier.blurOrScrim(useBlur: Boolean) = if (useBlur) {
    blur(80.dp, BlurredEdgeTreatment.Unbounded)
} else {
    graphicsLayer(
        compositingStrategy = CompositingStrategy.Offscreen,
    ).drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.65f to Color.Black.copy(alpha = 0.5f),
                    1f to Color.Transparent,
                ),
                startY = 0f,
                endY = Float.POSITIVE_INFINITY,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
}

@Composable
private fun ActionButton(
    data: PlaylistHeaderData.ActionButton,
    contentAlignment: Alignment,
    style: ActionButtonStyle,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    CompositionLocalProvider(LocalRippleConfiguration provides style.rememberRippleConfiguration()) {
        Box(
            contentAlignment = contentAlignment,
            modifier = modifier,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .widthIn(max = actionButtonMaxWidth)
                    .fillMaxSize()
                    .clip(actionButtonShape)
                    .background(style.backgroundColor(), actionButtonShape)
                    .border(2.dp, style.borderColor(), actionButtonShape)
                    .clickable(
                        enabled = isEnabled,
                        role = Role.Button,
                        onClick = data.onClick,
                    )
                    .padding(vertical = 12.dp, horizontal = 8.dp)
                    .semantics(mergeDescendants = true) {},
            ) {
                Image(
                    painter = painterResource(data.iconId),
                    colorFilter = ColorFilter.tint(style.onBackgroundColor()),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(
                    modifier = Modifier.width(8.dp),
                )
                TextH40(
                    text = data.label,
                    color = style.onBackgroundColor(),
                    maxLines = 1,
                )
            }
        }
    }
}

private enum class ActionButtonStyle {
    Solid,
    Immersive,
    ;

    @Composable
    @ReadOnlyComposable
    fun backgroundColor() = when (this) {
        Solid -> MaterialTheme.theme.colors.primaryText01
        Immersive -> Color.Unspecified
    }

    @Composable
    @ReadOnlyComposable
    fun onBackgroundColor() = when (this) {
        Solid -> MaterialTheme.theme.colors.primaryUi02
        Immersive -> MaterialTheme.theme.colors.primaryText01
    }

    @Composable
    @ReadOnlyComposable
    fun borderColor() = when (this) {
        Solid -> Color.Unspecified
        Immersive -> MaterialTheme.theme.colors.primaryText01.copy(alpha = 0.2f)
    }

    @Composable
    fun rememberRippleConfiguration(): RippleConfiguration {
        val color = when (this) {
            Solid -> MaterialTheme.theme.colors.primaryUi02
            Immersive -> MaterialTheme.theme.colors.primaryText01
        }
        return remember(color) {
            RippleConfiguration(color = color)
        }
    }
}

private val artworkCrossfadeFastSpec = spring<Float>(stiffness = Spring.StiffnessLow)
private val artworkCrossfadeSpec = spring<Float>(stiffness = Spring.StiffnessVeryLow)
private val artworkShadowSpec = tween<Dp>(durationMillis = 500, delayMillis = 500)

private val actionButtonShape = RoundedCornerShape(8.dp)
private val actionButtonMaxWidth = 200.dp
private val actionButtonsInnerPadding = 8.dp
private val actionButtonsOuterPadding = 32.dp
private val actionButtonsOffsetSpec = spring<IntOffset>(stiffness = Spring.StiffnessLow)
private val actionButtonsAlphaSpec = spring<Float>(stiffness = Spring.StiffnessLow)

private val noContentEnterTransition =
    fadeIn(spring(stiffness = Spring.StiffnessLow)) + expandVertically(spring(stiffness = Spring.StiffnessLow))
private val noContentExitTransition =
    fadeOut(spring(stiffness = Spring.StiffnessLow)) + shrinkVertically(spring(stiffness = Spring.StiffnessLow))

private val previewColors = listOf(
    Color(0xFFCC99C9),
    Color(0xFF9EC1CF),
    Color(0xFF9EE09E),
    Color(0xFFFDFD97),
    Color(0xFFFEB144),
    Color(0xFFFF6663),
    Color(0xFFCC99C9),
    Color(0xFF9EC1CF),
    Color(0xFF9EE09E),
    Color(0xFFFDFD97),
    Color(0xFFFEB144),
    Color(0xFFFF6663),
)

@PreviewRegularDevice
@Composable
private fun PlaylistHeaderNoEpisodesPreview() {
    var episodeCount by remember { mutableIntStateOf(0) }

    AppTheme(ThemeType.LIGHT) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .fillMaxSize(),
        ) {
            PlaylistHeader(
                data = PlaylistHeaderData(
                    title = "My Playlist",
                    totalEpisodeCount = episodeCount,
                    displayedEpisodeCount = episodeCount,
                    playbackDurationLeft = 0.seconds,
                    artworkPodcasts = emptyList(),
                    leftButton = PlaylistHeaderData.ActionButton(
                        iconId = IR.drawable.sleep_timer_cog,
                        label = "Smart Rules",
                        onClick = { episodeCount = 1 },
                    ),
                    rightButton = PlaylistHeaderData.ActionButton(
                        iconId = IR.drawable.ic_filters_play,
                        label = "Play All",
                        onClick = { episodeCount = 0 },
                    ),
                    searchState = rememberTextFieldState(),
                ),
                useBlurredArtwork = false,
                onMeasureSearchTopOffset = {},
                onChangeSearchFocus = {},
            )
        }
    }
}

@PreviewRegularDevice
@Composable
private fun PlaylistHeaderNoDisplayedEpisodesPreview() {
    AppTheme(ThemeType.LIGHT) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .fillMaxSize(),
        ) {
            PlaylistHeader(
                data = PlaylistHeaderData(
                    title = "My Playlist",
                    totalEpisodeCount = 20,
                    displayedEpisodeCount = 0,
                    playbackDurationLeft = 0.seconds,
                    artworkPodcasts = emptyList(),
                    leftButton = PlaylistHeaderData.ActionButton(
                        iconId = IR.drawable.sleep_timer_cog,
                        label = "Smart Rules",
                        onClick = {},
                    ),
                    rightButton = PlaylistHeaderData.ActionButton(
                        iconId = IR.drawable.ic_filters_play,
                        label = "Play All",
                        onClick = {},
                    ),
                    searchState = rememberTextFieldState(),
                ),
                useBlurredArtwork = false,
                onMeasureSearchTopOffset = {},
                onChangeSearchFocus = {},
            )
        }
    }
}

@PreviewRegularDevice
@Composable
private fun PlaylistHeaderSinglePodcastPreview() {
    AppTheme(ThemeType.LIGHT) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .fillMaxSize(),
        ) {
            PlaylistHeader(
                data = PlaylistHeaderData(
                    title = "My Playlist",
                    totalEpisodeCount = 100,
                    displayedEpisodeCount = 100,
                    playbackDurationLeft = 200.days + 12.hours,
                    artworkPodcasts = listOf(Podcast(uuid = "id-0")),
                    leftButton = PlaylistHeaderData.ActionButton(
                        iconId = IR.drawable.sleep_timer_cog,
                        label = "Smart Rules",
                        onClick = {},
                    ),
                    rightButton = PlaylistHeaderData.ActionButton(
                        iconId = IR.drawable.ic_filters_play,
                        label = "Play All",
                        onClick = {},
                    ),
                    searchState = rememberTextFieldState(),
                ),
                useBlurredArtwork = false,
                onMeasureSearchTopOffset = {},
                onChangeSearchFocus = {},
            )
        }
    }
}

@PreviewRegularDevice
@Composable
private fun PlaylistHeaderMultiPodcastPreview() {
    AppTheme(ThemeType.LIGHT) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .fillMaxSize(),
        ) {
            PlaylistHeader(
                data = PlaylistHeaderData(
                    title = "My Playlist",
                    totalEpisodeCount = 5,
                    displayedEpisodeCount = 5,
                    playbackDurationLeft = 1.hours + 15.minutes,
                    artworkPodcasts = List(4) { index -> Podcast(uuid = "id-$index") },
                    leftButton = PlaylistHeaderData.ActionButton(
                        iconId = IR.drawable.sleep_timer_cog,
                        label = "Smart Rules",
                        onClick = {},
                    ),
                    rightButton = PlaylistHeaderData.ActionButton(
                        iconId = IR.drawable.ic_filters_play,
                        label = "Play All",
                        onClick = {},
                    ),
                    searchState = rememberTextFieldState(),
                ),
                useBlurredArtwork = false,
                onMeasureSearchTopOffset = {},
                onChangeSearchFocus = {},
            )
        }
    }
}

@PreviewRegularDevice
@Composable
private fun PlaylistHeaderThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppTheme(themeType) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .fillMaxSize(),
        ) {
            PlaylistHeader(
                data = PlaylistHeaderData(
                    title = "My Playlist",
                    totalEpisodeCount = 5,
                    displayedEpisodeCount = 5,
                    playbackDurationLeft = 1.hours + 15.minutes,
                    artworkPodcasts = List(4) { index -> Podcast(uuid = "id-$index") },
                    leftButton = PlaylistHeaderData.ActionButton(
                        iconId = IR.drawable.sleep_timer_cog,
                        label = "Smart Rules",
                        onClick = {},
                    ),
                    rightButton = PlaylistHeaderData.ActionButton(
                        iconId = IR.drawable.ic_filters_play,
                        label = "Play All",
                        onClick = {},
                    ),
                    searchState = rememberTextFieldState(),
                ),
                useBlurredArtwork = false,
                onMeasureSearchTopOffset = {},
                onChangeSearchFocus = {},
            )
        }
    }
}
