package au.com.shiftyjelly.pocketcasts.sharing.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PagerDotIndicator
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.sharing.social.PlatformBar
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VerticalSharePage(
    shareTitle: String,
    shareDescription: String,
    shareColors: ShareColors,
    socialPlatforms: Set<SocialPlatform>,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onShareToPlatform: (SocialPlatform, VisualCardType) -> Unit,
    middleContent: @Composable (VisualCardType, Modifier) -> Unit,
) = Box {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(shareColors.background),
    ) {
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier
                .weight(0.2f)
                .fillMaxSize(),
        ) {
            CloseButton(
                shareColors = shareColors,
                onClick = onClose,
                modifier = Modifier.padding(top = 12.dp, end = 12.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                TextH30(
                    text = shareTitle,
                    textAlign = TextAlign.Center,
                    color = shareColors.backgroundPrimaryText,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(
                    modifier = Modifier.height(8.dp),
                )
                TextH40(
                    text = shareDescription,
                    textAlign = TextAlign.Center,
                    color = shareColors.backgroundSecondaryText,
                    modifier = Modifier.sizeIn(maxWidth = 220.dp),
                )
            }
        }
        val pagerState = rememberPagerState(pageCount = { CardType.visualEntries.size })
        Box(
            contentAlignment = Alignment.Center,
            content = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(0.92f),
                    ) {
                        val expectedVerticalCardPadding = remember(maxWidth, maxHeight) { (maxWidth - maxHeight / 1.5f) / 2 }
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { pageIndex ->
                            val cardType = CardType.visualEntries[pageIndex]
                            val modifier = Modifier
                                .fillMaxSize()
                                .then(if (cardType != CardType.Vertical) Modifier.padding(horizontal = expectedVerticalCardPadding) else Modifier)
                            middleContent(cardType, modifier)
                        }
                    }
                    PagerDotIndicator(
                        state = pagerState,
                        activeDotColor = shareColors.pagerIndicatorActive,
                        inactiveDotColor = shareColors.pagerIndicatorInactive,
                        modifier = Modifier.weight(0.08f),
                    )
                }
            },
            modifier = Modifier
                .weight(0.65f)
                .fillMaxSize(),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(0.15f),
        ) {
            PlatformBar(
                platforms = socialPlatforms,
                shareColors = shareColors,
                onClick = { platform ->
                    onShareToPlatform(platform, CardType.visualEntries[pagerState.currentPage])
                },
            )
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter),
        snackbar = { data -> SharingThemedSnackbar(data, shareColors) },
    )
}

@Composable
internal fun HorizontalSharePage(
    shareTitle: String,
    shareDescription: String,
    shareColors: ShareColors,
    socialPlatforms: Set<SocialPlatform>,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onShareToPlatform: (SocialPlatform, VisualCardType) -> Unit,
    middleContent: @Composable BoxScope.() -> Unit,
) = Box {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(shareColors.background),
    ) {
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier
                .weight(0.25f)
                .fillMaxSize(),
        ) {
            CloseButton(
                shareColors = shareColors,
                onClick = onClose,
                modifier = Modifier.padding(top = 12.dp, end = 12.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                TextH30(
                    text = shareTitle,
                    textAlign = TextAlign.Center,
                    color = shareColors.backgroundPrimaryText,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(
                    modifier = Modifier.height(8.dp),
                )
                TextH40(
                    text = shareDescription,
                    textAlign = TextAlign.Center,
                    color = shareColors.backgroundSecondaryText,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(0.75f)
                .fillMaxSize(),
        ) {
            Spacer(
                modifier = Modifier.weight(0.1f),
            )
            Box(
                contentAlignment = Alignment.Center,
                content = middleContent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Spacer(
                modifier = Modifier.weight(0.1f),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                PlatformBar(
                    platforms = socialPlatforms,
                    shareColors = shareColors,
                    onClick = { platform ->
                        onShareToPlatform(platform, CardType.Horizontal)
                    },
                )
            }
            Spacer(
                modifier = Modifier.weight(0.1f),
            )
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter),
        snackbar = { data -> SharingThemedSnackbar(data, shareColors) },
    )
}

@Composable
private fun SharingThemedSnackbar(
    snackbarData: SnackbarData,
    shareColors: ShareColors,
    modifier: Modifier = Modifier,
) {
    val actionLabel = snackbarData.actionLabel
    val actionComposable: (@Composable () -> Unit)? = if (actionLabel != null) {
        @Composable {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = shareColors.snackbarText),
                onClick = { snackbarData.performAction() },
                content = { TextH50(actionLabel) },
            )
        }
    } else {
        null
    }
    Snackbar(
        modifier = modifier.padding(12.dp),
        content = { TextH50(snackbarData.message, color = shareColors.snackbarText) },
        action = actionComposable,
        actionOnNewLine = false,
        shape = MaterialTheme.shapes.small,
        backgroundColor = shareColors.snackbar,
        contentColor = shareColors.snackbar,
        elevation = 0.dp,
    )
}
