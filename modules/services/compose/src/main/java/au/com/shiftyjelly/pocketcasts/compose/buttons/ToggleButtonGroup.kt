package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.theme

data class ToggleButtonOption(
    val imageId: Int,
    val descriptionId: Int,
    val click: () -> Unit,
    val isOn: () -> Boolean
)

@Composable
fun ToggleButtonGroup(options: List<ToggleButtonOption>?) {
    if (options.isNullOrEmpty()) {
        return
    }
    var selectedImageId by remember { mutableStateOf(options.first { it.isOn() }.imageId) }
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .border(
                width = 2.dp,
                color = MaterialTheme.theme.colors.primaryInteractive01,
                shape = RoundedCornerShape(size = 8.dp)
            )
            .clip(RoundedCornerShape(size = 8.dp))
    ) {
        options.forEachIndexed { index, option ->
            val backgroundColor = if (option.isOn()) MaterialTheme.theme.colors.primaryInteractive01 else Color.Transparent
            val isSelected = selectedImageId == option.imageId
            IconToggleSquareButton(
                checked = isSelected,
                rippleColor = MaterialTheme.theme.colors.primaryInteractive01,
                onCheckedChange = {
                    option.click()
                    selectedImageId = option.imageId
                },
                modifier = Modifier
                    .border(width = 0.dp, color = Color.Transparent, shape = RoundedCornerShape(size = 8.dp))
                    .background(color = backgroundColor)
            ) {
                val color = if (isSelected) MaterialTheme.theme.colors.primaryInteractive02 else MaterialTheme.theme.colors.primaryInteractive01
                Icon(
                    painter = painterResource(id = option.imageId),
                    contentDescription = stringResource(option.descriptionId),
                    tint = color
                )
                // button divider
                if (index <= options.size - 1) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.fillMaxHeight().fillMaxWidth()
                    ) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(color = MaterialTheme.theme.colors.primaryInteractive01)
                        )
                    }
                }
            }
        }
    }
}

private val RippleRadius = 48.dp
private val IconButtonSizeModifier = Modifier.size(48.dp)

// Copied from androidx.compose.material.IconButton so we can make the ripple go the edge of the button and set the ripple colour
@Composable
private fun IconToggleSquareButton(
    checked: Boolean,
    rippleColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            enabled = enabled,
            role = Role.Checkbox,
            interactionSource = interactionSource,
            indication = rememberRipple(bounded = true, radius = RippleRadius, color = rippleColor)
        ).then(IconButtonSizeModifier),
        contentAlignment = Alignment.Center
    ) {
        val contentAlpha = if (enabled) LocalContentAlpha.current else ContentAlpha.disabled
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha, content = content)
    }
}
