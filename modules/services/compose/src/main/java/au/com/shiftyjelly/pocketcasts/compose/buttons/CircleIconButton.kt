package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun CircleIconButton(
    size: Dp,
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    iconColor: Color = Color.White,
    onClick: () -> Unit
) {
    CircleButton(
        size = size,
        onClick = onClick,
        backgroundColor = backgroundColor,
        contentColor = iconColor,
        modifier = modifier
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
fun CircleIconButton(
    size: Dp,
    icon: Painter,
    modifier: Modifier = Modifier,
    iconSize: Dp? = null,
    iconVisible: Boolean = true,
    contentDescription: String,
    backgroundColor: Color,
    iconColor: Color = Color.White,
    onClick: () -> Unit
) {
    CircleButton(
        size = size,
        onClick = onClick,
        backgroundColor = backgroundColor,
        contentColor = iconColor,
        modifier = modifier
    ) {
        if (!iconVisible) {
            return@CircleButton
        }
        var iconModifier: Modifier = Modifier
        if (iconSize != null) {
            iconModifier = Modifier.size(iconSize)
        }
        Icon(icon, contentDescription = contentDescription, modifier = iconModifier)
    }
}

@Preview(showBackground = true)
@Composable
fun CircleIconButtonPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        CircleIconButton(
            size = 50.dp,
            icon = Icons.Default.Add,
            contentDescription = "Add",
            backgroundColor = MaterialTheme.colors.primary,
            onClick = {}
        )
    }
}
