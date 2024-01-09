package au.com.shiftyjelly.pocketcasts.wear.ui.component

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
import androidx.wear.compose.material.MaterialTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearColors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun NotificationScreen(
    text: String,
    onClose: () -> Unit,
    closeAfterDuration: Duration? = 2.seconds,
    icon: @Composable () -> Unit = {
        Icon(
            painter = painterResource(IR.drawable.ic_check_black_24dp),
            tint = WearColors.success,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
        )
    },
) {
    LaunchedEffect(closeAfterDuration) {
        if (closeAfterDuration != null) {
            delay(closeAfterDuration)
            onClose()
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClose() }
            .padding(16.dp)
            .fillMaxSize(),
    ) {
        icon()
        Spacer(modifier = Modifier.height(4.dp))
        TextH30(
            text = text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onPrimary,
        )
    }
}

@Preview
@Composable
private fun NotificationScreenPreview() {
    WearAppTheme {
        NotificationScreen(
            text = "Done",
            onClose = {},
        )
    }
}
