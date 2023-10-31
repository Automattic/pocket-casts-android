package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastLogoWhite
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryBlurredBackground
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryBlurredBackgroundStyle
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryFontFamily
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.disableScale
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryEpilogue
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val HeartImageSize = 72.dp

@Composable
fun StoryEpilogueView(
    story: StoryEpilogue,
    onReplayClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val context = LocalView.current.context
        StoryBlurredBackground(
            offset = Offset(
                -maxWidth.value.toInt().dpToPx(context) * 0.4f,
                -maxHeight.value.toInt().dpToPx(context) * 0.4f
            ),
            style = StoryBlurredBackgroundStyle.Default,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 40.dp)
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

            PodcastLogoWhite()
        }
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
    val context = LocalContext.current
    val currentLocalView = LocalView.current
    val screenWidth = currentLocalView.width.pxToDp(context)
    Button(
        onClick = { onClick() },
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults
            .buttonColors(backgroundColor = Color.White),
        modifier = Modifier.width((screenWidth * 0.6f).dp),
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = Color.Black
        )
        TextP40(
            text = stringResource(id = LR.string.end_of_year_replay),
            color = Color.Black,
            fontFamily = StoryFontFamily,
            disableScale = disableScale(),
            modifier = modifier.padding(2.dp)
        )
    }
}
