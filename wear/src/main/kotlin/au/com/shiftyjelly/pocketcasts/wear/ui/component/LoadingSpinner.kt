package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme

@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
) {
    CircularProgressIndicator(
        indicatorColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.9f),
        trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
        strokeWidth = 5.dp,
        modifier = modifier
    )
}
