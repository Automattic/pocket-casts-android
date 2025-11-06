package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.ExpandableText
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.podcasts.R
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
    category: String,
    author: String,
    description: AnnotatedString,
    podcastInfoState: PodcastInfoState,
    rating: RatingState,
    isFollowed: Boolean,
    areNotificationsEnabled: Boolean,
    isFundingUrlAvailable: Boolean,
    folderIcon: PodcastFolderIcon,
    isHeaderExpanded: Boolean,
    isDescriptionExpanded: Boolean,
    contentPadding: PaddingValues,
    useBlurredArtwork: Boolean,
    onClickCategory: () -> Unit,
    onClickRating: (RatingTappedSource) -> Unit,
    onClickFollow: () -> Unit,
    onClickUnfollow: () -> Unit,
    onClickFolder: () -> Unit,
    onClickNotification: () -> Unit,
    onClickDonate: () -> Unit,
    onClickSettings: () -> Unit,
    onClickWebsiteLink: () -> Unit,
    onToggleHeader: () -> Unit,
    onToggleDescription: () -> Unit,
    onLongClickArtwork: () -> Unit,
    onArtworkAvailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val expandedCoverSize = minOf(maxWidth * 0.48f, 192.dp)
        val shrunkCoverSize = expandedCoverSize * 0.5625f
        val coverSize by animateDpAsState(
            targetValue = if (isHeaderExpanded) expandedCoverSize else shrunkCoverSize,
            animationSpec = coverSizeSpec,
        )

        PodcastBackgroundArtwork(
            uuid = uuid,
            useBlurredArtwork = useBlurredArtwork,
            maxWidth = maxWidth,
            bottomAnchor = contentPadding.calculateTopPadding() + coverSize * 0.75f,
            onArtworkAvailable = onArtworkAvailable,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
        ) {
            PodcastImage(
                uuid = uuid,
                imageSize = coverSize,
                elevation = 16.dp,
                modifier = Modifier
                    .combinedClickable(
                        indication = null,
                        interactionSource = null,
                        onClick = onToggleHeader,
                        onLongClick = onLongClickArtwork,
                    ),
            )
            Spacer(
                modifier = Modifier.height(24.dp),
            )
            PodcastControls(
                title = title,
                category = category,
                author = author,
                rating = rating,
                onClickRating = onClickRating,
                isFollowed = isFollowed,
                areNotificationsEnabled = areNotificationsEnabled,
                isFundingUrlAvailable = isFundingUrlAvailable,
                folderIcon = folderIcon,
                isHeaderExpanded = isHeaderExpanded,
                onClickTitle = onToggleHeader,
                onClickCategory = onClickCategory,
                onClickFollow = onClickFollow,
                onClickUnfollow = onClickUnfollow,
                onClickFolder = onClickFolder,
                onClickNotification = onClickNotification,
                onClickDonate = onClickDonate,
                onClickSettings = onClickSettings,
            )
            AnimatedVisibility(
                visible = isHeaderExpanded,
                enter = headerInTranstion,
                exit = headerOutTranstion,
            ) {
                PodcastDetails(
                    description = description,
                    podcastInfoState = podcastInfoState,
                    isDescriptionExpanded = isDescriptionExpanded,
                    onClickShowNotes = onToggleDescription,
                    onClickWebsiteLink = onClickWebsiteLink,
                )
            }
        }
    }
}

