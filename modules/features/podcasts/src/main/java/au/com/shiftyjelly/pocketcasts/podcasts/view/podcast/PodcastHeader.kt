package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings.PodcastRating
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingState
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel.RatingTappedSource
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil.compose.rememberAsyncImagePainter
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PodcastHeader(
    uuid: String,
    title: String,
    rating: RatingState,
    isFollowed: Boolean,
    areNotificationsEnabled: Boolean,
    folderIcon: PodcastFolderIcon,
    contentPadding: PaddingValues,
    useBlurredArtwork: Boolean,
    onClickRating: (String, RatingTappedSource) -> Unit,
    onClickFollow: () -> Unit,
    onClickUnfollow: () -> Unit,
    onClickFolder: () -> Unit,
    onClickNotification: () -> Unit,
    onClickSettings: () -> Unit,
    onLongClickArtwork: () -> Unit,
    onArtworkAvailable: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val expandedCoverSize = minOf(maxWidth * 0.48f, 192.dp)

        PodcastBackgroundArtwork(
            uuid = uuid,
            useBlurredArtwork = useBlurredArtwork,
            maxWidth = maxWidth,
            bottomAnchor = contentPadding.calculateTopPadding() + expandedCoverSize * 0.75f,
            onArtworkAvailable = { onArtworkAvailable(uuid) },
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
        ) {
            PodcastImage(
                uuid = uuid,
                cornerSize = 8.dp,
                modifier = Modifier
                    .size(expandedCoverSize)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClickArtwork,
                    ),
            )
            Spacer(
                modifier = Modifier.height(24.dp),
            )
            PodcastControls(
                title = title,
                rating = rating,
                onClickRating = { source -> onClickRating(uuid, source) },
                isFollowed = isFollowed,
                areNotificationsEnabled = areNotificationsEnabled,
                folderIcon = folderIcon,
                onClickFollow = onClickFollow,
                onClickUnfollow = onClickUnfollow,
                onClickFolder = onClickFolder,
                onClickNotification = onClickNotification,
                onClickSettings = onClickSettings,
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
        }
    }
}

@Composable
private fun PodcastControls(
    title: String,
    rating: RatingState,
    isFollowed: Boolean,
    areNotificationsEnabled: Boolean,
    folderIcon: PodcastFolderIcon,
    onClickRating: (RatingTappedSource) -> Unit,
    onClickFollow: () -> Unit,
    onClickUnfollow: () -> Unit,
    onClickFolder: () -> Unit,
    onClickNotification: () -> Unit,
    onClickSettings: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // TODO: Categories list
        TextH20(
            text = title,
            textAlign = TextAlign.Center,
        )
        AnimatedPodcastRating(
            rating = rating,
            onClickRating = onClickRating,
        )
        PodcastActions(
            isFollowed = isFollowed,
            areNotificationsEnabled = areNotificationsEnabled,
            folderIcon = folderIcon,
            onClickFollow = onClickFollow,
            onClickUnfollow = onClickUnfollow,
            onClickFolder = onClickFolder,
            onClickNotification = onClickNotification,
            onClickSettings = onClickSettings,
        )
    }
}

