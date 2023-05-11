package au.com.shiftyjelly.pocketcasts.compose.images

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import au.com.shiftyjelly.pocketcasts.utils.Gravatar

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

    ProfileImage(
        avatarUrl = gravatarUrl,
        contentDescription = contentDescription,
        placeholder = placeholder,
        modifier = modifier,
    )
}
