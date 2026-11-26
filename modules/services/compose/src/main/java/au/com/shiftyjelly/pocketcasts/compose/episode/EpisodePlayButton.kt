package au.com.shiftyjelly.pocketcasts.compose.episode

import androidx.annotation.DrawableRes
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun EpisodePlayButton(
    @DrawableRes iconId: Int,
    contentDescription: String,
    progress: Float?,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 52.dp, height = 48.dp)
            .combinedClickable(
                interactionSource = null,
                indication = ripple(bounded = false),
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { this.contentDescription = contentDescription },
    ) {
        if (progress != null) {
            CircularProgressIndicator(
                progress = progress,
                color = tint,
                backgroundColor = tint.copy(alpha = 0.3f),
                strokeWidth = 2.dp,
                modifier = Modifier.size(32.dp),
            )
        }
        Icon(
            painter = painterResource(iconId),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(32.dp),
        )
    }
}