@Composable
private fun PodcastControls(
    title: String,
    category: String,
    author: String,
    rating: RatingState,
    isFollowed: Boolean,
    areNotificationsEnabled: Boolean,
    isFundingUrlAvailable: Boolean,
    folderIcon: PodcastFolderIcon,
    isHeaderExpanded: Boolean,
    onClickTitle: () -> Unit,
    onClickCategory: () -> Unit,
    onClickRating: (RatingTappedSource) -> Unit,
    onClickFollow: () -> Unit,
    onClickUnfollow: () -> Unit,
    onClickFolder: () -> Unit,
    onClickNotification: () -> Unit,
    onClickDonate: () -> Unit,
    onClickSettings: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isHeaderExpanded) 0f else 180f,
        animationSpec = chevronRotationSpec,
        visibilityThreshold = 0.001f,
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = isHeaderExpanded,
            enter = categoriesEnterTransition,
            exit = categoriesExitTransition,
        ) {
            PodcastCategoriesLabel(
                category = category,
                author = author,
                onClickCategory = onClickCategory,
            )
        }
        TextH20(
            text = buildAnnotatedString {
                append(title)
                appendInlineContent(id = "chevronId")
            },
            textAlign = TextAlign.Center,
            inlineContent = mapOf(
                "chevronId" to InlineTextContent(Placeholder(24.sp, 24.sp, PlaceholderVerticalAlign.TextCenter)) {
                    Image(
                        painter = painterResource(IR.drawable.ic_chevron_small_up),
                        colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryText01),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(chevronRotation),
                    )
                },
            ),
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = null,
                    onClick = onClickTitle,
                ),
        )
        PodcastRatingOrSpacing(
            rating = rating,
            onClickRating = onClickRating,
        )
        PodcastActions(
            isFollowed = isFollowed,
            areNotificationsEnabled = areNotificationsEnabled,
            isFundingUrlAvailable = isFundingUrlAvailable,
            folderIcon = folderIcon,
            onClickFollow = onClickFollow,
            onClickUnfollow = onClickUnfollow,
            onClickFolder = onClickFolder,
            onClickNotification = onClickNotification,
            onClickDonate = onClickDonate,
            onClickSettings = onClickSettings,
        )
    }
}

