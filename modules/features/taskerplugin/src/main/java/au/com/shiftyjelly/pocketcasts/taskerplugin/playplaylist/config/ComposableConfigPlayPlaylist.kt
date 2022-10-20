package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class ConfigPlayPlaylistState(val content: Content) {
    data class Content(
        val filterName: String,
        val possibleFilterNames: List<String>,
        val onFilterNameChanged: (String) -> Unit,
        val onFinish: () -> Unit,
    )
}

@Composable
fun ComposableConfigPlayPlaylist(content: ConfigPlayPlaylistState.Content) {
    var isSearching by remember { mutableStateOf(false) }

    if (!isSearching) {
        Box(modifier = Modifier.wrapContentHeight(unbounded = true)) {
            Column {
                Row {
                    TextField(value = content.filterName, label = { Text(text = stringResource(id = au.com.shiftyjelly.pocketcasts.localization.R.string.filters_filter_name)) }, onValueChange = {
                        content.onFilterNameChanged(it)
                    }, modifier = Modifier.weight(1f), trailingIcon = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.search),
                                tint = MaterialTheme.theme.colors.primaryIcon01,
                                modifier = Modifier.padding(end = 16.dp, start = 16.dp)
                            )
                        }
                    })
                }
            }
            Button(onClick = content.onFinish, modifier = Modifier.align(Alignment.BottomEnd)) {
                Text(stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.ok))
            }
        }
    } else {
        LazyColumn {
            content.possibleFilterNames.forEach {
                item {
                    Text(
                        it,
                        modifier = Modifier
                            .clickable {
                                content.onFilterNameChanged(it)
                                isSearching = false
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ComposableConfigPlayPlaylistPreview() {
    AppTheme(Theme.ThemeType.CLASSIC_LIGHT) {
        ComposableConfigPlayPlaylist(
            ConfigPlayPlaylistState.Content("New Release", listOf("New Releases", "Up Next"), {}, {})
        )
    }
}