package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.images.HorizontalLogo
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class AnimationState {
    Appearing,
    Disappearing
}

@Composable
fun BestAppAnimation(
    animationState: AnimationState,
    modifier: Modifier = Modifier
) {

    val transition = updateTransition(animationState)
    val contentYOffset by transition.animateDp(label = "contentYOffset") {
        when (it) {
            AnimationState.Appearing -> 32.dp
            AnimationState.Disappearing -> (-32).dp
        }
    }
    val mainTextAlpha by transition.animateFloat(
        label = "mainTextAlpha",
        transitionSpec = {
            tween(durationMillis = 500, delayMillis = 300)
        }
    ) {
        when (it) {
            AnimationState.Appearing -> 0f
            AnimationState.Disappearing -> 1f
        }
    }
    val mainTextYOffset by transition.animateDp(
        label = "mainTextYOffset",
        transitionSpec = {
            tween(durationMillis = 500, delayMillis = 300)
        }
    ) {
        when (it) {
            AnimationState.Appearing -> 12.dp
            AnimationState.Disappearing -> (-12).dp
        }
    }
    val secondaryTextAlpha by transition.animateFloat(
        label = "secondaryTextAlpha",
        transitionSpec = {
            tween(durationMillis = 500, delayMillis = 600)
        }
    ) {
        when (it) {
            AnimationState.Appearing -> 0f
            AnimationState.Disappearing -> 1f
        }
    }
    val secondaryTextYOffset by transition.animateDp(
        label = "secondaryTextYOffset",
        transitionSpec = {
            tween(durationMillis = 500, delayMillis = 600)
        }
    ) {
        when (it) {
            AnimationState.Appearing -> 12.dp
            AnimationState.Disappearing -> (-12).dp
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalLogo(
            modifier = Modifier.height(32.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        BestAppArtworkCollage(
            modifier = Modifier.offset {
                IntOffset(x = 0, y = contentYOffset.roundToPx())
            }
        )
        Spacer(modifier = Modifier.weight(1f))
        TextH10(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
                .offset {
                    IntOffset(x = 0, y = mainTextYOffset.roundToPx())
                }.graphicsLayer {
                    alpha = mainTextAlpha
                },
            text = stringResource(LR.string.onboarding_intro_carousel_best_app_title),
            textAlign = TextAlign.Center,
            color = MaterialTheme.theme.colors.primaryText01,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextP40(
            fontSize = 15.sp,
            lineHeight = 21.sp,
            text = stringResource(LR.string.onboarding_intro_carousel_pc_user),
            color = MaterialTheme.theme.colors.primaryText02,
            modifier = Modifier.offset {
                IntOffset(x = 0, y = secondaryTextYOffset.roundToPx())
            }.graphicsLayer {
                alpha = secondaryTextAlpha
            }
        )
    }
}

@Composable
private fun BestAppArtworkCollage(
    modifier: Modifier = Modifier
) {
    Layout(
        modifier = modifier.height(240.dp),
        content = {
            Image(
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer {
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(4.dp)
                        clip = true
                    },
                painter = painterResource(IR.drawable.artwork_13),
                contentDescription = ""
            )
            Image(
                modifier = Modifier
                    .size(69.dp)
                    .graphicsLayer {
                        shadowElevation = 4.dp.toPx()
                        shape = RoundedCornerShape(4.dp)
                        clip = true
                    },
                painter = painterResource(IR.drawable.artwork_14),
                contentDescription = ""
            )
            Image(
                modifier = Modifier
                    .size(69.dp)
                    .graphicsLayer {
                        shadowElevation = 4.dp.toPx()
                        shape = RoundedCornerShape(4.dp)
                        clip = true
                    },
                painter = painterResource(IR.drawable.artwork_11),
                contentDescription = ""
            )
            Image(
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer {
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(4.dp)
                        clip = true
                    },
                painter = painterResource(IR.drawable.artwork_3),
                contentDescription = ""
            )
            Image(
                modifier = Modifier
                    .size(69.dp)
                    .graphicsLayer {
                        shadowElevation = 4.dp.toPx()
                        shape = RoundedCornerShape(4.dp)
                        clip = true
                    },
                painter = painterResource(IR.drawable.artwork_16),
                contentDescription = ""
            )
            Image(
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer {
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(4.dp)
                        clip = true
                    },
                painter = painterResource(IR.drawable.artwork_4),
                contentDescription = ""
            )
            Image(
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer {
                        shadowElevation = 4.dp.toPx()
                        shape = RoundedCornerShape(4.dp)
                        clip = true
                    },
                painter = painterResource(IR.drawable.artwork_15),
                contentDescription = ""
            )
        }) { measurables, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {
            val placeables = measurables.map { it.measure(Constraints()) }

            placeables.forEachIndexed { index, placeable ->
                when (index) {
                    0 -> placeable.placeRelative(
                        x = placeable.width / -2,
                        y = 90.dp.roundToPx(),
                    )

                    1 -> placeable.placeRelative(
                        x = 72.dp.roundToPx(),
                        y = 0
                    )

                    2 -> placeable.placeRelative(
                        x = 98.dp.roundToPx(),
                        y = constraints.maxHeight - placeable.height
                    )

                    3 -> placeable.placeRelative(
                        x = 121.dp.roundToPx(),
                        y = 36.dp.roundToPx(),
                        zIndex = 1f
                    )

                    4 -> placeable.placeRelative(
                        x = 242.dp.roundToPx(),
                        y = constraints.maxHeight - placeable.height - 20.dp.roundToPx()
                    )

                    5 -> placeable.placeRelative(
                        x = constraints.maxWidth - placeable.width - 27.dp.roundToPx(),
                        y = 0,
                        zIndex = 1f
                    )

                    6 -> placeable.placeRelative(
                        x = constraints.maxWidth - placeable.width / 2,
                        y = 90.dp.roundToPx()
                    )
                }
            }
        }
    }
}

@Composable
fun CustomizationIsInsaneAnimation(modifier: Modifier = Modifier) {
}

@Composable
fun OrganizingPodcastsAnimation(modifier: Modifier = Modifier) {
}

@Preview
@Composable
private fun PreviewBestAppAnim() = AppThemeWithBackground(Theme.ThemeType.LIGHT) {
    val animStateIndex by produceState(0) {
        while (true) {
            delay(2000)
            value = (value + 1) % AnimationState.entries.size
        }
    }
    BestAppAnimation(modifier = Modifier.fillMaxWidth(), animationState = AnimationState.entries[animStateIndex])
}