@Composable
private fun PodcastCategoriesLabel(
    category: String,
    author: String,
    onClickCategory: () -> Unit,
) {
    val text = remember(category, author, onClickCategory) {
        val text = listOf(category, author).filter(String::isNotBlank).joinToString(separator = " · ")
        buildAnnotatedString {
            append(text)
            if (category.isNotBlank()) {
                addLink(
                    LinkAnnotation.Clickable(
                        tag = "category",
                        linkInteractionListener = LinkInteractionListener { onClickCategory() },
                        styles = TextLinkStyles(
                            style = SpanStyle(textDecoration = TextDecoration.None),
                            focusedStyle = SpanStyle(textDecoration = TextDecoration.Underline),
                        ),
                    ),
                    start = 0,
                    end = category.length,
                )
            }
        }
    }

    TextP60(
        text = text,
        color = MaterialTheme.theme.colors.primaryText02,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun PodcastRatingOrSpacing(
    rating: RatingState,
    onClickRating: (RatingTappedSource) -> Unit,
) {
    SubcomposeLayout { constraints ->
        val anyRating = RatingState.Loaded(
            ratings = PodcastRatings(
                podcastUuid = "",
                average = null,
                total = null,
            ),
        )
        val dummyRating = subcompose("dummyRating") {
            PodcastRating(
                state = anyRating,
                onClick = {},
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
            )
        }[0].measure(constraints)

        val width = dummyRating.width.toDp()
        val height = dummyRating.height.toDp()

        val ratingUi = subcompose("rating") {
            when (rating) {
                is RatingState.Loaded -> PodcastRating(
                    state = rating,
                    onClick = onClickRating,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                )

                is RatingState.Error, is RatingState.Loading -> Spacer(
                    modifier = Modifier.size(width, height),
                )
            }
        }[0].measure(constraints)

        layout(ratingUi.width, ratingUi.height) {
            ratingUi.place(0, 0)
        }
    }
}

@Composable
private fun PodcastActions(
    isFollowed: Boolean,
    areNotificationsEnabled: Boolean,
    isFundingUrlAvailable: Boolean,
    folderIcon: PodcastFolderIcon,
    onClickFollow: () -> Unit,
    onClickUnfollow: () -> Unit,
    onClickFolder: () -> Unit,
    onClickNotification: () -> Unit,
    onClickDonate: () -> Unit,
    onClickSettings: () -> Unit,
) {
    val transition = updateTransition(targetState = isFollowed)
    var notificationButtonPositionX by remember { mutableIntStateOf(0) }
    SubcomposeLayout { constraints ->
        val controls = subcompose("controls") {
            val alpha by transition.animateFloat(
                label = "controlsAlpha",
                transitionSpec = { if (targetState) followAlphaInSpec else followAlphaOutSpec },
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
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        notificationButtonPositionX = coordinates.positionInParent().x.roundToInt()
                    },
                    onClick = {
                        if (!transition.isRunning) {
                            onClickNotification()
                        }
                    },
                )
                if (isFundingUrlAvailable) {
                    Spacer(
                        modifier = Modifier.size(24.dp),
                    )
                }
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
                modifier = Modifier.padding(horizontal = 52.dp, vertical = 10.dp),
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

        val donateButton = subcompose("donateButton") {
            val description = stringResource(LR.string.donate)

            val backgroundColor = Color.Transparent
            val iconColor by transition.animateColor(
                label = "donateButtonIconColor",
                transitionSpec = { followColorSpec },
                targetValueByState = { followed -> if (followed) MaterialTheme.theme.colors.primaryIcon03 else MaterialTheme.theme.colors.primaryIcon02 },
            )

            val cornerRadius by transition.animateDp(
                label = "donateButtonCornerRadius",
                transitionSpec = { followDpSpec },
                targetValueByState = { followed -> if (followed) 0.dp else 8.dp },
            )

            val borderColor by transition.animateColor(
                label = "donateButtonBorderColor",
                transitionSpec = { followColorSpec },
                targetValueByState = { followed -> if (followed) Color.Transparent else MaterialTheme.theme.colors.primaryIcon02 },
            )

            val buttonOffset by transition.animateIntOffset(
                label = "donateButtonOffset",
                transitionSpec = { followIntOffsetSpec },
                targetValueByState = { followed ->
                    if (followed) {
                        IntOffset(16.dp.roundToPx(), 4.dp.roundToPx())
                    } else {
                        IntOffset(8.dp.roundToPx(), 0)
                    }
                },
            )

            val buttonSize by transition.animateDp(
                label = "donateButtonSize",
                transitionSpec = { followDpSpec },
                targetValueByState = { followed -> if (followed) 24.dp else dummyButtonHeightDp },
            )

            val iconSize by transition.animateDp(
                label = "donateButtonIconSize",
                transitionSpec = { followDpSpec },
                targetValueByState = { followed -> if (followed) 24.dp else 20.dp },
            )

            val borderWidth by transition.animateDp(
                label = "donateButtonBorderWidth",
                transitionSpec = { followDpSpec },
                targetValueByState = { followed -> if (followed) 0.dp else 2.dp },
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { buttonOffset }
                    .size(buttonSize)
                    .background(backgroundColor, RoundedCornerShape(cornerRadius))
                    .border(
                        width = borderWidth,
                        color = borderColor,
                        shape = RoundedCornerShape(cornerRadius),
                    )
                    .clip(RoundedCornerShape(cornerRadius))
                    .clickable(
                        indication = if (isFollowed) controlActionRipple else buttonRipple,
                        interactionSource = null,
                        onClick = onClickDonate,
                    )
                    .clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = description
                    },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_donate_coin),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .size(iconSize),
                )
            }
        }[0].measure(constraints)

        val followButton = subcompose("followButton") {
            val backgroundColor by transition.animateColor(
                label = "backgroundColor",
                transitionSpec = { followColorSpec },
                targetValueByState = { followed -> if (followed) MaterialTheme.theme.colors.support02 else Color.Transparent },
            )
            val borderColor by transition.animateColor(
                label = "borderColor",
                transitionSpec = { followColorSpec },
                targetValueByState = { followed -> if (followed) MaterialTheme.theme.colors.support02 else MaterialTheme.theme.colors.primaryIcon02 },
            )
            val cornerRadius by transition.animateDp(
                label = "cornerRadius",
                transitionSpec = { followDpSpec },
                targetValueByState = { followed -> if (followed) 12.dp else 8.dp },
            )
            val buttonWidth by transition.animateDp(
                label = "buttonWidth",
                transitionSpec = { followDpSpec },
                targetValueByState = { followed -> if (followed) 32.dp else dummyButtonWidthDp },
            )
            val buttonHeight by transition.animateDp(
                label = "buttonHeight",
                transitionSpec = { followDpSpec },
                targetValueByState = { followed -> if (followed) 32.dp else dummyButtonHeightDp },
            )
            val buttonPadding by transition.animateDp(
                label = "buttonPadding",
                transitionSpec = { followDpSpec },
                targetValueByState = { followed -> if (followed) 4.dp else 0.dp },
            )
            val buttonOffset by transition.animateIntOffset(
                label = "buttonOffset",
                transitionSpec = { followIntOffsetSpec },
                targetValueByState = { followed -> if (followed) IntOffset(unfollowButtonOffsetX, 0) else IntOffset.Zero },
            )
            val textAlpha by transition.animateFloat(
                label = "textAlpha",
                transitionSpec = { followFloatSpec },
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
            val donateButtonWidth = if (isFollowed || !isFundingUrlAvailable) 0 else donateButton.width
            val followButtonX = ((width - (followButton.width + donateButtonWidth)) / 2f).roundToInt()
            val followButtonY = ((height - followButton.height) / 2f).roundToInt()
            // Donate button relative to notification button when podcast is followed and relative to follow button when podcast is not followed
            val donateButtonX = if (isFollowed) notificationButtonPositionX + 24.dp.roundToPx() else followButtonX + followButton.width
            followButton.place(x = followButtonX, y = followButtonY)
            if (isFundingUrlAvailable) {
                donateButton.place(x = donateButtonX, y = followButtonY)
            }
        }
    }
}

