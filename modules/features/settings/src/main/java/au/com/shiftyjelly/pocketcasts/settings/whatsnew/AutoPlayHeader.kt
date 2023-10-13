package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.extensions.brush
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AutoPlayHeader() {
    val gradientBrush = Brush.linearGradient(
        0f to MaterialTheme.theme.colors.primaryInteractive01,
        1f to MaterialTheme.theme.colors.primaryInteractive01Hover,
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(gradientBrush)
            .fillMaxWidth(),
    ) {

        var iconIsVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            iconIsVisible = true
        }

        // Using the same spring animation for the scaleIn and rotation ensures that they
        // finish at the same time.
        val springAnimation = spring<Float>(
            stiffness = Spring.StiffnessVeryLow,
            dampingRatio = Spring.DampingRatioLowBouncy,
        )

        AnimatedVisibility(
            visible = iconIsVisible,
            enter = scaleIn(animationSpec = springAnimation)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(all = 43.dp)
                    .clip(shape = CircleShape)
                    .background(Color.White)
                    .size(95.dp),

            ) {

                var rotating by remember { mutableStateOf(false) }
                val rotation by animateFloatAsState(
                    targetValue = if (rotating) 2 * 360f else 0f,
                    animationSpec = springAnimation
                )
                LaunchedEffect(Unit) {
                    rotating = true
                }

                Icon(
                    imageVector = ImageVector.vectorResource(IR.drawable.whatsnew_autoplay),
                    contentDescription = null,
                    modifier = Modifier
                        .height(24.dp)
                        .rotate(rotation)
                        .brush(gradientBrush),
                )
            }
        }
    }
}
