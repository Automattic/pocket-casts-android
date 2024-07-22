package au.com.shiftyjelly.pocketcasts.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.account.AccountActivity.AccountUpdatedSource
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ChangeEmailViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.DoneViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.extensions.setupKeyboardModePan
import au.com.shiftyjelly.pocketcasts.ui.extensions.setupKeyboardModeResize
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChangeEmailFragment : BaseFragment() {
    companion object {
        fun newInstance(): ChangeEmailFragment {
            return ChangeEmailFragment()
        }
    }
    private val viewModel: ChangeEmailViewModel by activityViewModels()
    private val doneViewModel: DoneViewModel by activityViewModels()

    @Inject lateinit var settings: Settings

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val email: String by viewModel.email.observeAsState("")
                val password: String by viewModel.password.observeAsState("")
                val bottomOffset by settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
                AppThemeWithBackground(theme.activeTheme) {
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

                            doneViewModel.setChangedEmailState(detail = second)

                            doneViewModel.trackShown(AccountUpdatedSource.CHANGE_EMAIL)

                            val activity = requireActivity()

                            @Suppress("DEPRECATION")
                            activity.onBackPressed() // done fragment needs to back to profile page

                            val fragment = ChangeDoneFragment.newInstance()
                            (activity as FragmentHostListener).addFragment(fragment)
                        },
                        existingEmail = viewModel.existingEmail ?: "",
                        bottomOffset = bottomOffset.pxToDp(LocalContext.current).dp,
                    )
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setupKeyboardModeResize()
    }

    override fun onDetach() {
        super.onDetach()
        setupKeyboardModePan()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.clearValues()
        viewModel.clearServerError()
    }
}
