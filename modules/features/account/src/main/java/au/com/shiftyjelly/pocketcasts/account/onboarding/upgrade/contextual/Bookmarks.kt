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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR

private data class AnimationParams(
    val shouldStart: Boolean,
    val centerOffset: IntOffset,
    val rotationOffset: Int,
)

private const val WIDTH_DP = 202

@Composable
fun BookmarksAnimation(
    modifier: Modifier = Modifier,
    bookmarks: List<BookmarkConfig> = predefinedBookmarks,
) {
    val centerYOffsetPx = LocalDensity.current.run {
        8.dp.toPx().toInt()
    }
    val animationTriggers = remember {
        List(bookmarks.size) {
            val totalOffset = (bookmarks.size - 1) * centerYOffsetPx
            mutableStateOf(
                AnimationParams(
                    shouldStart = false,
                    centerOffset = IntOffset(x = 0, y = totalOffset - (it * centerYOffsetPx)),
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

    Layout(
        modifier = modifier
            .semantics(mergeDescendants = true) { role = Role.Image }
            .focusable(false),
        content = {
            bookmarks.forEachIndexed { index, item ->
                val animParams = animationTriggers[index].value
                val rotationDirection = if (index % 2 == 0) {
                    1
                } else {
                    -1
                }
                val startRotation = (30 + abs(item.endRotationDegree)).toInt() * rotationDirection
                val endRotation = item.endRotationDegree.toInt()
                Bookmark(
                    modifier = Modifier
                        .aspectRatio(WIDTH_DP / 219f),
                    bookmarkConfig = item,
                    startAnimation = animationTriggers[index].value.shouldStart,
                    centerOffset = animParams.centerOffset,
                    startRotation = startRotation,
                    endRotation = endRotation,
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map {
            it.measure(
                Constraints.fixedWidth(WIDTH_DP.dp.roundToPx()),
            )
        }
        val maxHeight = placeables.maxOf { it.height }
        layout(constraints.maxWidth, maxHeight) {
            placeables.forEachIndexed { index, item ->
                item.placeRelative(
                    x = (constraints.maxWidth - item.width) / 2,
                    y = 0,
                )
            }
        }
    }
}

private val predefinedBookmarks = listOf(
    BookmarkConfig(
        backgroundStartColor = Color(0xffE4D820),
        backgroundEndColor = Color(0xffE8A92C),
        artworkResId = IR.drawable.artwork_12,
        text = "Amazing quote!",
        timestamp = "19:05",
        endRotationDegree = 9.6f,
    ),
    BookmarkConfig(
        backgroundStartColor = Color(0xffFF9D00),
        backgroundEndColor = Color(0xffEC4034),
        artworkResId = IR.drawable.artwork_10,
        text = "This bit cracks me up",
        timestamp = "6:45",
        endRotationDegree = -7.48f,
    ),
    BookmarkConfig(
        backgroundStartColor = Color(0xff27D9E9),
        backgroundEndColor = Color(0xff0202FE),
        artworkResId = IR.drawable.artwork_11,
        text = "Love this part!",
        timestamp = "6:45",
        endRotationDegree = 5.72f,
    ),
)

data class BookmarkConfig(
    val backgroundStartColor: Color,
    val backgroundEndColor: Color,
    @DrawableRes val artworkResId: Int,
    val text: String,
    val timestamp: String,
    val endRotationDegree: Float,
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

    val gradientStartPx = LocalDensity.current.run {
        24.dp.toPx()
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
                brush = Brush.linearGradient(
                    colors = listOf(bookmarkConfig.backgroundStartColor, bookmarkConfig.backgroundEndColor),
                    start = Offset(x = gradientStartPx, y = gradientStartPx),
                ),
                shape = RoundedCornerShape(13.dp),
            )
            .padding(vertical = 25.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Image(
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(8.dp)),
            painter = painterResource(bookmarkConfig.artworkResId),
            contentDescription = "",
        )
        TextP40(
            text = bookmarkConfig.text,
            color = Color.White,
            disableAutoScale = true,
            fontWeight = FontWeight.W500,
            lineHeight = 20.sp,
        )
        Row(
            modifier = Modifier
                .height(36.dp)
                .wrapContentWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(color = Color.White, shape = RoundedCornerShape(18.dp))
                .padding(horizontal = 17.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
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
        predefinedBookmarks.forEach {
            Bookmark(
                bookmarkConfig = it,
                modifier = Modifier
                    .widthIn(max = 230.dp)
                    .aspectRatio(1f),
            )
        }
    }
}
