package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowCloseButton
import au.com.shiftyjelly.pocketcasts.compose.extensions.brush
import au.com.shiftyjelly.pocketcasts.compose.theme
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun NewWidgetsHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    fullModal: Boolean = true,
) {
    var animationState by remember { mutableStateOf(AnimationState.SmallWidget) }
    var rotationCounter by remember { mutableIntStateOf(0) }
    val targetRotation by animateFloatAsState(
        targetValue = rotationCounter * 180f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 300f,
        ),
        label = "rotation",
    )
    val tintColor = if (MaterialTheme.theme.isLight) Color.Black else Color.White

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .then(if (fullModal) Modifier else Modifier.padding(top = 80.dp)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    .phoneFrame(
                        color = tintColor.copy(alpha = 0.3f),
                        strokeWidth = 8.dp,
                        cornerRadius = 16.dp,
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = "9:30",
                        color = tintColor,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.width(40.dp),
                    )
                    Image(
                        painter = painterResource(id = IR.drawable.ic_circle),
                        colorFilter = ColorFilter.tint(tintColor.copy(alpha = 0.8f)),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Image(
                        painter = painterResource(id = IR.drawable.ic_phone_idicator_icons),
                        colorFilter = ColorFilter.tint(tintColor),
                        contentDescription = null,
                        modifier = Modifier.width(40.dp).height(16.dp),
                    )
                }
                Spacer(modifier = Modifier.height(60.dp))
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer { rotationZ = targetRotation }
                            .background(Color(0xFFE9F0F5), RoundedCornerShape(16.dp))
                            .animateContentSize(spring(dampingRatio = 0.65f, stiffness = 300f))
                            .then(animationState.widgetSize),
                    )
                }
            }
        }

        if (!fullModal) {
            RowCloseButton(
                onClose = onClose,
                tintColor = tintColor,
            )
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1500.milliseconds)
            animationState = animationState.nextState
            rotationCounter = animationState.newRotationCounter(rotationCounter)
        }
    }
}

private fun Modifier.phoneFrame(
    color: Color,
    strokeWidth: Dp,
    cornerRadius: Dp,
) = then(
    composed(
        factory = {
            val density = LocalDensity.current
            val strokeWidthPx = density.run { strokeWidth.toPx() }
            val cornerRadiusPx = density.run { cornerRadius.toPx() }
            Modifier.drawBehind {
                drawLine(
                    brush = Brush.verticalGradient(
                        0f to color,
                        0.8f to color,
                        1f to color.copy(alpha = 0f),
                    ),
                    start = Offset(x = 0f, y = size.height),
                    end = Offset(x = 0f, y = cornerRadiusPx),
                    strokeWidth = strokeWidthPx,
                )
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset.Zero,
                    size = Size(cornerRadiusPx * 2, cornerRadiusPx * 2),
                    style = Stroke(strokeWidthPx),
                )
                drawLine(
                    color = color,
                    start = Offset(x = cornerRadiusPx, y = 0f),
                    end = Offset(x = size.width - cornerRadiusPx, y = 0f),
                    strokeWidth = strokeWidthPx,
                )
                drawArc(
                    color = color,
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(x = size.width - cornerRadiusPx * 2, y = 0f),
                    size = Size(cornerRadiusPx * 2, cornerRadiusPx * 2),
                    style = Stroke(width = strokeWidthPx),
                )
                drawLine(
                    brush = Brush.verticalGradient(
                        0f to color,
                        0.8f to color,
                        1f to color.copy(alpha = 0f),
                    ),
                    start = Offset(x = size.width, y = size.height),
                    end = Offset(x = size.width, y = cornerRadiusPx),
                    strokeWidth = strokeWidthPx,
                )
            }
        },
    ),
)

private enum class AnimationState(
    val widgetSize: Modifier,
) {
    SmallWidget(
        widgetSize = Modifier.size(100.dp),
    ) {
        override val nextState get() = MediumWidget
    },
    MediumWidget(
        widgetSize = Modifier
            .fillMaxWidth(0.9f)
            .height(100.dp),
    ) {
        override val nextState get() = LargeWidget1
    },
    LargeWidget1(
        widgetSize = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.95f),
    ) {
        override val nextState get() = LargeWidget2
    },
    LargeWidget2(
        widgetSize = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.95f),
    ) {
        override val nextState get() = SmallWidget
    },
    ;

    abstract val nextState: AnimationState

    fun newRotationCounter(counter: Int) = when (this) {
        SmallWidget -> counter - 1
        MediumWidget, LargeWidget1 -> counter
        LargeWidget2 -> counter + 1
    }
}
