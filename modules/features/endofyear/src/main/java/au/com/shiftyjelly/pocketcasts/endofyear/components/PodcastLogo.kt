package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import au.com.shiftyjelly.pocketcasts.endofyear.R

@Composable
fun PodcastLogoWhite(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_white),
        contentDescription = null,
        modifier = modifier,
    )
}
