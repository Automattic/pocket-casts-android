package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun ScreenHeaderChip(
    @StringRes text: Int,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
) {
    ScreenHeaderChip(
        text = stringResource(text),
        textColor = textColor,
        modifier = modifier,
    )
}

@Composable
fun ScreenHeaderChip(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
) {
    Header(
        text = text,
        textColor = textColor,
        modifier = modifier.padding(
            start = horizontalPadding,
            end = horizontalPadding,
            bottom = verticalPadding,
        )
    )
}

@Composable
fun SectionHeaderChip(
    @StringRes text: Int,
    modifier: Modifier = Modifier
) {
    Header(
        text = stringResource(text),
        modifier = modifier.padding(vertical = verticalPadding, horizontal = horizontalPadding)
    )
}

@Composable
private fun Header(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
) {
    Text(
        text = text,
        color = textColor ?: MaterialTheme.colors.onSecondary,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.button,
        modifier = modifier.fillMaxWidth()
    )
}

private val horizontalPadding = 10.dp
private val verticalPadding = 14.dp
