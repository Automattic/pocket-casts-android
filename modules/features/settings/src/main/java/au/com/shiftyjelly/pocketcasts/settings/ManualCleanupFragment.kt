package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.ManualCleanupViewModel
import au.com.shiftyjelly.pocketcasts.views.extensions.findToolbar
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ARG_TOOLBAR = "addtoolbar"

@AndroidEntryPoint
class ManualCleanupFragment private constructor() : BaseFragment() {
    companion object {
        fun newInstance(addToolbar: Boolean = false): ManualCleanupFragment {
            val fragment = ManualCleanupFragment()
            fragment.arguments = bundleOf(
                ARG_TOOLBAR to addToolbar
            )
            return fragment
        }
    }

    private val viewModel: ManualCleanupViewModel by viewModels()
    private val addToolbar: Boolean
        get() = arguments?.getBoolean(ARG_TOOLBAR) ?: false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme(theme.activeTheme) {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    ManualCleanupPage(
                        viewModel = viewModel,
                        showToolbar = addToolbar, // fragment needs to add it's own toolbar when it is added to fragment host
                        onBackClick = { activity?.onBackPressed() },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /*  When the fragment is added as a child fragment to it's parent, which already has it's toolbar,
        title is updated on the parent's toolbar. */
        parentFragment?.view?.findToolbar()?.title =
            getString(LR.string.settings_title_manage_downloads)
    }
}
