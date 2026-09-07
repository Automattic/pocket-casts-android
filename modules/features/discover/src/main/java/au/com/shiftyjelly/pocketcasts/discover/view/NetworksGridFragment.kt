package au.com.shiftyjelly.pocketcasts.discover.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.discover.compose.NetworksGrid
import au.com.shiftyjelly.pocketcasts.discover.databinding.FragmentNetworksGridBinding
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.NetworksGridViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NetworksGridFragment : BaseFragment() {
    companion object {
        private const val ARG_SOURCE_URL = "sourceUrl"
        private const val ARG_TITLE = "title"

        fun newInstance(sourceUrl: String, title: String?): NetworksGridFragment {
            return NetworksGridFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SOURCE_URL, sourceUrl)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }

    @Inject lateinit var settings: Settings

    private val viewModel: NetworksGridViewModel by viewModels()
    private var binding: FragmentNetworksGridBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentNetworksGridBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return
        val sourceUrl = arguments?.getString(ARG_SOURCE_URL) ?: return

        setupToolbarAndStatusBar(
            toolbar = binding.toolbar,
            title = arguments?.getString(ARG_TITLE),
            navigationIcon = BackArrow,
        )

        viewModel.load(sourceUrl)

        binding.networksGrid.setContentWithViewCompositionStrategy {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val bottomInsetPx by settings.bottomInset.collectAsStateWithLifecycle(0)
            val bottomInset = with(LocalDensity.current) { bottomInsetPx.toDp() }

            AppTheme(theme.activeTheme) {
                NetworksGrid(
                    state = state,
                    onClickNetwork = { network ->
                        (requireActivity() as FragmentHostListener).openNetworkPage(
                            listId = network.uuid,
                            title = network.title,
                            sourceView = SourceView.DISCOVER,
                        )
                    },
                    onClickRetry = viewModel::retry,
                    bottomInset = bottomInset,
                )
            }
        }
    }
}
