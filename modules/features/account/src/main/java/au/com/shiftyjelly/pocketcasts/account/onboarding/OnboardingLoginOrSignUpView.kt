package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun OnboardingLoginOrSignUpView(
    onNotNowPressed: () -> Unit,
    onSignUpFreePressed: () -> Unit,
    onLoginPressed: () -> Unit,
    showToast: (String) -> Unit
) {
    Column {
        Text("login_or_sign_up")

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

        Button(onClick = onLoginPressed) {
            Text("Log in")
        }
    }
}
