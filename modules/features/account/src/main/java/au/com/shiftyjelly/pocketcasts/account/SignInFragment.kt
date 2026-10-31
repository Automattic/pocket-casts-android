package au.com.shiftyjelly.pocketcasts.account

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInError
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInFragment : BaseFragment() {

    private val viewModel: SignInViewModel by viewModels()
    private var form by mutableStateOf(SignInForm())
    private var fieldsState: SignInFieldsState? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            val isCar = Util.isCarUiMode(requireContext())
            val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            val form = form
            val fieldsState = rememberSignInFieldsState().also { this@SignInFragment.fieldsState = it }
            SignInPage(
                state = SignInPageState(
                    isCar = isCar,
                    isLandscape = isLandscape,
                    isFormValid = !form.isEmailInvalid && !form.isPasswordInvalid,
                    isLoading = form.isLoading,
                    isProgressVisible = form.isProgressVisible,
                    errorText = form.errorText,
                ),
                onSignInClick = ::signIn,
                onForgotPasswordClick = { findNavController().navigate(R.id.action_signInFragment_to_resetPasswordFragment) },
                fields = {
                    SignInFields(
                        state = fieldsState,
                        isCar = isCar,
                        isLandscape = isLandscape,
                        isEmailInvalid = form.isEmailInvalid,
                        onEmailChange = viewModel::updateEmail,
                        onPasswordChange = viewModel::updatePassword,
                        onSubmit = ::signIn,
                    )
                },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        form = SignInForm()

        viewModel.clearValues()

        viewModel.signInState.observe(viewLifecycleOwner) {
            when (it) {
                is SignInState.Empty -> {
                    updateForm(invalidEmail = false, invalidPwd = false, loading = false)
                }

                is SignInState.Failure -> {
                    form = form.copy(isProgressVisible = false)

                    val invalidEmail = it.errors.contains(SignInError.INVALID_EMAIL)
                    val invalidPwd = it.errors.contains(SignInError.INVALID_PASSWORD)
                    val serverFail = it.errors.contains(SignInError.SERVER)

                    updateForm(invalidEmail, invalidPwd, loading = false)

                    if (serverFail) {
                        val error = it.message ?: "Check your email and password"
                        form = form.copy(errorText = error)
                        viewModel.clearServerError()
                    }
                }

                is SignInState.Loading -> {
                    form = form.copy(errorText = form.errorText?.let { "" }, isProgressVisible = true)

                    updateForm(invalidEmail = false, invalidPwd = false, loading = true)
                }

                is SignInState.Success -> {
                    form = form.copy(isProgressVisible = false)
                    if (findNavController().graph.startDestinationId == R.id.promoCodeFragment) {
                        findNavController().popBackStack(R.id.promoCodeFragment, false)
                    } else {
                        activity?.finish()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fieldsState = null
    }

    private fun signIn() {
        val view = view ?: return
        form = form.copy(isProgressVisible = true)
        UiUtil.hideKeyboard(view)
        viewModel.signIn()
    }

    override fun onPause() {
        super.onPause()

        val view = view ?: return
        UiUtil.hideKeyboard(view)
    }

    private fun updateForm(invalidEmail: Boolean, invalidPwd: Boolean, loading: Boolean) {
        form = form.copy(isEmailInvalid = invalidEmail, isPasswordInvalid = invalidPwd, isLoading = loading)
        fieldsState?.updateIcons(invalidEmail)
    }
}

private data class SignInForm(
    val isEmailInvalid: Boolean = false,
    val isPasswordInvalid: Boolean = false,
    val isLoading: Boolean = false,
    val isProgressVisible: Boolean = false,
    val errorText: String? = null,
)
