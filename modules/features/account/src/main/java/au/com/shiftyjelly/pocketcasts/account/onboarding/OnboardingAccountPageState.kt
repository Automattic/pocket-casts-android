package au.com.shiftyjelly.pocketcasts.account.onboarding

import au.com.shiftyjelly.pocketcasts.account.viewmodel.AccountViewModel

interface OnboardingSubmissionHelper {
    val isEmailValid: Boolean
    val isPasswordValid: Boolean
    val showEmailError: Boolean
    val showPasswordError: Boolean
    val isReadyToSubmit: Boolean
}

class OnboardingSubmissionHelperImpl(
    email: String,
    password: String,
    isCallInProgress: Boolean,
    hasAttemptedLogIn: Boolean,
) : OnboardingSubmissionHelper {
    override val isEmailValid = AccountViewModel.isEmailValid(email)
    override val isPasswordValid = AccountViewModel.isPasswordValid(password)

    override val showEmailError = hasAttemptedLogIn && !isEmailValid
    override val showPasswordError = hasAttemptedLogIn && !isPasswordValid

    override val isReadyToSubmit =
        isEmailValid &&
            isPasswordValid &&
            !isCallInProgress
}
