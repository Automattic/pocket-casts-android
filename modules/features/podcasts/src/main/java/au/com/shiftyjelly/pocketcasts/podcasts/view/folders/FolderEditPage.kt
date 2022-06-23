package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.BottomSheetAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.components.FolderColorPicker
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.view.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@Composable
fun FolderEditPage(
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    viewModel: FolderEditViewModel
) {
    val folderName: String by viewModel.folderName.collectAsState()
    val colorId: Int by viewModel.colorId.collectAsState()
    val focusManager = LocalFocusManager.current
    val resources = LocalContext.current.resources
    Column(modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi01)) {
        Column(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            BottomSheetAppBar(
                title = stringResource(LR.string.edit_folder),
                navigationButton = NavigationButton.Close,
                onNavigationClick = { onBackClick() }
            )
            TextC70(
                text = stringResource(LR.string.name),
                modifier = Modifier.padding(top = 16.dp, end = 16.dp, start = 16.dp)
            )
            FormField(
                value = folderName,
                placeholder = stringResource(LR.string.folder_name),
                onValueChange = {
                    viewModel.changeFolderName(it)
                    viewModel.saveFolderName(resources = resources)
                },
                onNext = { focusManager.clearFocus() },
                modifier = Modifier
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
            TextC70(
                text = stringResource(LR.string.color),
                modifier = Modifier.padding(top = 16.dp, end = 16.dp, start = 16.dp)
            )
            FolderColorPicker(
                selectedId = colorId,
                onClick = { colorId ->
                    viewModel.changeColor(colorId)
                    viewModel.saveColor()
                }
            )
            DeleteButton(
                onDeleteClick = onDeleteClick,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        // so the bottom sheet goes full height
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DeleteButton(onDeleteClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDeleteClick() }
    ) {
        Icon(
            painter = painterResource(VR.drawable.ic_delete),
            contentDescription = null,
            tint = MaterialTheme.theme.colors.support05,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Column(
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            TextP40(
                text = stringResource(LR.string.delete_folder),
                color = MaterialTheme.theme.colors.support05
            )
            TextP50(
                text = stringResource(LR.string.delete_folder_summary),
                color = MaterialTheme.theme.colors.primaryText02
            )
        }
    }
}
