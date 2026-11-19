package au.com.shiftyjelly.pocketcasts.account

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountError
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SubscriptionType
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import com.automattic.eventhorizon.CreateAccountNextButtonTappedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.OnboardingFlowType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CreateEmailFragment : BaseFragment() {
    @Inject lateinit var eventHorizon: EventHorizon

    private val viewModel: CreateAccountViewModel by activityViewModels()
    private var form by mutableStateOf(CreateEmailForm())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            val isCar = Util.isCarUiMode(requireContext())
            val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            val form = form
            var email by rememberSaveable { mutableStateOf(viewModel.email.value.orEmpty()) }
            var password by rememberSaveable { mutableStateOf(viewModel.password.value.orEmpty()) }
            CreateEmailPage(
                state = CreateEmailPageState(
                    isCar = isCar,
                    isLandscape = isLandscape,
                    email = email,
                    password = password,
                    isEmailValid = !form.isEmailInvalid,
                    isFormValid = form.isFormValid,
                    isPasswordInvalid = form.isPasswordInvalid,
                    isProgressVisible = form.isProgressVisible,
                    errorText = form.errorText,
                ),
                onEmailChange = {
                    email = it
                    viewModel.updateEmail(it)
                },
                onPasswordChange = {
                    password = it
                    viewModel.updatePassword(it)
                },
                onFocusChange = { isEmail ->
                    if (isEmail) viewModel.updateEmailRefresh() else viewModel.updatePasswordRefresh()
                },
                onNextClick = ::next,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        form = CreateEmailForm()

        viewModel.clearError(CreateAccountError.CANNOT_CREATE_ACCOUNT)

        val startingEmailValue = viewModel.email.value ?: ""
        val startingPasswordValue = viewModel.password.value ?: ""
        viewModel.updateEmail(startingEmailValue)
        viewModel.updatePassword(startingPasswordValue)

        viewModel.createAccountState.observe(viewLifecycleOwner) {
            when (it) {
                is CreateAccountState.CurrentlyValid -> {
                    form = form.copy(isProgressVisible = false)
                    updateForm(invalidEmail = false, invalidPassword = false)
                }

                is CreateAccountState.AccountCreating -> {
                    form = form.copy(isProgressVisible = true)
                }

                is CreateAccountState.AccountCreated -> {
                    form = form.copy(isProgressVisible = false)
                    if (viewModel.subscriptionType.value == SubscriptionType.FREE) {
                        findNavController().navigate(R.id.action_createEmailFragment_to_createDoneFragment)
                    }
                }

                is CreateAccountState.Failure -> {
                    form = form.copy(isProgressVisible = false)
                    val invalidEmail = viewModel.currentStateHasError(CreateAccountError.INVALID_EMAIL)
                    val invalidPassword = viewModel.currentStateHasError(CreateAccountError.INVALID_PASSWORD)
                    val serverFail = viewModel.currentStateHasError(CreateAccountError.CANNOT_CREATE_ACCOUNT)
                    updateForm(invalidEmail, invalidPassword)

                    if (serverFail) {
                        form = form.copy(errorText = it.message ?: getString(LR.string.profile_create_failed))
                        viewModel.clearError(CreateAccountError.CANNOT_CREATE_ACCOUNT)
                    }
                }

                else -> {}
            }
        }
    }

    override fun onPause() {
        super.onPause()

        val view = view ?: return
        UiUtil.hideKeyboard(view)
    }

    private fun next() {
        if (!viewModel.currentStateHasError(CreateAccountError.INVALID_EMAIL) &&
            !viewModel.currentStateHasError(CreateAccountError.INVALID_PASSWORD)
        ) {
            form = form.copy(errorText = "")
            view?.let(UiUtil::hideKeyboard)
            eventHorizon.track(CreateAccountNextButtonTappedEvent(flow = OnboardingFlowType.Unknown))
            viewModel.sendCreateAccount()
        }
    }

    private fun updateForm(invalidEmail: Boolean, invalidPassword: Boolean) {
        form = form.copy(
            isEmailInvalid = invalidEmail || viewModel.email.value.isNullOrEmpty(),
            isPasswordInvalid = invalidPassword && !viewModel.password.value.isNullOrEmpty(),
            isFormValid = !invalidEmail && !invalidPassword,
        )
    }
}

private data class CreateEmailForm(
    val isEmailInvalid: Boolean = true,
    val isPasswordInvalid: Boolean = false,
    val isFormValid: Boolean = true,
    val isProgressVisible: Boolean = false,
    val errorText: String = "",
)
