package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20

@Composable
fun OnboardingWelcomePage(
    onContinue: () -> Unit
) {
    BackHandler {
        onContinue()
    }

    TextH20("Welcome")
}
