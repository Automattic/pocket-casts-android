package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10

@Composable
fun OnboardingPlusFeatures(
    onShown: () -> Unit,
    onBackPressed: () -> Unit,
) {

    LaunchedEffect(Unit) { onShown() }
    BackHandler { onBackPressed() }

    Column {
        TextH10("So many features!!!")
    }
}

@Preview
@Composable
private fun backgroundPreview() {
    OnboardingPlusFeatures(
        onShown = {},
        onBackPressed = {},
    )
}
