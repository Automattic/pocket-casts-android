package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.Confetti
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryAppLogo
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryBlurredBackground
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryBlurredBackgroundStyle
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryButton
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryEpilogue
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val HeartImageSize = 72.dp

@Composable
fun StoryEpilogueView(
    story: StoryEpilogue,
    userTier: UserTier,
    onReplayClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor)
    ) {
        val context = LocalView.current.context
        StoryBlurredBackground(
            offset = Offset(
                -maxWidth.value.toInt().dpToPx(context) * 0.4f,
                -maxHeight.value.toInt().dpToPx(context) * 0.4f
            ),
            style = blurredBackgroundStyle(userTier),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 30.dp)
        ) {
            Spacer(modifier = modifier.weight(1f))

            PulsatingHeart()

            Spacer(modifier = modifier.weight(0.34f))

            PrimaryText(story)

            Spacer(modifier = modifier.weight(0.16f))

            SecondaryText(story)

            Spacer(modifier = modifier.weight(0.16f))

            ReplayButton(onClick = onReplayClicked)

            Spacer(modifier = modifier.weight(1f))

            StoryAppLogo()
        }
        Confetti {}
    }
}

@Composable
private fun PulsatingHeart(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(modifier = Modifier.scale(scale)) {
        Image(
            painter = painterResource(id = R.drawable.heart_rainbow),
            contentDescription = null,
            modifier = modifier
                .size(HeartImageSize)
        )
    }
}

@Composable
private fun PrimaryText(
    story: StoryEpilogue,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(id = LR.string.end_of_year_story_epilogue_title)
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryEpilogue,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(id = LR.string.end_of_year_story_epilogue_subtitle)
    StorySecondaryText(text = text, color = story.subtitleColor, modifier = modifier)
}

@Composable
private fun ReplayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StoryButton(
        text = stringResource(id = LR.string.end_of_year_replay),
        onClick = onClick,
        textIcon = IR.drawable.ic_retry,
        modifier = modifier.width(250.dp)
    )
}

@Composable
private fun blurredBackgroundStyle(userTier: UserTier) = when (userTier) {
    UserTier.Patron, UserTier.Plus -> StoryBlurredBackgroundStyle.Plus
    UserTier.Free -> StoryBlurredBackgroundStyle.Default
}

@Preview(name = "Free user")
@Composable
fun EpilogueFreeUserPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        StoryEpilogueView(
            StoryEpilogue(),
            userTier = UserTier.Free,
            onReplayClicked = {}
        )
    }
}

@Preview(name = "Paid user")
@Composable
fun EpiloguePaidUserPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        StoryEpilogueView(
            StoryEpilogue(),
            userTier = UserTier.Plus,
            onReplayClicked = {}
        )
    }
}
