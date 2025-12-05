package au.com.shiftyjelly.pocketcasts.compose.images

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun ProfileImage(
    avatarUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit) = {},
) {
    val avatarPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(avatarUrl)
            .crossfade(true)
            .build(),
    )
    val state by avatarPainter.state.collectAsState()
    val isShowingPlaceholder = state !is AsyncImagePainter.State.Success

    Crossfade(
        isShowingPlaceholder,
        animationSpec = tween(500),
    ) { showPlaceholder ->
        Image(
            painter = avatarPainter,
            contentDescription = contentDescription,
            modifier = modifier
                .alpha(if (showPlaceholder) 0f else 1f),
        )

        // If the gravatar image has not loaded or fails to load (because there is no gravatar image associated
        // with this account), show the placeholder. We are settings the placeholder this way instead of
        // setting a placeholder on an AsyncImagePainter because this approach continues showing the placeholder
        // when there is not a gravatar image associated with the account.
        if (showPlaceholder) {
            placeholder()
        }
    }
}
