package au.com.shiftyjelly.pocketcasts.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.account.AccountActivity.AccountUpdatedSource
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ChangeEmailViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.DoneViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ChangeEmailFragment : BaseFragment() {
    companion object {
        fun newInstance(): ChangeEmailFragment {
            return ChangeEmailFragment()
        }
    }

    private val viewModel: ChangeEmailViewModel by viewModels()
    private val doneViewModel: DoneViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                val email: String by viewModel.email.observeAsState("")
                val password: String by viewModel.password.observeAsState("")
                AppThemeWithBackground(theme.activeTheme) {
                    // Text("Test")
                    ChangeEmailFragmentPage(
                        changeEmailState = viewModel.changeEmailState.value,
                        email = email,
                        password = password,
                        updateEmail = viewModel::updateEmail,
                        updatePassword = viewModel::updatePassword,
                        onBackPressed = {
                            @Suppress("DEPRECATION")
                            activity?.onBackPressed()
                        },
                        changeEmail = viewModel::changeEmail,
                        clearServerError = viewModel::clearServerError,
                        onSuccess = {
                            val second = viewModel.email.value ?: ""
                            doneViewModel.updateTitle(getString(LR.string.profile_email_address_changed))
                            doneViewModel.updateDetail(second)
                            doneViewModel.updateImage(R.drawable.ic_email_address_changed)
                            doneViewModel.trackShown(AccountUpdatedSource.CHANGE_EMAIL)

                            val activity = requireActivity()

                            @Suppress("DEPRECATION")
                            activity.onBackPressed() // done fragment needs to back to profile page

                            val fragment = ChangeDoneFragment.newInstance()
                            (activity as FragmentHostListener).addFragment(fragment)
                        },
                        existingEmail = viewModel.existingEmail ?: ""
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // hack: enable scrolling upon keyboard
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onDetach() {
        super.onDetach()
        // hack: enable scrolling upon keyboard
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.clearValues()
    }
}
