package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.graphics.Bitmap
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import au.com.shiftyjelly.pocketcasts.compose.bars.AppCompatTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewAlignment
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.profile.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import au.com.shiftyjelly.pocketcasts.images.R as IR
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
        AppCompatTopAppBar(
            title = state.titleRes?.let { stringResource(it) }.orEmpty(),
            navigationButton = NavigationButton.Drawable(IR.drawable.ic_arrow_back, LR.string.back),
            onNavigationClick = onNavigationClick,
            iconColor = colors.secondaryIcon01,
            titleColor = colors.secondaryText01,
            backgroundColor = colors.secondaryUi01,
            bottomShadow = false,
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
                TextViewText(
                    text = state.fileName,
                    fontSize = 14.sp,
                    color = colors.primaryText02,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp)
                        .weight(1f),
                )
                TextViewText(
                    text = state.fileSize,
                    fontSize = 14.sp,
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
        contentAlignment = TextViewAlignment.Center,
        modifier = Modifier
            .fillMaxHeight()
            .padding(end = 16.dp),
    ) {
        Box(
            contentAlignment = TextViewAlignment.Center,
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 8.dp),
        ) {
            TextViewText(
                text = stringResource(LR.string.profile_files_addfile_save).uppercase(),
                fontSize = 14.sp,
                color = MaterialTheme.theme.colors.secondaryText01,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun NameField(name: String, onNameChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val hint = stringResource(LR.string.profile_files_add_filename)
    val textColor = MaterialTheme.theme.colors.primaryText01.toArgb()
    val currentOnNameChange by rememberUpdatedState(onNameChange)
    AndroidView(
        factory = { context ->
            TextInputLayout(ContextThemeWrapper(context, R.style.AddFileNameInput)).apply {
                val editText = TextInputEditText(this.context).apply {
                    imeOptions = EditorInfo.IME_ACTION_DONE
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    doAfterTextChanged { currentOnNameChange(it.toString()) }
                }
                addView(editText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        },
        update = { layout ->
            layout.hint = hint
            layout.editText?.let { editText ->
                editText.setTextColor(textColor)
                if (editText.text.toString() != name) {
                    editText.setText(name)
                }
            }
        },
        modifier = modifier,
    )
}