@Composable
private fun AnimatedPodcastRating(
    rating: RatingState,
    onClickRating: (RatingTappedSource) -> Unit,
) {
    AnimatedContent(
        targetState = rating,
    ) { state ->
        when (state) {
            is RatingState.Loaded -> PodcastRating(
                state = state,
                onClick = onClickRating,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            is RatingState.Loading, is RatingState.Error -> Spacer(
                modifier = Modifier.height(16.dp),
            )
        }
    }
}

@Composable
private fun PodcastActions(
    isFollowed: Boolean,
    areNotificationsEnabled: Boolean,
    folderIcon: PodcastFolderIcon,
    onClickFollow: () -> Unit,
    onClickUnfollow: () -> Unit,
    onClickFolder: () -> Unit,
    onClickNotification: () -> Unit,
    onClickSettings: () -> Unit,
) {
    val transition = updateTransition(targetState = isFollowed)

    SubcomposeLayout { constraints ->
        val controls = subcompose("controls") {
            val alpha by transition.animateFloat(
                label = "controlsAlpha",
                transitionSpec = { if (targetState) actionAlphaInSpec else actionAlphaOutSpec },
                targetValueByState = { followed -> if (followed) 1f else 0f },
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(alpha),
            ) {
                Spacer(
                    modifier = Modifier.size(32.dp),
                )
                ActionButton(
                    iconId = folderIcon.id,
                    contentDescription = stringResource(LR.string.podcast_change_folder),
                    onClick = {
                        if (!transition.isRunning) {
                            onClickFolder()
                        }
                    },
                )
                ActionButton(
                    iconId = if (areNotificationsEnabled) IR.drawable.ic_notifications_on else IR.drawable.ic_notifications_off,
                    contentDescription = stringResource(LR.string.podcast_notifications),
                    onClick = {
                        if (!transition.isRunning) {
                            onClickNotification()
                        }
                    },
                )
                ActionButton(
                    iconId = IR.drawable.ic_profile_settings,
                    contentDescription = stringResource(LR.string.podcast_settings),
                    onClick = {
                        if (!transition.isRunning) {
                            onClickSettings()
                        }
                    },
                )
            }
        }[0].measure(constraints)

        val dummyButton = subcompose("dummyButton") {
            TextH40(
                text = stringResource(LR.string.subscribe),
                modifier = Modifier.padding(horizontal = 60.dp, vertical = 12.dp),
            )
        }[0].measure(Constraints())

        val width = maxOf(dummyButton.width, controls.width)
        val height = maxOf(dummyButton.height, controls.height)

        val controlsOffsetX = ((width - controls.width) / 2f).roundToInt()
        val controlsOffsetY = ((height - controls.height) / 2f).roundToInt()
        val startEdgeOffset = ((24.dp.roundToPx() - width) / 2f).roundToInt()
        val unfollowButtonOffsetX = if (controls.width == width) {
            startEdgeOffset
        } else {
            startEdgeOffset + controlsOffsetX
        }

        val dummyButtonWidthDp = dummyButton.width.toDp()
        val dummyButtonHeightDp = dummyButton.height.toDp()

        val followButton = subcompose("followButton") {
            val backgroundColor by transition.animateColor(
                label = "backgroundColor",
                transitionSpec = { colorSpec },
                targetValueByState = { followed -> if (followed) MaterialTheme.theme.colors.support02 else Color.Transparent },
            )
            val borderColor by transition.animateColor(
                label = "borderColor",
                transitionSpec = { colorSpec },
                targetValueByState = { followed -> if (followed) MaterialTheme.theme.colors.support02 else MaterialTheme.theme.colors.primaryIcon02 },
            )
            val cornerRadius by transition.animateDp(
                label = "cornerRadius",
                transitionSpec = { dpSpec },
                targetValueByState = { followed -> if (followed) 12.dp else 8.dp },
            )
            val buttonWidth by transition.animateDp(
                label = "buttonWidth",
                transitionSpec = { dpSpec },
                targetValueByState = { followed -> if (followed) 32.dp else dummyButtonWidthDp },
            )
            val buttonHeight by transition.animateDp(
                label = "buttonHeight",
                transitionSpec = { dpSpec },
                targetValueByState = { followed -> if (followed) 32.dp else dummyButtonHeightDp },
            )
            val buttonPadding by transition.animateDp(
                label = "buttonPadding",
                transitionSpec = { dpSpec },
                targetValueByState = { followed -> if (followed) 4.dp else 0.dp },
            )
            val buttonOffset by transition.animateIntOffset(
                label = "buttonOffset",
                transitionSpec = { intOffsetSpec },
                targetValueByState = { followed -> if (followed) IntOffset(unfollowButtonOffsetX, 0) else IntOffset.Zero },
            )
            val textAlpha by transition.animateFloat(
                label = "textAlpha",
                transitionSpec = { floatSpec },
                targetValueByState = { followed -> if (followed) 0f else 1f },
            )

            val buttonDescription = stringResource(if (isFollowed) LR.string.unsubscribe else LR.string.subscribe)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { buttonOffset.copy(x = buttonOffset.x + buttonPadding.roundToPx()) }
                    .width(buttonWidth)
                    .height(buttonHeight)
                    .padding(buttonPadding)
                    .background(backgroundColor, RoundedCornerShape(cornerRadius))
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(cornerRadius),
                    )
                    .then(if (isFollowed) Modifier else Modifier.clip(RoundedCornerShape(cornerRadius)))
                    .clickable(
                        indication = if (isFollowed) controlActionRipple else buttonRipple,
                        interactionSource = null,
                        onClick = {
                            if (!transition.isRunning) {
                                if (isFollowed) onClickUnfollow() else onClickFollow()
                            }
                        },
                    )
                    .clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = buttonDescription
                    },
            ) {
                TextH40(
                    text = stringResource(LR.string.subscribe),
                    modifier = Modifier.alpha(textAlpha),
                )
                Image(
                    painter = painterResource(IR.drawable.ic_check),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryInteractive02),
                    modifier = Modifier
                        .size(16.dp)
                        .alpha(1f - textAlpha),
                )
            }
        }[0].measure(constraints)

        layout(width, height) {
            controls.place(x = controlsOffsetX, y = controlsOffsetY)
            followButton.place(x = ((width - followButton.width) / 2f).roundToInt(), y = ((height - followButton.height) / 2f).roundToInt())
        }
    }
}

