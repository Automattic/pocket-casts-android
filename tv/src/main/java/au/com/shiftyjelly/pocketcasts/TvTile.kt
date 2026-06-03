package au.com.shiftyjelly.pocketcasts

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardGlow
import androidx.tv.material3.CardScale
import androidx.tv.material3.CardShape
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun TvTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    scale: CardScale = CardDefaults.scale(focusedScale = 1.1f),
    shape: CardShape = CardDefaults.shape(),
    colors: CardColors = CardDefaults.colors(
        containerColor = TvColors.DarkGray,
        focusedContainerColor = TvColors.Gray,
    ),
    border: CardBorder = CardDefaults.border(),
    glow: CardGlow = CardDefaults.glow(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        scale = scale,
        shape = shape,
        colors = colors,
        border = border,
        glow = glow,
        interactionSource = interactionSource,
        content = content,
    )
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun TvTilePreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvTile(onClick = {}) {
                Box(
                    modifier = Modifier
                        .size(160.dp, 100.dp)
                        .background(TvColors.DarkGray)
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Text(
                        text = "Sample Tile",
                        color = Color.White,
                    )
                }
            }
        }
    }
}
