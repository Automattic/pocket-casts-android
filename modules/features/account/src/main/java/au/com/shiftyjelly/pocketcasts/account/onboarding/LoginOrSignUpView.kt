package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun LoginOrSignUpView(
    onNotNowPressed: () -> Unit,
    onSignUpFreePressed: () -> Unit,
    showToast: (String) -> Unit
) {
    Column {
        Text(OnboardingNavRoute.loginOrSignUp)

        Button(onClick = onNotNowPressed) {
            Text("Not Now")
        }

        Button(onClick = {
            showToast("Tapped\nContinue with Google")
        }) {
            Text("Continue with Google")
        }

        Button(onClick = onSignUpFreePressed) {
            Text("Sign up Free")
        }

        Button(onClick = {
            showToast("Tapped\nLog in")
        }) {
            Text("Log in")
        }
    }
}
