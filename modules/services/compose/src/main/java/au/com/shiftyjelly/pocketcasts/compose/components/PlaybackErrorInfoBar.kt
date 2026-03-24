package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

/**
 * Fullscreen player variant: centered text, uses player colors.
 */
@Composable
fun PlaybackErrorInfoBar(
    message: String,
    playerColors: PlayerColors,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        color = playerColors.contrast01,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(playerColors.contrast06)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/**
 * Miniplayer variant: centered text with trailing chevron.
 */
@Composable
fun PlaybackErrorInfoBar(
    message: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.secondaryUi01)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = message,
            color = colors.primaryText01,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Image(
            painter = painterResource(IR.drawable.ic_chevron_right),
            colorFilter = ColorFilter.tint(colors.primaryIcon02),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}
