package au.com.shiftyjelly.pocketcasts.playlists.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CreatePlaylistFragment : BaseDialogFragment() {
    private val viewModel by viewModels<CreatePlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<CreatePlaylistViewModel.Factory> { factory ->
                factory.create(initialPlaylistName = getString(LR.string.new_playlist))
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        Box(
            modifier = Modifier
                .fillMaxHeight(0.93f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        ) {
            AppThemeWithBackground(
                themeType = theme.activeTheme,
            ) {
                NewPlaylistPage(
                    titleState = viewModel.playlistNameState,
                    onCreateManualPlaylist = { Timber.i("Create Manual Playlist") },
                    onContinueToSmartPlaylist = { Timber.i("Continue to Smart Playlist") },
                    onClickClose = ::dismiss,
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .nestedScroll(rememberNestedScrollInteropConnection())
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}
