package au.com.shiftyjelly.pocketcasts.account.onboarding

import au.com.shiftyjelly.pocketcasts.account.viewmodel.AccountViewModel

abstract class OnboardingSubmissionState(
    email: String,
    password: String,
    isCallInProgress: Boolean,
    hasAttemptedLogIn: Boolean,
) {
    val isEmailValid = AccountViewModel.isEmailValid(email)
    val isPasswordValid = AccountViewModel.isPasswordValid(password)

    val showEmailError = hasAttemptedLogIn && !isEmailValid
    val showPasswordError = hasAttemptedLogIn && !isPasswordValid

    val isReadyToSubmit =
        isEmailValid &&
            isPasswordValid &&
            !isCallInProgress
}
