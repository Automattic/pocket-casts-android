package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun CircleButton(size: Dp, onClick: () -> Unit, backgroundColor: Color, modifier: Modifier = Modifier, contentColor: Color = Color.White, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = backgroundColor, contentColor = contentColor)
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun CircleButtonPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        CircleButton(
            size = 48.dp,
            onClick = {},
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary
        ) {}
    }
}
