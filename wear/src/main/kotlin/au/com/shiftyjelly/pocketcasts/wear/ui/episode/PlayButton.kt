package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme

@Composable
fun PlayButton(
    isPlaying: Boolean,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color = backgroundColor)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(
                if (isPlaying) {
                    R.drawable.button_pause
                } else {
                    R.drawable.button_play
                }
            ),
            contentDescription = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.play),
            modifier = Modifier.size(52.dp)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    WearAppTheme {
        Column {
            PlayButton(
                isPlaying = true,
                backgroundColor = Color.Yellow,
                onClick = {}
            )
            PlayButton(
                isPlaying = false,
                backgroundColor = Color.Blue,
                onClick = {}
            )
        }
    }
}
