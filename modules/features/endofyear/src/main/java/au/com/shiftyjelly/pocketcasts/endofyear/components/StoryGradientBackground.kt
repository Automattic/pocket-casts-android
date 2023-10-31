package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp

@Composable
fun StoryBlurredBackground(
    offset: Offset,
    rotate: Float = 0f,
    style: StoryBlurredBackgroundStyle = StoryBlurredBackgroundStyle.Default,
) {
    Box(
        Modifier.wrapContentWidth(unbounded = true)
    ) {
        val context = LocalContext.current
        val screenWidthInPx = LocalView.current.width
        val screenWidthInDp = screenWidthInPx.pxToDp(context)
        Box(
            modifier = Modifier
                .size((screenWidthInDp * 2f).dp, (screenWidthInDp * 2f).dp)
                .offset(
                    x = offset.x.toInt().pxToDp(context).dp,
                    y = offset.y.toInt().pxToDp(context).dp
                )
                .rotate(rotate)
        ) {
            Image(
                painterResource(style.imgResId()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(style.alpha())
            )
        }
    }
}

enum class StoryBlurredBackgroundStyle {
    Default,
    Plus;

    fun imgResId() = when (this) {
        Default -> R.drawable.story_blurred_background_default
        Plus -> R.drawable.story_blurred_background_gold
    }

    fun alpha() = when (this) {
        Default -> 0.6f
        Plus -> 0.8f
    }
}
