package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.RippleDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.player.R

@Composable
internal fun ChapterLinkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(
            Color.White,
            RippleDefaults.rippleAlpha(Color.White, lightTheme = true),
        ),
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier.clip(CircleShape),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_link_back),
                contentDescription = stringResource(id = au.com.shiftyjelly.pocketcasts.localization.R.string.player_chapter_url),
                modifier = Modifier.sizeIn(36.dp),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_link),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
