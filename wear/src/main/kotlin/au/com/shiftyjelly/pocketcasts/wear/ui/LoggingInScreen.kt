package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.images.GravatarProfileImage
import au.com.shiftyjelly.pocketcasts.compose.images.ProfileImage
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object LoggingInScreen {
    const val route = "loggingInScreen"
}

@Composable
fun LoggingInScreen(
    avatarUrl: String? = null,
    name: String? = null,
    onClose: () -> Unit,
) {

    val viewModel = hiltViewModel<LoggingInScreenViewModel>()
    val state = viewModel.state.collectAsState().value

    when (state) {

        LoggingInScreenViewModel.State.RefreshComplete -> {
            onClose()
        }

        is LoggingInScreenViewModel.State.Refreshing -> {

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onClose() }
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                val placeholder = @Composable {
                    SpinningIcon(Modifier.size(48.dp))
                }

                val profileModifier = Modifier
                    .clip(CircleShape)
                    .size(48.dp)

                if (avatarUrl != null) {
                    ProfileImage(
                        avatarUrl = avatarUrl,
                        contentDescription = null,
                        placeholder = placeholder,
                        modifier = profileModifier,
                    )
                } else if (state.email != null) {
                    // If there's no avatar, but we have an email, try to use Gravatar
                    GravatarProfileImage(
                        email = state.email,
                        contentDescription = null,
                        placeholder = placeholder,
                        modifier = profileModifier,
                    )
                } else {
                    placeholder()
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (name != null) {
                    Text(stringResource(LR.string.profile_hi, name))
                } else {
                    Text(
                        text = stringResource(LR.string.profile_logging_in),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.title3
                    )
                }

                AnimatedVisibility(visible = state.email != null) {
                    BoxWithConstraints(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {

                        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val background = MaterialTheme.colors.background

                        if (state.email != null) {
                            Text(
                                text = state.email,
                                style = MaterialTheme.typography.body2,
                                // Turn off softWrap to make sure the text doesn't get truncated if it runs long.
                                // Without this if "xxxx@gmail.com" ran just a bit long, it would get shortened
                                // to "xxx@gmail".
                                softWrap = false,
                                modifier = Modifier.fadeOutOverflow(
                                    overFlowWidthPx = widthPx.toInt(),
                                    backgroundColor = background
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.fadeOutOverflow(
    overFlowWidthPx: Int,
    backgroundColor: Color,
) = drawWithContent {

    // draw text first
    drawContent()

    // Use < instead of <= because text that doesn't fit will have a width that is equal
    // to the available space
    val textFits = size.width < overFlowWidthPx

    if (!textFits) {
        // Apply a gradient over the end of the text that fades in the background color to indicate overflow
        drawRect(
            // Add a bit of extra width so the gradient goes to the end of the screen
            size = Size(size.width * 1.2f, size.height),
            brush = Brush.horizontalGradient(
                0.7f to Color.Transparent,
                1.0f to backgroundColor,
            ),
        )
    }
}

@Composable
private fun SpinningIcon(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Icon(
        painter = painterResource(IR.drawable.ic_retry),
        contentDescription = null,
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
            }
    )
}

@Preview
@Composable
private fun LoggingInScreenPreview() {
    WearAppTheme(Theme.ThemeType.DARK) {
        LoggingInScreen(
            onClose = {}
        )
    }
}
