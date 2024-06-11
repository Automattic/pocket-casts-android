package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import au.com.shiftyjelly.pocketcasts.account.viewmodel.DoneViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_CLOSE_PARENT = "close_parent"

@AndroidEntryPoint
class ChangeDoneFragment : BaseFragment() {
    companion object {
        fun newInstance(closeParent: Boolean = false): ChangeDoneFragment {
            return ChangeDoneFragment().apply {
                arguments = bundleOf(ARG_CLOSE_PARENT to closeParent)
            }
        }
    }

    private val viewModel: DoneViewModel by activityViewModels()

    private val shouldCloseParent: Boolean
        get() = arguments?.getBoolean(ARG_CLOSE_PARENT) ?: false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    ChangeDonePage(
                        viewModel = viewModel,
                        closeForm = {
                            closeForm()
                        },
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun closeForm() {
        val activity = activity ?: return
        activity.onBackPressed()
        if (shouldCloseParent) {
            activity.onBackPressed()
        }
    }

    override fun onBackPressed(): Boolean {
        viewModel.trackDismissed()
        return super.onBackPressed()
    }
}
