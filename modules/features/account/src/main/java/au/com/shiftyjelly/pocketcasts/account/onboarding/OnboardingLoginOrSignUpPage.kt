package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
internal fun OnboardingLoginOrSignUpPage(
    onNotNowClicked: () -> Unit,
    onSignUpFreeClicked: () -> Unit,
    onLoginClicked: () -> Unit,
    onLoginGoogleClicked: () -> Unit
) {
    Column {
        Text("login_or_sign_up")

        Button(onClick = onNotNowClicked) {
            Text("Not Now")
        }

        Button(onClick = onLoginGoogleClicked) {
            Text("Continue with Google")
        }

        Button(onClick = onSignUpFreeClicked) {
            Text("Sign up Free")
        }

        Button(onClick = onLoginClicked) {
            Text("Log in")
        }
    }
}
