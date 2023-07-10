package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.CircleIconButton
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.accompanist.flowlayout.FlowRow
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun FolderColorPicker(selectedId: Int, onClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier.padding(start = 14.dp, top = 16.dp, end = 2.dp)) {
        MaterialTheme.theme.colors.folderColors.forEach { (id, color) ->
            Box(
                modifier = Modifier
                    .padding(bottom = 10.dp, end = 10.dp)
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                ColorSelectCircle(
                    color = color,
                    selected = (id == selectedId),
                    onClick = { onClick(id) }
                )
            }
        }
    }
}

@Composable
private fun ColorSelectCircle(color: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val size by animateDpAsState(if (selected) 48.dp else 40.dp)
    CircleIconButton(
        size = size,
        iconSize = 28.dp,
        iconVisible = selected,
        icon = painterResource(IR.drawable.ic_tick),
        backgroundColor = color,
        onClick = onClick,
        contentDescription = "",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun ColorPickerLightPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        FolderColorPicker(selectedId = 0, onClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ColorPickerDarkPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        FolderColorPicker(selectedId = 0, onClick = {})
    }
}
