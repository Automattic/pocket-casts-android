package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentBookmarksContainerBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeTintedDrawable
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

/**
 * Bottom sheet dialog version of Bookmarks container for use from Player.
 * Extends BaseDialogFragment and configures bottom sheet behavior.
 */
@AndroidEntryPoint
class BookmarksDialogFragment : BaseDialogFragment() {
    companion object {
        private const val ARG_EPISODE_UUID = "episodeUUID"
        private const val ARG_SOURCE_VIEW = "sourceView"

        fun newInstance(
            episodeUuid: String? = null,
            sourceView: SourceView = SourceView.PLAYER,
        ) = BookmarksDialogFragment().apply {
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

    override var statusBarIconColor: StatusBarIconColor = StatusBarIconColor.Light

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

        setupBottomSheetBehavior(view)

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
            .addToBackStack(null)
            .commit()

        binding.toolbar.setup(
            title = getString(LR.string.bookmarks),
            navigationIcon = NavigationIcon.Close,
            onNavigationClick = { dismiss() },
            activity = activity,
            theme = theme,
        )

        with(binding.toolbar) {
            navigationIcon = requireContext().getThemeTintedDrawable(
                R.drawable.ic_close,
                UR.attr.primary_icon_01,
            )
            setTitleTextColor(requireContext().getThemeColor(UR.attr.primary_text_01))
            setBackgroundColor(requireContext().getThemeColor(UR.attr.primary_ui_01))
        }
    }

    private fun setupBottomSheetBehavior(view: View) {
        val bottomSheetDialog = dialog as? BottomSheetDialog ?: return

        bottomSheetDialog.onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (bookmarksViewModel.multiSelectHelper.isMultiSelecting) {
                        bookmarksViewModel.multiSelectHelper.isMultiSelecting = false
                        return
                    }
                    dismiss()
                }
            },
        )

        bottomSheetDialog.behavior.apply {
            isFitToContents = false
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        bottomSheetDialog.setOnShowListener {
            view.updateLayoutParams<ViewGroup.LayoutParams> {
                height = Resources.getSystem().displayMetrics.heightPixels
            }
        }
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
