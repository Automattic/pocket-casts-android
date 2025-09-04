package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.images.HorizontalLogo
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun BestAppAnimation(
    isAppearing: Boolean,
    disappearAnimDuration: Duration,
    modifier: Modifier = Modifier,
) {
    AnimatedCarouselItemContainer(
        isAppearing = isAppearing,
        disappearAnimDuration = disappearAnimDuration,
        modifier = modifier,
        title = stringResource(LR.string.onboarding_intro_carousel_best_app_title),
        content = {
            HorizontalLogo(
                modifier = Modifier
                    .height(32.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.weight(1f))
            Image(
                modifier = Modifier
                    .fillMaxWidth(),
                painter = painterResource(IR.drawable.intro_story_1),
                contentScale = ContentScale.FillWidth,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.weight(1f))
        },
    )
}

@Composable
fun CustomizationIsInsaneAnimation(
    isAppearing: Boolean,
    disappearAnimDuration: Duration,
    modifier: Modifier = Modifier,
) {
    AnimatedCarouselItemContainer(
        isAppearing = isAppearing,
        disappearAnimDuration = disappearAnimDuration,
        modifier = modifier,
        title = stringResource(LR.string.onboarding_intro_carousel_customization_insane_title),
        content = {
            Spacer(modifier = Modifier.weight(1f))
            val backgroundColor = MaterialTheme.colors.background
            Image(
                modifier = Modifier
                    .fillMaxWidth(.6f)
                    .align(Alignment.CenterHorizontally)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    backgroundColor,
                                    backgroundColor.copy(alpha = .5f),
                                    Color.Transparent,
                                ),
                                startY = 0f,
                                endY = 64.dp.toPx(),
                            ),
                        )
                    },
                contentScale = ContentScale.FillWidth,
                painter = painterResource(IR.drawable.intro_story_2),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.weight(1f))
        },
    )
}

@Composable
fun OrganizingPodcastsAnimation(
    isAppearing: Boolean,
    disappearAnimDuration: Duration,
    modifier: Modifier = Modifier,
) {
    AnimatedCarouselItemContainer(
        isAppearing = isAppearing,
        disappearAnimDuration = disappearAnimDuration,
        modifier = modifier,
        title = stringResource(LR.string.onboarding_intro_carousel_organizing_podcasts_title),
        content = {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                modifier = Modifier
                    .fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                painter = painterResource(IR.drawable.intro_story_3),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.weight(1f))
        },
    )
}

