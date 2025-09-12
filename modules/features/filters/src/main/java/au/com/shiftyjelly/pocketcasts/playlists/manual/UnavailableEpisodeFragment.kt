package au.com.shiftyjelly.pocketcasts.playlists.manual

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistViewModel
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class UnavailableEpisodeFragment : BaseDialogFragment() {
    private val viewModel by viewModels<PlaylistViewModel>({ requireParentFragment() })

    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        DialogBox(
            fillMaxHeight = false,
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(MaterialTheme.theme.colors.primaryUi05, CircleShape)
                        .size(56.dp, 4.dp),
                )
                UnavailableEpisodePage(
                    onClickRemove = {
                        viewModel.deleteEpisode(args.episodeUuid)
                        dismiss()
                    },
                    modifier = Modifier.padding(
                        top = 24.dp,
                        bottom = 16.dp,
                        start = 20.dp,
                        end = 20.dp,
                    ),
                )
            }
        }
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "UnavailableEpisodeFragmentArgs"

        fun newInstance(
            episodeUuid: String,
        ) = UnavailableEpisodeFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(episodeUuid))
        }
    }
}
