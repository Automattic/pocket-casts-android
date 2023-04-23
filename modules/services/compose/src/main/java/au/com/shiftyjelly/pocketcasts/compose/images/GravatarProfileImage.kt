package au.com.shiftyjelly.pocketcasts.compose.images

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun GravatarProfileImage(
    email: String,
    modifier: Modifier = Modifier,
    contentDescription: String?,
    placeholder: @Composable (() -> Unit) = {},
) {

    val gravatarUrl = remember(email) {
        Gravatar.getUrl(email)
    }

    val gravatarPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(gravatarUrl)
            .crossfade(true)
            .build(),
    )

    Box {
        // Image component that attempts to load the gravatar image
        Image(
            painter = gravatarPainter,
            contentDescription = contentDescription,
            modifier = modifier,
        )

        // If the gravatar image has not loaded or fails to load (because there is no gravatar image associated
        // with this account), show the placeholder. We are doing this because setting a placeholder on an
        // AsyncImagePainter fails to show the placeholder when there is no gravatar image associated with
        // the account.
        if (gravatarPainter.state.painter == null) {
            placeholder()
        }
    }
}
