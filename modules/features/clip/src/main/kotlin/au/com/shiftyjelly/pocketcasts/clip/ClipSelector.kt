package au.com.shiftyjelly.pocketcasts.clip

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ClipSelector(
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0x476B6B6B), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
    ) {
        Image(
            painter = painterResource(if (isPlaying) IR.drawable.ic_widget_pause else IR.drawable.ic_widget_play),
            contentDescription = stringResource(if (isPlaying) LR.string.pause else LR.string.play),
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier
                .size(72.dp)
                .clickable {
                    if (isPlaying) onPauseClick() else onPlayClick()
                }
                .padding(16.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxSize()
                .background(Color.Magenta),
        ) {
            TextH30(
                text = "SELECTOR PLACEHOLDER",
                color = Color.White,
            )
        }
    }
}

@ShowkaseComposable(name = "ClipSelector", group = "Clip", styleName = "Light")
@Preview(name = "ClipSelectorLight", showBackground = true, backgroundColor = 0xFF3E6266)
@Composable
fun ClipSelectorLightPreview() = ClipSelectorPreview()

@ShowkaseComposable(name = "ClipSelector", group = "Clip", styleName = "Dark")
@Preview(name = "ClipSelectorDark", showBackground = true, backgroundColor = 0xFF0E1A17)
@Composable
fun ClipSelectorDarkPreview() = ClipSelectorPreview()

@Composable
private fun ClipSelectorPreview() = Column {
    ClipSelector(
        isPlaying = false,
        onPlayClick = {},
        onPauseClick = {},
    )
    Spacer(modifier = Modifier.height(32.dp))
    ClipSelector(
        isPlaying = true,
        onPlayClick = {},
        onPauseClick = {},
    )
}