@Composable
private fun AnimatedCarouselItemContainer(
    title: String,
    isAppearing: Boolean,
    modifier: Modifier = Modifier,
    disappearAnimDuration: Duration = 300.milliseconds,
    appearAnimDuration: Duration = 600.milliseconds,
    subTitle: String = stringResource(LR.string.onboarding_intro_carousel_pc_user),
    content: @Composable ColumnScope.() -> Unit,
) {
    var isVisible by remember {
        mutableStateOf(
            !isAppearing,
        )
    }

    LaunchedEffect(isVisible, isAppearing) {
        isVisible = isAppearing
    }

    val transition = updateTransition(isVisible)
    val contentAlpha by transition.animateFloat(
        label = "contentAlpha",
        transitionSpec = {
            tween(
                durationMillis = isVisible.durationMillis(
                    appearAnimDuration = appearAnimDuration,
                    disappearAnimDuration = disappearAnimDuration,
                ),
                easing = isVisible.easing,
            )
        },
    ) {
        it.alpha
    }
    val contentYOffset by transition.animateDp(
        label = "contentYOffset",
        transitionSpec = {
            tween(
                durationMillis = isVisible.durationMillis(
                    appearAnimDuration = appearAnimDuration,
                    disappearAnimDuration = disappearAnimDuration,
                ),
                easing = isVisible.easing,
            )
        },
    ) {
        it.contentOffsetY
    }
    val mainTextAlpha by transition.animateFloat(
        label = "mainTextAlpha",
        transitionSpec = {
            tween(
                durationMillis = isVisible.durationMillis(
                    appearAnimDuration = appearAnimDuration,
                    disappearAnimDuration = disappearAnimDuration,
                ),
                delayMillis = isVisible.textAnimDelay(300.milliseconds),
                easing = isVisible.easing,
            )
        },
    ) {
        it.alpha
    }
    val mainTextYOffset by transition.animateDp(
        label = "mainTextYOffset",
        transitionSpec = {
            tween(
                durationMillis = isVisible.durationMillis(
                    appearAnimDuration = appearAnimDuration,
                    disappearAnimDuration = disappearAnimDuration,
                ),
                delayMillis = isVisible.textAnimDelay(300.milliseconds),
                easing = isVisible.easing,
            )
        },
    ) {
        it.textOffsetY
    }
    val secondaryTextAlpha by transition.animateFloat(
        label = "secondaryTextAlpha",
        transitionSpec = {
            tween(
                durationMillis = isVisible.durationMillis(
                    appearAnimDuration = appearAnimDuration,
                    disappearAnimDuration = disappearAnimDuration,
                ),
                delayMillis = isVisible.textAnimDelay(600.milliseconds),
                easing = isVisible.easing,
            )
        },
    ) {
        it.alpha
    }
    val secondaryTextYOffset by transition.animateDp(
        label = "secondaryTextYOffset",
        transitionSpec = {
            tween(
                durationMillis = isVisible.durationMillis(
                    appearAnimDuration = appearAnimDuration,
                    disappearAnimDuration = disappearAnimDuration,
                ),
                delayMillis = isVisible.textAnimDelay(600.milliseconds),
                easing = isVisible.easing,
            )
        },
    ) {
        it.textOffsetY
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    role = Role.Image
                }
                .weight(1f)
                .fillMaxWidth()
                .offset {
                    IntOffset(x = 0, y = contentYOffset.roundToPx())
                }
                .graphicsLayer {
                    alpha = contentAlpha
                },
        ) {
            content()
        }
        TextH10(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .offset {
                    IntOffset(x = 0, y = mainTextYOffset.roundToPx())
                }
                .graphicsLayer {
                    alpha = mainTextAlpha
                },
            text = title,
            textAlign = TextAlign.Center,
            color = MaterialTheme.theme.colors.primaryText01,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextP40(
            fontSize = 15.sp,
            lineHeight = 21.sp,
            text = subTitle,
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .offset {
                    IntOffset(x = 0, y = secondaryTextYOffset.roundToPx())
                }
                .graphicsLayer {
                    alpha = secondaryTextAlpha
                },
        )
    }
}

private val Boolean.alpha
    get() = if (this) 1f else 0f

private val Boolean.textOffsetY
    get() = if (this) (-6).dp else 6.dp

private val Boolean.contentOffsetY
    get() = if (this) (-16).dp else 16.dp

private val Boolean.easing
    get() = if (this) FastOutSlowInEasing else FastOutLinearInEasing

private fun Boolean.durationMillis(
    appearAnimDuration: Duration,
    disappearAnimDuration: Duration,
) = if (this) appearAnimDuration.inWholeMilliseconds.toInt() else disappearAnimDuration.inWholeMilliseconds.toInt()

private fun Boolean.textAnimDelay(additionalDelay: Duration) = if (this) additionalDelay.inWholeMilliseconds.toInt() else 0

@Preview
@Composable
private fun PreviewBestAppAnim() = AppThemeWithBackground(Theme.ThemeType.LIGHT) {
    BestAppAnimation(modifier = Modifier.fillMaxWidth(), isAppearing = false, disappearAnimDuration = 300.milliseconds)
}

@Preview
@Composable
private fun PreviewCustomizationAppAnim() = AppThemeWithBackground(Theme.ThemeType.LIGHT) {
    CustomizationIsInsaneAnimation(modifier = Modifier.fillMaxWidth(), isAppearing = false, disappearAnimDuration = 300.milliseconds)
}

@Preview
@Composable
private fun PreviewOrganizingAppAnim() = AppThemeWithBackground(Theme.ThemeType.LIGHT) {
    OrganizingPodcastsAnimation(modifier = Modifier.fillMaxWidth(), isAppearing = false, disappearAnimDuration = 300.milliseconds)
}
