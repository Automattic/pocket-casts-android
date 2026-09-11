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
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.discover.compose.NetworksGridPage
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.NetworksGridViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.automattic.eventhorizon.DiscoverListShowAllTappedEvent
import com.automattic.eventhorizon.EventHorizon
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

    @Inject lateinit var eventHorizon: EventHorizon

    private val viewModel: NetworksGridViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val bottomInsetPx by settings.bottomInset.collectAsStateWithLifecycle(0)
        val bottomInset = with(LocalDensity.current) { bottomInsetPx.toDp() }

        AppTheme(theme.activeTheme) {
            NetworksGridPage(
                title = arguments?.getString(ARG_TITLE),
                state = state,
                onClickBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                onClickNetwork = { network ->
                    eventHorizon.track(
                        DiscoverListShowAllTappedEvent(
                            listId = network.uuid,
                            // No date passed as it's not available until the list is loaded
                            listDatetime = "",
                        ),
                    )
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sourceUrl = arguments?.getString(ARG_SOURCE_URL) ?: return
        viewModel.load(sourceUrl)
    }
}
