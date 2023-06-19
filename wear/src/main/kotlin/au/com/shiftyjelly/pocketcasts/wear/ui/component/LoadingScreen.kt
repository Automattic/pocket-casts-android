package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            indicatorColor = MaterialTheme.colors.onPrimary,
            trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
            strokeWidth = 3.dp
        )
    }
}
