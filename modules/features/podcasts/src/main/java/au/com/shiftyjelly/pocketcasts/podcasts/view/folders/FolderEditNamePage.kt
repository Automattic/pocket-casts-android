package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.BottomSheetAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.podcasts.view.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun FolderEditNamePage(
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    viewModel: FolderEditViewModel
) {
    val folderName: String by viewModel.folderName.collectAsState()
    val focusRequester = remember { FocusRequester() }
    Column {
        BottomSheetAppBar(
            title = stringResource(LR.string.name_your_folder),
            navigationButton = NavigationButton.Back,
            onNavigationClick = { onBackClick() }
        )
        TextC70(
            text = stringResource(LR.string.name),
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 8.dp)
        )
        FormField(
            value = folderName,
            placeholder = stringResource(LR.string.folder_name),
            onValueChange = { viewModel.changeFolderName(it) },
            onNext = { onNextClick() },
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .focusRequester(focusRequester)
        )
        RowButton(
            text = stringResource(LR.string.navigation_continue),
            onClick = { onNextClick() }
        )
        // so the bottom sheet goes full height
        Spacer(modifier = Modifier.weight(1f))
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