@Composable
private fun ActionButton(
    @DrawableRes iconId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(iconId),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon03),
        modifier = modifier
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
private fun PodcastDetails(
    description: AnnotatedString,
    podcastInfoState: PodcastInfoState,
    isDescriptionExpanded: Boolean,
    onClickShowNotes: () -> Unit,
    onClickWebsiteLink: () -> Unit,
) {
    Column {
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        ExpandableText(
            text = description,
            overflowText = stringResource(LR.string.see_more),
            isExpanded = isDescriptionExpanded,
            style = detailsInfoTextStyle.copy(
                color = MaterialTheme.theme.colors.primaryText01,
            ),
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = null,
                    onClick = onClickShowNotes,
                ),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        PodcastInfoView(
            state = podcastInfoState,
            onWebsiteLinkClick = onClickWebsiteLink,
        )
    }
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
                val imageHeightPx = if (useBlurredArtwork) {
                    imageSizePx
                } else {
                    imageSizePx - imageBottomOffsetPx
                }
                val const = constraints.copy(
                    minWidth = imageSizePx,
                    maxWidth = imageSizePx,
                    minHeight = imageHeightPx,
                    maxHeight = imageHeightPx,
                )

                val placeable = measurable.measure(const)
                val width = const.constrainWidth(placeable.width)
                val height = const.constrainHeight(placeable.height)
                val offset = if (useBlurredArtwork) {
                    imageBottomOffsetPx
                } else {
                    0
                }
                layout(width, height - offset) { placeable.place(0, -offset) }
            }
            .blurOrScrim(useBlur = useBlurredArtwork),
    )
}

@Composable
private fun ImageOrPreview(
    uuid: String,
    onArtworkAvailable: () -> Unit,
    modifier: Modifier = Modifier,
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

val detailsInfoTextStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 22.sp,
)

private val buttonRipple = ripple()
private val controlActionRipple = ripple(bounded = false)

