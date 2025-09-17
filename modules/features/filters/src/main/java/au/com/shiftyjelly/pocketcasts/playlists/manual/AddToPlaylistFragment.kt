package au.com.shiftyjelly.pocketcasts.playlists.manual

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class AddToPlaylistFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        DialogBox(
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            TextP40(
                text = "Add episode '${args.episodeUuid}'",
            )
        }
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "AddToPlaylistFragmentArgs"

        fun newInstance(episodeUuid: String) = AddToPlaylistFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(episodeUuid))
        }
    }
}
