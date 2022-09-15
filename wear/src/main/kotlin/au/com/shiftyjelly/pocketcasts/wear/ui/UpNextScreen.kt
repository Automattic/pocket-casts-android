package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object UpNextScreen {
    const val route = "up_next_screen"
}

@Composable
fun UpNextScreen() {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(LR.string.up_next)
    )
}
