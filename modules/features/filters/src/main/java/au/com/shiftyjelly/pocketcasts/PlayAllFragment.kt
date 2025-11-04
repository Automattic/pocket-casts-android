package au.com.shiftyjelly.pocketcasts

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
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
                onSaveQueue = {
                    viewModel.trackSaveUpNextTapped()
                    viewModel.saveUpNextAsPlaylist(getString(LR.string.up_next))
                    viewModel.playAll()
                    isFinalizingActionUsed = true
                    dismiss()
                },
                onReplaceAndPlay = {
                    viewModel.trackReplaceAndPlayTapped()
                },
                onConfirmReplaceAndPlay = {
                    viewModel.trackReplaceAndPlayConfirmTapped()
                    viewModel.playAll()
                    isFinalizingActionUsed = true
                    dismiss()
                },
                onDismiss = ::dismiss,
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

private enum class PlayAllNavigationKey {
    SaveQueue,
    ReplaceAndPlay,
}

@Composable
private fun PlayAllPage(
    onSaveQueue: () -> Unit,
    onReplaceAndPlay: () -> Unit,
    onConfirmReplaceAndPlay: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var navigationKey by rememberSaveable { mutableStateOf(PlayAllNavigationKey.SaveQueue) }

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
        AnimatedContent(targetState = navigationKey) { state ->
            when (state) {
                PlayAllNavigationKey.SaveQueue -> SaveQueuePage(
                    onSaveQueue = onSaveQueue,
                    onReplaceAndPlay = {
                        navigationKey = PlayAllNavigationKey.ReplaceAndPlay
                        onReplaceAndPlay()
                    },
                )

                PlayAllNavigationKey.ReplaceAndPlay -> ReplaceAndPlayPage(
                    onReplaceAndPlay = onConfirmReplaceAndPlay,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun SaveQueuePage(
    onSaveQueue: () -> Unit,
    onReplaceAndPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, start = 20.dp, end = 20.dp),
    ) {
        Image(
            painter = painterResource(IR.drawable.ic_upnext),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon01),
            modifier = Modifier.size(36.dp),
        )
        Spacer(
            modifier = Modifier.height(18.dp),
        )
        TextH20(
            text = stringResource(LR.string.up_next_as_playlist_title),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        TextP40(
            text = stringResource(LR.string.up_next_as_playlist_body),
            fontSize = 15.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.theme.colors.primaryText02,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(24.dp),
        )
        RowButton(
            text = stringResource(LR.string.up_next_as_playlist_button_primary),
            includePadding = false,
            onClick = onSaveQueue,
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        RowOutlinedButton(
            text = stringResource(LR.string.up_next_as_playlist_button_secondary),
            includePadding = false,
            onClick = onReplaceAndPlay,
        )
    }
}

@Composable
private fun ReplaceAndPlayPage(
    onReplaceAndPlay: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        RowButton(
            text = stringResource(LR.string.up_next_as_playlist_replace_button_primary),
            includePadding = false,
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.theme.colors.support05),
            onClick = onReplaceAndPlay,
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        RowOutlinedButton(
            text = stringResource(LR.string.up_next_as_playlist_replace_button_secondary),
            includePadding = false,
            onClick = onDismiss,
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
            onSaveQueue = {},
            onReplaceAndPlay = {},
            onConfirmReplaceAndPlay = {},
            onDismiss = {},
        )
    }
}
