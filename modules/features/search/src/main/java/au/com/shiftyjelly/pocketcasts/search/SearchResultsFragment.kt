package au.com.shiftyjelly.pocketcasts.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_TYPE = "arg_type"

@AndroidEntryPoint
class SearchResultsFragment : BaseFragment() {
    private val viewModel by activityViewModels<SearchViewModel>()

    val type: ResultsType
        get() = ResultsType.fromString(arguments?.getString(ARG_TYPE))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppThemeWithBackground(theme.activeTheme) {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                when (type) {
                    ResultsType.PODCASTS -> {
                        SearchPodcastResultsPage(
                            viewModel = viewModel,
                            onBackClick = ::onBackClick,
                        )
                    }

                    ResultsType.EPISODES -> {
                        SearchEpisodeResultsPage(
                            viewModel = viewModel,
                            onBackClick = ::onBackClick,
                        )
                    }

                    ResultsType.UNKNOWN -> throw IllegalStateException("Unknown search results type")
                }
            }
        }
    }

    private fun onBackClick() {
        @Suppress("DEPRECATION")
        activity?.onBackPressed()
    }

    companion object {
        enum class ResultsType(val value: String) {
            PODCASTS("podcasts"),
            EPISODES("episodes"),
            UNKNOWN("unknown");

            companion object {
                fun fromString(value: String?) =
                    ResultsType.values().find { it.value == value } ?: UNKNOWN
            }
        }

        fun newInstance(type: ResultsType): SearchResultsFragment {
            val fragment = SearchResultsFragment()
            val arguments = Bundle().apply {
                putString(ARG_TYPE, type.value)
            }
            fragment.arguments = arguments
            return fragment
        }
    }
}
