package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingForgotPassword(
    onBackPressed: () -> Unit,
) {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.profile_reset_password),
            onNavigationClick = onBackPressed
        )
    }
}
