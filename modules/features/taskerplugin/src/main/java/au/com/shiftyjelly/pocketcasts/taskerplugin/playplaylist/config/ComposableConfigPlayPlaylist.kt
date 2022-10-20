package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.config.ViewModelConfigPlayPlaylist

@Composable
fun ComposableConfigPlayPlaylist(viewModel: ViewModelConfigPlayPlaylist, onFinish: () -> Unit) {
    val text = viewModel.titleState.collectAsState().value ?: ""
    var isSearching by remember { mutableStateOf(false) }

    if (!isSearching) {
        Box(modifier = Modifier.fillMaxHeight(1f)) {
            Column {
                Row {
                    TextField(value = text, label = { Text(text = stringResource(id = au.com.shiftyjelly.pocketcasts.localization.R.string.filters_filter_name)) }, onValueChange = {
                        viewModel.title = it
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
            Button(onClick = onFinish, modifier = Modifier.align(Alignment.BottomEnd)) {
                Text(stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.ok))
            }
        }
    } else {
        val playlists = viewModel.playlists.subscribeAsState(listOf()).value
        LazyColumn {
            playlists.forEach {
                item {
                    Text(
                        it.title,
                        modifier = Modifier
                            .clickable {
                                viewModel.title = it.title
                                isSearching = false
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
