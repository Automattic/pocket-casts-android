package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowCloseButton
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

private const val AnimationDuration = 4000
private const val AnimationHeight = 20f
private const val AnimationDelay = 1000
private const val ScaleFactor = 1.4f
private const val PaddingTop = 80

@Composable
fun SlumberStudiosHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    fullModal: Boolean = true,
) {
    val width = LocalConfiguration.current.screenWidthDp - 48
    val size = width / slumberStudioImageResItems.size
    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteFloatTransition")
    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .clipToBounds()
                .height((size * 2 + PaddingTop).dp)
                .then(if (fullModal) Modifier else Modifier.padding(top = PaddingTop.dp)),
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .scale(ScaleFactor),
            ) {
                slumberStudioImageResItems.forEachIndexed { index, imageRes ->
                    val position by infiniteTransition.animateFloat(
                        initialValue = -AnimationHeight,
                        targetValue = AnimationHeight,
                        animationSpec = infiniteRepeatable(
                            initialStartOffset = StartOffset(AnimationDelay * index + 1),
                            animation = tween(AnimationDuration, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "TopBottomAnimation",
                    )

                    SlumberStudiosImage(
                        imageRes = imageRes,
                        position = position,
                        modifier = Modifier
                            .size(size.dp),
                    )
                }
            }
        }

        if (!fullModal) {
            RowCloseButton(
                onClose = onClose,
                tintColor = if (MaterialTheme.theme.isLight) Color.Black else Color.White,
            )
        }
    }
}

@Composable
private fun SlumberStudiosImage(
    @DrawableRes imageRes: Int,
    position: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Box(
            Modifier
                .offset(y = position.dp),
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = modifier.padding(4.dp),
            )
        }
    }
}

private val slumberStudioImageResItems = listOf(
    IR.drawable.slumber_studios_send_me_to_sleep,
    IR.drawable.slumber_studios_deep_sleep_sounds,
    IR.drawable.slumber_studios_get_sleepy,
    IR.drawable.slumber_studios_the_sleepy_bookshelf,
)

@Preview
@Composable
fun SlumberStudiosImagesPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SlumberStudiosHeader(
            onClose = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
