package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearColors
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun NotificationScreen(
    text: String,
    delayDuration: Duration? = 2.seconds,
    onClose: () -> Unit,
) {

    LaunchedEffect(delayDuration) {
        if (delayDuration != null) {
            delay(delayDuration)
            onClose()
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClose() }
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_check_black_24dp),
            tint = WearColors.FFA1E7B0,
            contentDescription = null,
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextH30(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun NotificationScreenPreview() {
    WearAppTheme(Theme.ThemeType.DARK) {
        NotificationScreen(
            text = "Done",
            onClose = {}
        )
    }
}
