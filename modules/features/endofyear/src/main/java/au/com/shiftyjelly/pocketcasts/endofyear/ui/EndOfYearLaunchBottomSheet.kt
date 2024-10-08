package au.com.shiftyjelly.pocketcasts.endofyear.ui

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.BottomSheetContentState
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.ModalBottomSheet
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EndOfYearLaunchBottomSheet(
    parent: ViewGroup,
    modifier: Modifier = Modifier,
    shouldShow: Boolean = true,
    onClick: () -> Unit,
    onExpanded: () -> Unit,
) {
    ModalBottomSheet(
        parent = parent,
        shouldShow = shouldShow,
        onExpanded = onExpanded,
        content = BottomSheetContentState.Content(
            imageContent = { ImageContent(modifier) },
            summaryText = stringResource(LR.string.end_of_year_launch_modal_summary),
            primaryButton = BottomSheetContentState.Content.Button.Primary(
                label = stringResource(LR.string.end_of_year_launch_modal_primary_button_title),
                onClick = { onClick.invoke() },
            ),
        ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageContent(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(Color(0xFFEE661C), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp),
    ) {
        val density = LocalDensity.current
        val shouldTargetHeight = maxWidth > 300.dp
        val targetSize = density.run {
            val size = if (shouldTargetHeight) maxHeight else maxWidth
            size.toPx()
        }

        var isTextSizeComputed by remember { mutableStateOf(false) }
        var fontSize by remember { mutableStateOf(24.sp) }
        var padding by remember { mutableStateOf(0.dp) }

        AnimatedVisibility(
            visible = isTextSizeComputed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                VerticalPager(
                    state = rememberPagerState(initialPage = 1, pageCount = { 3 }),
                    contentPadding = PaddingValues(vertical = padding),
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    PlaybackText(
                        color = Color(0xFFEEB1F4),
                        fontSize = fontSize,
                    )
                }
                Image(
                    painter = painterResource(IR.drawable.end_of_year_2024_sticker_1),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = -90.dp, y = -58.dp)
                        .size(width = 116.dp, height = 88.dp),
                )
            }
        }

        // Use an invisible 'PLAYBACK' text to compute an appropriate font size.
        // The font should occupy the whole banner viewport with some padding.
        if (!isTextSizeComputed) {
            PlaybackText(
                color = Color.Transparent,
                fontSize = fontSize,
                onTextLayout = { result ->
                    when {
                        isTextSizeComputed -> Unit
                        else -> {
                            val textSize = if (shouldTargetHeight) result.size.height else result.size.width
                            val ratio = targetSize / textSize
                            if (ratio !in 0.95..1.01) {
                                fontSize *= ratio
                            } else {
                                padding = density.run {
                                    (result.size.height - result.firstBaseline).toDp() / 1.8f
                                }.coerceAtLeast(0.dp)
                                isTextSizeComputed = true
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun PlaybackText(
    color: Color,
    fontSize: TextUnit,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    Text(
        text = "PLAYBACK",
        color = color,
        fontSize = fontSize,
        fontFamily = humaneFontFamily,
        onTextLayout = onTextLayout,
    )
}
