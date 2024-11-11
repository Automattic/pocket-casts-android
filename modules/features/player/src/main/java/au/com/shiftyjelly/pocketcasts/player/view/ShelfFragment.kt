package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.player.view.shelf.ShelfPage
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlin.getValue

@AndroidEntryPoint
class ShelfFragment : BaseFragment() {
    private val episodeId: String?
        get() = arguments?.getString(ARG_EPISODE_ID)

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val shelfViewModel: ShelfViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ShelfViewModel.Factory> { factory ->
                factory.create(episodeId)
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        AppTheme(theme.activeTheme) {
            ShelfPage(
                theme = theme,
                playerViewModel = playerViewModel,
                onBackPressed = {
                    shelfViewModel.trackRearrangeFinishedEvent()
                    (activity as? FragmentHostListener)?.closeModal(this)
                },
            )
        }
    }

    override fun onBackPressed(): Boolean {
        shelfViewModel.trackRearrangeFinishedEvent()
        return super.onBackPressed()
    }

    companion object {
        private const val ARG_EPISODE_ID = "episode_id"
        fun newInstance(
            episodeId: String? = null,
        ) = ShelfBottomSheet().apply {
            arguments = bundleOf(
                ARG_EPISODE_ID to episodeId,
            )
        }
    }
}
