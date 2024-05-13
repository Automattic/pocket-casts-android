package au.com.shiftyjelly.pocketcasts.compose.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R

@Composable
fun <T> RadioOptionsDialog(
    title: String,
    selectedOption: T,
    allOptions: List<T>,
    modifier: Modifier = Modifier,
    optionName: @Composable (T) -> String,
    onSelectOption: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    var selection by remember { mutableStateOf(selectedOption) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                TextH30(
                    text = title,
                    modifier = Modifier.padding(16.dp),
                )
                allOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selection = option
                                onSelectOption(option)
                            },
                    ) {
                        Spacer(
                            modifier = Modifier.width(8.dp),
                        )
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = {
                                selection = option
                                onSelectOption(option)
                            },
                        )
                        TextH40(
                            text = optionName(option),
                        )
                        Spacer(
                            modifier = Modifier.width(8.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        TextH40(
                            text = stringResource(R.string.cancel).uppercase(),
                            color = MaterialTheme.theme.colors.primaryInteractive01,
                        )
                    }
                }
            }
        }
    }
}
