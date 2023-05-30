package au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ControlButtonLayoutStyled(
    leftButton: @Composable () -> Unit,
    middleButton: @Composable () -> Unit,
    rightButton: @Composable () -> Unit,
    sidePadding: Dp = 17.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Absolute.Center
    ) {
        Box(modifier = Modifier.padding(start = sidePadding)) {
            leftButton()
        }

        Spacer(modifier = Modifier.weight(1f))

        middleButton()

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier.padding(end = sidePadding)) {
            rightButton()
        }
    }
}
