package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ChangePwdViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.DoneViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ChangePwdFragment : BaseFragment() {
    companion object {
        fun newInstance(): ChangePwdFragment {
            return ChangePwdFragment()
        }
    }

    @Inject
    lateinit var settings: Settings

    private val doneViewModel: DoneViewModel by activityViewModels()
    private val viewModel: ChangePwdViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val bottomOffset by settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
                AppThemeWithBackground(theme.activeTheme) {
                    ChangePasswordPage(
                        viewModel = viewModel,
                        onBackPressed = {
                            @Suppress("DEPRECATION")
                            activity?.onBackPressed()
                        },
                        changePassword = {
                            viewModel.changePassword()
                        },
                        onSuccess = {
                            doneViewModel.setChangedPasswordState(detail = getString(LR.string.profile_password_changed_successful))

                            doneViewModel.trackShown(AccountActivity.AccountUpdatedSource.CHANGE_PASSWORD)

                            val fragment = ChangeDoneFragment.newInstance(closeParent = true)
                            (activity as FragmentHostListener).addFragment(fragment)
                        },
                        bottomOffset = bottomOffset.pxToDp(LocalContext.current).dp,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.clearValues()
        viewModel.clearServerError()
    }
}
