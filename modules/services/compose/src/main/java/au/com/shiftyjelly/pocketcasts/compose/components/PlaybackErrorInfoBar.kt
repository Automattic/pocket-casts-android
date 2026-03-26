package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors

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
