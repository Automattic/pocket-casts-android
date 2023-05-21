package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme

@Composable
fun ErrorScreen(
    text: String,
    icon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.Error,
            tint = Color.Red,
            contentDescription = null,
            modifier = Modifier.size(52.dp)
        )
    },
) {

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        icon()
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.title3,
            color = Color.White,
            // Adding bottom padding to make sure that long text can be scrolled up from the
            // bottom of the screen (where it gets cut off on round watches)
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

@Preview
@Composable
private fun NotificationScreenPreview() {
    WearAppTheme(Theme.ThemeType.DARK) {
        ErrorScreen("There was a problem")
    }
}
