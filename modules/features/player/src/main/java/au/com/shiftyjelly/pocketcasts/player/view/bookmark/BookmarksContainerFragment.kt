package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectController
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectTopAppBar
import com.automattic.eventhorizon.EventHorizon
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class BookmarksContainerFragment : BaseDialogFragment() {
    companion object {
        private const val ARG_EPISODE_UUID = "episodeUUID"
        private const val ARG_SOURCE_VIEW = "sourceView"
        fun newInstance(
            episodeUuid: String? = null,
            sourceView: SourceView,
        ) = BookmarksContainerFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_EPISODE_UUID, episodeUuid)
                putString(ARG_SOURCE_VIEW, sourceView.key)
            }
        }
    }

    private val episodeUUID: String?
        get() = arguments?.getString(ARG_EPISODE_UUID)

    private val sourceView: SourceView
        get() = SourceView.fromString(arguments?.getString(ARG_SOURCE_VIEW))

    override val statusBarIconColor: StatusBarIconColor
        get() = if (sourceView == SourceView.PROFILE) {
            StatusBarIconColor.Theme
        } else {
            StatusBarIconColor.Light
        }

    private val bookmarksViewModel: BookmarksViewModel by viewModels()

    @Inject
    lateinit var eventHorizon: EventHorizon

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = requireContext()
        return LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(context.getThemeColor(UR.attr.primary_ui_02))
            addView(
                ComposeView(context).apply {
                    consumeWindowInsets = false
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent { TopBar() }
                },
            )
            addView(
                FrameLayout(context).apply { id = R.id.fragmentContainer },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }
    }

    @Composable
    private fun TopBar() {
        val controller = remember { MultiSelectController(bookmarksViewModel.multiSelectHelper, viewLifecycleOwner.lifecycleScope) }
        val isMultiSelecting by controller.isMultiSelecting.collectAsStateWithLifecycle()
        AppTheme(theme.activeTheme) {
            BookmarksContainerTopBar(
                isDialog = dialog != null,
                onNavigationClick = {
                    if (dialog == null) {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    } else {
                        dismiss()
                    }
                },
                multiSelectTopBar = if (isMultiSelecting) {
                    { MultiSelectTopAppBar(controller, eventHorizon) }
                } else {
                    null
                },
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { theme.toggleDarkLightThemeActivity(requireActivity() as AppCompatActivity) })
                },
            )
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheetDialog = dialog as? BottomSheetDialog
        bottomSheetDialog?.onBackPressedDispatcher?.addCallback(
            this,
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

        bookmarksViewModel.multiSelectHelper.context = context

        childFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                BookmarksFragment.newInstance(
                    sourceView = sourceView,
                    episodeUuid = episodeUUID,
                ),
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        with(bookmarksViewModel.multiSelectHelper) {
            isMultiSelecting = false
            context = null
        }
    }
}
