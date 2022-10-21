package au.com.shiftyjelly.pocketcasts.taskerplugin.base

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class TaskerInputFieldState<T>(val content: Content<T>) {
    data class Content<T> constructor(
        val value: String?,
        @StringRes val labelResId: Int,
        val onTextChange: (String) -> Unit,
        val taskerVariables: Array<String>,
        val possibleItems: List<T>? = null,
        val itemToString: (T?) -> String = { it?.toString() ?: "" },
        val itemContent: @Composable (T) -> Unit = { Text(text = itemToString(it)) }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> ComposableTaskerInputField(content: TaskerInputFieldState.Content<T>) {
    var isSearching by remember { mutableStateOf(false) }
    var isSelectingTaskerVariable by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    Box {

        Row {
            val possibleItems = content.possibleItems

            val hasSuggestedItems = !possibleItems.isNullOrEmpty()
            val hasTaskerVariables = content.taskerVariables.isNotEmpty()
            OutlinedTextField(
                value = content.value ?: "", label = { Text(text = stringResource(id = content.labelResId)) }, onValueChange = {
                    content.onTextChange(it)
                }, modifier = Modifier.weight(1f),
                trailingIcon = if (!hasSuggestedItems && !hasTaskerVariables) null else {
                    {
                        Row {
                            if (hasTaskerVariables) {
                                IconButton(onClick = { isSelectingTaskerVariable = true }) {
                                    Icon(
                                        painter = painterResource(au.com.shiftyjelly.pocketcasts.images.R.drawable.ic_filters),
                                        contentDescription = stringResource(R.string.tasker_variables),
                                        tint = MaterialTheme.theme.colors.primaryIcon01,
                                        modifier = Modifier.padding(end = 16.dp, start = 16.dp)
                                    )
                                }
                            }
                            if (hasSuggestedItems) {
                                IconButton(onClick = { isSearching = true }) {
                                    Icon(
                                        painter = painterResource(au.com.shiftyjelly.pocketcasts.images.R.drawable.ic_search),
                                        contentDescription = stringResource(R.string.search),
                                        tint = MaterialTheme.theme.colors.primaryIcon01,
                                        modifier = Modifier.padding(end = 16.dp, start = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            )
            if (possibleItems != null && !possibleItems.isEmpty()) {
                DropdownMenu(
                    expanded = isSearching,
                    onDismissRequest = { },
                    properties = PopupProperties(focusable = false)
                ) {
                    possibleItems.forEach {
                        DropdownMenuItem(onClick = {
                            isSearching = false
                            keyboardController?.hide()
                            content.onTextChange(content.itemToString(it))
                        }) {
                            content.itemContent(it)
                        }
                    }
                }
            }
            if (hasTaskerVariables) {
                DropdownMenu(
                    expanded = isSelectingTaskerVariable,
                    onDismissRequest = { },
                    properties = PopupProperties(focusable = false)
                ) {
                    content.taskerVariables.forEach {
                        DropdownMenuItem(onClick = {
                            isSelectingTaskerVariable = false
                            keyboardController?.hide()
                            content.onTextChange(it)
                        }) {
                            Text(it)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComposableTaskerInputFieldPreview() {
    AppTheme(Theme.ThemeType.CLASSIC_LIGHT) {
        ComposableTaskerInputField(
            TaskerInputFieldState.Content(
                "some value", R.string.archive, {},
                arrayOf("%test"),
                listOf("Hi", "Hello")
            )
        )
    }
}

@Composable
fun ComposableTaskerInputFieldList(
    fieldContents: List<TaskerInputFieldState.Content<*>>,
    onFinish: () -> Unit
) {
    Box(modifier = Modifier.fillMaxHeight()) {
        LazyColumn {
            fieldContents.forEach { content ->
                item {
                    ComposableTaskerInputField(content)
                }
            }
        }
        Button(onClick = onFinish, modifier = Modifier.align(Alignment.BottomEnd)) {
            Text(stringResource(R.string.ok))
        }
    }
}
