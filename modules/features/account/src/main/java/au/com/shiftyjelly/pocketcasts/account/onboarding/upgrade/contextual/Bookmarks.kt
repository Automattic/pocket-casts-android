package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.random.Random
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR

private data class AnimationParams(
    val shouldStart: Boolean,
    val centerOffset: IntOffset,
    val rotationOffset: Int,
)

@Composable
fun BookmarksAnimation(
    modifier: Modifier = Modifier,
    bookmarks: List<BookmarkConfig> = demoBookmarks,
) {
    val centerOffsetTolerance = LocalDensity.current.run {
        24.dp.toPx().toInt()
    }
    val animationTriggers = remember {
        List(bookmarks.size) {
            mutableStateOf(
                AnimationParams(
                    shouldStart = false,
                    centerOffset = IntOffset(Random.nextInt(centerOffsetTolerance), Random.nextInt(centerOffsetTolerance)),
                    rotationOffset = Random.nextInt(10),
                ),
            )
        }
    }

    LaunchedEffect("animations") {
        for (i in bookmarks.indices) {
            animationTriggers[i].value = animationTriggers[i].value.copy(shouldStart = true)
            delay(800)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .semantics(mergeDescendants = true) { role = Role.Image }
            .focusable(false),
        contentAlignment = Alignment.TopCenter,
    ) {
        val itemWidth = this.maxWidth * .4f
        bookmarks.forEachIndexed { index, item ->
            val animParams = animationTriggers[index].value
            val startRotation = if (index % 2 == 0) {
                30 - animParams.rotationOffset
            } else {
                -30 + animParams.rotationOffset
            }
            val endRotation = if (index % 2 == 0) {
                0 + animParams.rotationOffset
            } else {
                0 - animParams.rotationOffset
            }
            Bookmark(
                modifier = Modifier
                    .widthIn(max = itemWidth)
                    .aspectRatio(1f),
                bookmarkConfig = item,
                startAnimation = animationTriggers[index].value.shouldStart,
                centerOffset = animParams.centerOffset,
                startRotation = startRotation,
                endRotation = endRotation,
            )
        }
    }
}

private val demoBookmarks = listOf(
    BookmarkConfig(
        backgroundStartColor = Color(0xffE4D820),
        backgroundEndColor = Color(0xffE8A92C),
        artworkResId = IR.drawable.artwork_0,
        text = "Amazing quote!",
        timestamp = "19:50",
    ),
    BookmarkConfig(
        backgroundStartColor = Color(0xffFF9D00),
        backgroundEndColor = Color(0xffEC4034),
        artworkResId = IR.drawable.artwork_3,
        text = "This bit cracks me up",
        timestamp = "6:10",
    ),
    BookmarkConfig(
        backgroundStartColor = Color(0xff27D9E9),
        backgroundEndColor = Color(0xff0202FE),
        artworkResId = IR.drawable.artwork_4,
        text = "Love this part!",
        timestamp = "6:45",
    ),
)

data class BookmarkConfig(
    val backgroundStartColor: Color,
    val backgroundEndColor: Color,
    @DrawableRes val artworkResId: Int,
    val text: String,
    val timestamp: String,
)

@Composable
private fun Bookmark(
    bookmarkConfig: BookmarkConfig,
    modifier: Modifier = Modifier,
    startAnimation: Boolean = true,
    startRotation: Int = 0,
    endRotation: Int = 0,
    centerOffset: IntOffset = IntOffset(0, 0),
    startScale: Float = 1.5f,
) {
    val transition = updateTransition(startAnimation, "bookmarkAnimation")
    val rotationAnim by transition.animateInt(
        transitionSpec = { tween(durationMillis = 600, easing = LinearEasing) },
    ) { visible ->
        if (visible) {
            endRotation
        } else {
            startRotation
        }
    }
    val alphaAnim by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 600, easing = LinearOutSlowInEasing) },
    ) { visible ->
        if (visible) {
            1f
        } else {
            0f
        }
    }
    val scaleAnim by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 600, easing = FastOutLinearInEasing) },
    ) { visible ->
        if (visible) {
            1f
        } else {
            startScale
        }
    }

    Column(
        modifier = modifier
            .offset {
                IntOffset(centerOffset.x, centerOffset.y)
            }
            .graphicsLayer {
                rotationZ = rotationAnim.toFloat()
                alpha = alphaAnim
                scaleX = scaleAnim
                scaleY = scaleAnim
                transformOrigin = TransformOrigin.Center
            }
            .clip(RoundedCornerShape(13.dp))
            .background(
                brush = Brush.linearGradient(colors = listOf(bookmarkConfig.backgroundStartColor, bookmarkConfig.backgroundEndColor)),
                shape = RoundedCornerShape(13.dp),
            )
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        Image(
            modifier = Modifier
                .size(77.dp)
                .clip(RoundedCornerShape(8.dp)),
            painter = painterResource(bookmarkConfig.artworkResId),
            contentDescription = "",
        )
        TextP40(
            text = bookmarkConfig.text,
            color = Color.White,
            disableAutoScale = true,
        )
        Row(
            modifier = Modifier
                .height(36.dp)
                .wrapContentWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(color = Color.White, shape = RoundedCornerShape(18.dp))
                .padding(horizontal = 17.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextP40(
                text = bookmarkConfig.timestamp,
                color = Color.Black,
                disableAutoScale = true,
            )
            Icon(
                painter = painterResource(IR.drawable.ic_play),
                contentDescription = "",
                tint = Color.Black,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewBookmark(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    Column {
        demoBookmarks.forEach {
            Bookmark(
                bookmarkConfig = it,
                modifier = Modifier
                    .widthIn(max = 230.dp)
                    .aspectRatio(1f),
            )
        }
    }
}
