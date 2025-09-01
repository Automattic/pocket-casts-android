package au.com.shiftyjelly.pocketcasts.playlists.manual.episode

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedNonNullVisibility
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class AddEpisodesFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    private val viewModel by viewModels<AddEpisodesViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<AddEpisodesViewModel.Factory> { factory ->
                factory.create(playlistUuid = args.playlistUuid)
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        DialogBox(
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            AnimatedNonNullVisibility(
                item = viewModel.uiState.collectAsState().value,
                enter = fadeIn,
                exit = fadeOut,
            ) { uiState ->
                AddEpisodesPage(
                    playlistTitle = uiState.playlist.title,
                    episodeSources = uiState.sources,
                    episodesFlow = viewModel::getEpisodesFlow,
                    useEpisodeArtwork = uiState.useEpisodeArtwork,
                    onClose = ::dismiss,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Parcelize
    private class Args(
        val playlistUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "SmartRulesEditFragmentArgs"

        fun newInstance(playlistUuid: String) = AddEpisodesFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(playlistUuid))
        }
    }
}

private val fadeIn = fadeIn()
private val fadeOut = fadeOut()