private val followColorSpec = tween<Color>(durationMillis = 450, easing = EaseOutCubic)
private val followDpSpec = tween<Dp>(durationMillis = 450, easing = EaseOutCubic)
private val followIntOffsetSpec = tween<IntOffset>(durationMillis = 450, easing = EaseOutCubic)
private val followFloatSpec = tween<Float>(durationMillis = 450, easing = EaseOutCubic)
private val followAlphaInSpec = tween<Float>(durationMillis = 250, delayMillis = 300, easing = EaseOutCubic)
private val followAlphaOutSpec = tween<Float>(durationMillis = 200, delayMillis = 0, easing = EaseOutCubic)

private val coverSizeSpec = spring<Dp>(
    dampingRatio = 0.65f,
    stiffness = 100f,
    visibilityThreshold = Dp.VisibilityThreshold,
)
private val categoriesEnterTransition = run {
    val fadeIn = fadeIn(
        animationSpec = spring(stiffness = 200f, visibilityThreshold = 0.001f),
    )
    val expand = expandVertically(
        animationSpec = spring(stiffness = 200f, visibilityThreshold = IntSize.VisibilityThreshold),
        expandFrom = Alignment.Top,
    )
    fadeIn + expand
}
private val categoriesExitTransition = run {
    val fadeOut = fadeOut(
        animationSpec = spring(stiffness = 200f, visibilityThreshold = 0.001f),
    )
    val shrink = shrinkVertically(
        animationSpec = spring(stiffness = 200f, visibilityThreshold = IntSize.VisibilityThreshold),
        shrinkTowards = Alignment.Top,
    )
    fadeOut + shrink
}
private val chevronRotationSpec = spring<Float>(
    stiffness = 200f,
    visibilityThreshold = 0.001f,
)
private val headerInTranstion = expandVertically(
    animationSpec = spring(stiffness = 200f, visibilityThreshold = IntSize.VisibilityThreshold),
    expandFrom = Alignment.Top,
)
private val headerOutTranstion = shrinkVertically(
    animationSpec = spring(stiffness = 200f, visibilityThreshold = IntSize.VisibilityThreshold),
    shrinkTowards = Alignment.Top,
)

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

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun PodcastHeaderPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    var isFollowed by remember { mutableStateOf(true) }
    var isHeaderExpanded by remember { mutableStateOf(true) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    AppThemeWithBackground(themeType) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            PodcastHeader(
                uuid = "uuid",
                title = "The Pitchfork Review",
                category = "Music",
                author = "Pitchfork",
                description = AnnotatedString(
                    """
                    |Savor & Stir is a culinary podcast exploring flavors, techniques, and food stories from chefs and home cooks.
                    |Each episode serves up tips, trends, and recipes to inspire your kitchen adventures.
                    |Whether you cook or just love to eat, join us for a delicious journey!
                    """.trimMargin().lines().joinToString(separator = " "),
                ),
                podcastInfoState = PodcastInfoState(
                    author = "Pocket Casts",
                    link = "pocketcasts.com",
                    schedule = "Every two weeks",
                    next = "Meaning of life",
                ),
                rating = RatingState.Loaded(
                    ratings = PodcastRatings(
                        podcastUuid = "uuid",
                        average = 3.5,
                        total = 42_000,
                    ),
                ),
                isFollowed = isFollowed,
                areNotificationsEnabled = true,
                isFundingUrlAvailable = true,
                folderIcon = PodcastFolderIcon.BuyFolders,
                isHeaderExpanded = isHeaderExpanded,
                isDescriptionExpanded = isDescriptionExpanded,
                contentPadding = PaddingValues(
                    top = 48.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                useBlurredArtwork = false,
                onClickCategory = {},
                onClickRating = {},
                onClickFollow = { isFollowed = true },
                onClickUnfollow = { isFollowed = false },
                onClickFolder = {},
                onClickNotification = {},
                onClickDonate = {},
                onClickSettings = {},
                onClickWebsiteLink = {},
                onToggleHeader = { isHeaderExpanded = !isHeaderExpanded },
                onToggleDescription = { isDescriptionExpanded = !isDescriptionExpanded },
                onLongClickArtwork = {},
                onArtworkAvailable = {},
            )
        }
    }
}
