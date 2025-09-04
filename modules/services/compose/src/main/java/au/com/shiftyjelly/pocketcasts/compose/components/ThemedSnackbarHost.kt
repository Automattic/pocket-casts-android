package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.compose.theme

@Composable
fun ThemedSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData, SnackbarTheme) -> Unit = { data, theme ->
        Snackbar(
            snackbarData = data,
            backgroundColor = theme.surfaceColor,
            contentColor = theme.textColor,
        )
    },
) {
    val theme = rememberSnackbarTheme()
    SnackbarHost(
        hostState = hostState,
        snackbar = { data -> snackbar(data, theme) },
        modifier = modifier,
    )
}

data class SnackbarTheme(
    val surfaceColor: Color,
    val textColor: Color,
)

@Composable
private fun rememberSnackbarTheme(): SnackbarTheme {
    val isDarkTheme = MaterialTheme.theme.isDark
    return remember(isDarkTheme) {
        if (isDarkTheme) {
            SnackbarTheme(
                surfaceColor = Color.White,
                textColor = Color(0xFF292B2E),
            )
        } else {
            SnackbarTheme(
                surfaceColor = Color(0xFF292B2E),
                textColor = Color.White,
            )
        }
    }
}
