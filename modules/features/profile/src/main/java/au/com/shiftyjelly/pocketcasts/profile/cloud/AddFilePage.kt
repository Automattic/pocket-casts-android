package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class AddFilePageState(
    @StringRes val titleRes: Int?,
    val name: String,
    val fileName: String,
    val fileSize: String,
    val fileTint: Int?,
    val isArtworkVisible: Boolean,
    val artwork: Bitmap?,
    @StringRes val imageButtonRes: Int,
    val isLoading: Boolean,
    val isReadOnly: Boolean,
    val isUpgradeVisible: Boolean,
    val imageItem: Bitmap?,
    val colours: List<AddFileColour>,
    val selectedIndex: Int,
)

@Composable
internal fun AddFilePage(
    state: AddFilePageState,
    onNavigationClick: () -> Unit,
    onSaveClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onImageClick: (() -> Unit)?,
    onImageButtonClick: () -> Unit,
    onColourClick: (Int) -> Unit,
    onLockedColourClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    onUpgradeCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    toolbarModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryUi01)
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)),
    ) {
        ThemedTopAppBar(
            title = state.titleRes?.let { stringResource(it) }.orEmpty(),
            navigationButton = NavigationButton.Back,
            onNavigationClick = onNavigationClick,
            iconColor = colors.secondaryIcon01,
            textColor = colors.secondaryText01,
            backgroundColor = colors.secondaryUi01,
            modifier = toolbarModifier,
            actions = {
                if (state.titleRes != null) {
                    SaveAction(onSaveClick)
                }
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            NameField(
                name = state.name,
                onNameChange = onNameChange,
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
            )
            Row(modifier = Modifier.padding(top = 16.dp)) {
                TextP50(
                    text = state.fileName,
                    color = colors.primaryText02,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp)
                        .weight(1f),
                )
                TextP50(
                    text = state.fileSize,
                    color = colors.primaryText02,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
            AddFileImage(
                fileTint = state.fileTint,
                isArtworkVisible = state.isArtworkVisible,
                artwork = state.artwork,
                isLoading = state.isLoading,
                onImageClick = onImageClick,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .padding(top = 30.dp)
                    .align(Alignment.CenterHorizontally),
            )
            AddFileImageButton(
                text = stringResource(state.imageButtonRes),
                isLockVisible = state.isReadOnly,
                onClick = onImageButtonClick,
                modifier = Modifier.padding(top = 8.dp),
            )
            AddFileColourPicker(
                colours = state.colours,
                imageItem = state.imageItem,
                selectedIndex = state.selectedIndex,
                isLocked = state.isReadOnly,
                onColourClick = onColourClick,
                onLockedColourClick = onLockedColourClick,
                modifier = Modifier.padding(top = 30.dp),
            )
            if (state.isUpgradeVisible) {
                AddFileUpgradeCallout(
                    onClick = onUpgradeClick,
                    onCloseClick = onUpgradeCloseClick,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}

@Composable
private fun SaveAction(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxHeight()
            .padding(end = 16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 8.dp),
        ) {
            TextH50(
                text = stringResource(LR.string.profile_files_addfile_save).uppercase(),
                color = MaterialTheme.theme.colors.secondaryText01,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun NameField(name: String, onNameChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboardController = LocalSoftwareKeyboardController.current
    FormField(
        value = name,
        placeholder = stringResource(LR.string.profile_files_add_filename),
        onValueChange = onNameChange,
        onImeAction = { keyboardController?.hide() },
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
        modifier = modifier,
    )
}
