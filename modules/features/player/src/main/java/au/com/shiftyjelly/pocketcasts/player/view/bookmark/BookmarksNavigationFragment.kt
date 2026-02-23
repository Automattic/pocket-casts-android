package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentBookmarksContainerBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * Regular fragment version of Bookmarks container for navigation from Profile.
 * Extends BaseFragment and uses standard fragment navigation.
 */
@AndroidEntryPoint
class BookmarksNavigationFragment : BaseFragment() {
    companion object {
        private const val ARG_EPISODE_UUID = "episodeUUID"
        private const val ARG_SOURCE_VIEW = "sourceView"

        fun newInstance(
            episodeUuid: String? = null,
            sourceView: SourceView = SourceView.PROFILE,
        ) = BookmarksNavigationFragment().apply {
            arguments = bundleOf(
                ARG_EPISODE_UUID to episodeUuid,
                ARG_SOURCE_VIEW to sourceView.analyticsValue,
            )
        }
    }

    private val episodeUUID: String?
        get() = arguments?.getString(ARG_EPISODE_UUID)

    private val sourceView: SourceView
        get() = SourceView.fromString(arguments?.getString(ARG_SOURCE_VIEW))

    override var statusBarIconColor: StatusBarIconColor = StatusBarIconColor.Theme

    private var binding: FragmentBookmarksContainerBinding? = null
    private val bookmarksViewModel: BookmarksViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentBookmarksContainerBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.setupMultiSelectHelper()

        childFragmentManager.beginTransaction()
            .replace(
                binding.fragmentContainer.id,
                BookmarksFragment.newInstance(
                    sourceView = sourceView,
                    episodeUuid = episodeUUID,
                ),
            )
            .commit()

        binding.toolbar.setup(
            title = getString(LR.string.bookmarks),
            navigationIcon = NavigationIcon.BackArrow,
            onNavigationClick = {
                activity?.onBackPressedDispatcher?.onBackPressed()
            },
            activity = activity,
            theme = theme,
        )
    }

    private fun FragmentBookmarksContainerBinding.setupMultiSelectHelper() {
        bookmarksViewModel.multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            multiSelectToolbar.isVisible = isMultiSelecting
            toolbar.isVisible = !isMultiSelecting
            multiSelectToolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        }
        bookmarksViewModel.multiSelectHelper.context = context
        multiSelectToolbar.setup(
            lifecycleOwner = viewLifecycleOwner,
            multiSelectHelper = bookmarksViewModel.multiSelectHelper,
            menuRes = null,
            activity = requireActivity(),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        with(bookmarksViewModel.multiSelectHelper) {
            isMultiSelecting = false
            context = null
        }
    }
}
