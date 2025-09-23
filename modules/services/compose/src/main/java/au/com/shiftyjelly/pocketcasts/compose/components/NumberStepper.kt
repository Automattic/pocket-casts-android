package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun NumberStepper(
    onMinusClick: () -> Unit,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier,
    minusContentDescription: Int = LR.string.number_stepper_minus_content_description,
    plusContentDescription: Int = LR.string.number_stepper_plus_content_description,
    tint: Color = MaterialTheme.theme.colors.playerContrast01,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onMinusClick,
        ) {
            Icon(
                painter = painterResource(id = IR.drawable.ic_minus),
                contentDescription = stringResource(minusContentDescription),
                tint = tint,
            )
        }

        if (content != null) {
            content()
        }

        IconButton(
            onClick = onPlusClick,
        ) {
            Icon(
                painter = painterResource(id = IR.drawable.ic_effects_plus),
                contentDescription = stringResource(plusContentDescription),
                tint = tint,
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
private fun NumberStepperPreview() {
    NumberStepper(
        onMinusClick = { },
        onPlusClick = { },
    )
}
