package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.wear.theme.WearColors

@Composable
fun ChipScreenHeader(@StringRes text: Int, modifier: Modifier = Modifier) {
    Header(
        text,
        modifier.padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp,
        )
    )
}

@Composable
fun ChipSectionHeader(@StringRes text: Int, modifier: Modifier = Modifier) {
    Header(text, modifier.padding(16.dp))
}

@Composable
private fun Header(@StringRes text: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(text),
        color = WearColors.FFBDC1C6,
        style = MaterialTheme.typography.button,
        modifier = modifier
    )
}
