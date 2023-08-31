package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.content.res.Resources
import android.graphics.Color
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
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class BookmarksContainerFragment :
    BaseDialogFragment() {
    companion object {
        private const val ARG_EPISODE_UUID = "episodeUUID"
        private const val ARG_SOURCE_VIEW = "sourceView"
        fun newInstance(
            episodeUuid: String,
            sourceView: SourceView,
        ) = BookmarksContainerFragment().apply {
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

    override val statusBarColor: StatusBarColor
        get() = StatusBarColor.Custom(
            context?.getThemeColor(UR.attr.primary_ui_01)
                ?: Color.WHITE,
            theme.isDarkTheme
        )

    var binding: FragmentBookmarksContainerBinding? = null

    @Inject
    lateinit var multiSelectHelper: MultiSelectBookmarksHelper
    private val sharedBookmarksViewModel: SharedBookmarksViewModel by viewModels()

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
        val bottomSheetDialog = dialog as? BottomSheetDialog
        bottomSheetDialog?.onBackPressedDispatcher?.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (multiSelectHelper.isMultiSelecting) {
                        multiSelectHelper.isMultiSelecting = false
                        return
                    }
                    dismiss()
                }
            }
        )

        bottomSheetDialog?.behavior?.apply {
            isFitToContents = false
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        // Ensure the dialog ends up the full height of the screen
        // Bottom sheet dialogs get wrapped in a sheet that is WRAP_CONTENT so setting MATCH_PARENT on our
        // root view is ignored.
        bottomSheetDialog?.setOnShowListener {
            view.updateLayoutParams<ViewGroup.LayoutParams> {
                height = Resources.getSystem().displayMetrics.heightPixels
            }
        }

        val binding = binding ?: return

        binding.setupMultiSelectHelper()

        childFragmentManager.beginTransaction()
            .replace(
                binding.fragmentContainer.id,
                BookmarksFragment.newInstance(
                    sourceView = sourceView,
                    episodeUuid = episodeUUID,
                )
            )
            .addToBackStack(null)
            .commit()

        binding.btnClose.setOnClickListener { dismiss() }
    }

    private fun FragmentBookmarksContainerBinding.setupMultiSelectHelper() {
        sharedBookmarksViewModel.multiSelectHelper = multiSelectHelper
        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            multiSelectToolbar.isVisible = isMultiSelecting
            multiSelectToolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        }
        multiSelectHelper.context = context
        multiSelectToolbar.setup(
            lifecycleOwner = viewLifecycleOwner,
            multiSelectHelper = multiSelectHelper,
            menuRes = null,
            fragmentManager = parentFragmentManager,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        multiSelectHelper.isMultiSelecting = false
        multiSelectHelper.context = null
    }
}