@Composable
private fun ActionButton(
    @DrawableRes iconId: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Image(
        painter = painterResource(iconId),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon02),
        modifier = Modifier
            .padding(4.dp)
            .size(24.dp)
            .clickable(
                indication = controlActionRipple,
                interactionSource = null,
                onClick = onClick,
            ),
    )
}

@Composable
private fun PodcastBackgroundArtwork(
    uuid: String,
    useBlurredArtwork: Boolean,
    maxWidth: Dp,
    bottomAnchor: Dp,
    onArtworkAvailable: () -> Unit,
) {
    val imageSize = maxWidth * if (useBlurredArtwork) 1.3f else 1f
    val imageSizePx = LocalDensity.current.run { imageSize.roundToPx() }

    val imageBottomOffset = imageSize - bottomAnchor
    val imageBottomOffsetPx = LocalDensity.current.run { imageBottomOffset.roundToPx() }

    ImageOrPreview(
        uuid = uuid,
        onArtworkAvailable = onArtworkAvailable,
        modifier = Modifier
            .layout { measurable, constraints ->
                val const = constraints.copy(
                    minWidth = imageSizePx,
                    maxWidth = imageSizePx,
                    minHeight = imageSizePx - imageBottomOffsetPx,
                    maxHeight = imageSizePx - imageBottomOffsetPx,
                )
                val placeable = measurable.measure(const)
                val width = const.constrainWidth(placeable.width)
                val height = const.constrainHeight(placeable.height)
                layout(width, height) { placeable.placeRelative(0, 0) }
            }
            .blurOrScrim(useBlur = useBlurredArtwork),
    )
}

@Composable
private fun ImageOrPreview(
    uuid: String,
    onArtworkAvailable: () -> Unit,
    modifier: Modifier,
) {
    if (!LocalInspectionMode.current) {
        val context = LocalContext.current
        val imageRequest = remember(uuid) {
            PocketCastsImageRequestFactory(context).createForPodcast(uuid, onSuccess = onArtworkAvailable)
        }
        val painter = rememberAsyncImagePainter(
            model = imageRequest,
            contentScale = ContentScale.Crop,
        )
        Image(
            painter = painter,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            contentDescription = null,
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

internal enum class PodcastFolderIcon(
    @DrawableRes val id: Int,
) {
    NotInFolder(
        id = IR.drawable.ic_folder,
    ),
    AddedToFolder(
        id = IR.drawable.ic_folder_check,
    ),
    BuyFolders(
        id = IR.drawable.ic_folder_plus,
    ),
}

private val buttonRipple = ripple()
private val controlActionRipple = ripple(bounded = false)

private val colorSpec = tween<Color>(durationMillis = 450, easing = EaseOutCubic)
private val dpSpec = tween<Dp>(durationMillis = 450, easing = EaseOutCubic)
private val intOffsetSpec = tween<IntOffset>(durationMillis = 450, easing = EaseOutCubic)
private val floatSpec = tween<Float>(durationMillis = 450, easing = EaseOutCubic)

private val actionAlphaInSpec = tween<Float>(durationMillis = 250, delayMillis = 300, easing = EaseOutCubic)
private val actionAlphaOutSpec = tween<Float>(durationMillis = 200, delayMillis = 0, easing = EaseOutCubic)

@Preview
@Composable
private fun PodcastHeaderPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        var isFollowed by remember { mutableStateOf(false) }

        PodcastHeader(
            uuid = "uuid",
            title = "The Pitchfork Review",
            rating = RatingState.Loaded(
                ratings = PodcastRatings(
                    podcastUuid = "uuid",
                    average = 3.5,
                    total = 42_000,
                ),
            ),
            isFollowed = isFollowed,
            areNotificationsEnabled = true,
            folderIcon = PodcastFolderIcon.BuyFolders,
            contentPadding = PaddingValues(top = 48.dp),
            useBlurredArtwork = false,
            onClickRating = { _, _ -> },
            onClickFollow = { isFollowed = true },
            onClickUnfollow = { isFollowed = false },
            onClickFolder = {},
            onClickNotification = {},
            onClickSettings = {},
            onLongClickArtwork = {},
            onArtworkAvailable = {},
        )
    }
}
