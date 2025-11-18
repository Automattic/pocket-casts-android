package au.com.shiftyjelly.pocketcasts

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
internal class PlayAllFragment : BaseDialogFragment() {
    private val viewModel by viewModels<PlaylistViewModel>({ requireParentFragment() })

    private var isFinalizingActionUsed = false

    private fun isDismissedWithoutAction() = !requireActivity().isChangingConfigurations && !isFinalizingActionUsed

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        DialogBox(
            fillMaxHeight = false,
            modifier = Modifier
                .nestedScroll(rememberNestedScrollInteropConnection())
                .navigationBarsPadding(),
        ) {
            PlayAllPage(
                initialSaveUpNext = viewModel.saveUpNextDefaultValue,
                onReplaceAndPlay = { saveUpNext ->
                    isFinalizingActionUsed = true
                    viewModel.saveUpNextAsPlaylist(saveUpNext, getString(LR.string.up_next))
                    dismiss()
                },
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (isDismissedWithoutAction()) {
            viewModel.trackPlayAllDismissed()
        }
    }
}

@Composable
private fun PlayAllPage(
    initialSaveUpNext: Boolean,
    onReplaceAndPlay: (saveUpNext: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 32.dp)
                .background(MaterialTheme.theme.colors.primaryUi05, CircleShape)
                .size(56.dp, 4.dp),
        )
        ReplaceAndPlayPage(
            initialSaveUpNext = initialSaveUpNext,
            onReplaceAndPlay = onReplaceAndPlay,
        )
    }
}

@Composable
private fun ReplaceAndPlayPage(
    initialSaveUpNext: Boolean,
    onReplaceAndPlay: (saveUpNext: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var saveUpNext by rememberSaveable { mutableStateOf(initialSaveUpNext) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, start = 20.dp, end = 20.dp),
    ) {
        Image(
            painter = painterResource(IR.drawable.ic_playlist_play_all),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon01),
            modifier = Modifier.size(36.dp),
        )
        Spacer(
            modifier = Modifier.height(18.dp),
        )
        TextH20(
            text = stringResource(LR.string.up_next_as_playlist_replace_title),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        TextP40(
            text = stringResource(LR.string.up_next_as_playlist_replace_body),
            fontSize = 15.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.theme.colors.primaryText02,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(24.dp),
        )
        CompositionLocalProvider(
            LocalRippleConfiguration provides RippleConfiguration(color = MaterialTheme.theme.colors.primaryInteractive01),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.theme.colors.primaryUi02Active)
                    .fillMaxWidth()
                    .toggleable(
                        value = saveUpNext,
                        role = Role.Switch,
                        onValueChange = { isChecked -> saveUpNext = isChecked },
                    )
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) { },
            ) {
                TextP40(
                    text = stringResource(LR.string.up_next_as_playlist_replace_switch_description),
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.weight(1f),
                )
                Spacer(
                    modifier = Modifier.width(16.dp),
                )
                Switch(
                    checked = saveUpNext,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.Gray,
                    ),
                )
            }
        }
        Spacer(
            modifier = Modifier.height(24.dp),
        )
        RowButton(
            text = stringResource(LR.string.up_next_as_playlist_replace_button_primary),
            includePadding = false,
            onClick = { onReplaceAndPlay(saveUpNext) },
        )
    }
}

@Preview
@Composable
private fun PlayAllPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        PlayAllPage(
            initialSaveUpNext = true,
            onReplaceAndPlay = {},
        )
    }
}
