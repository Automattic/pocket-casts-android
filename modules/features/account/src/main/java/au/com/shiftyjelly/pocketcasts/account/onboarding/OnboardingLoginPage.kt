package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OnboardingLoginPage(
    onBackPressed: () -> Unit
) {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.welcome_back),
            onNavigationClick = onBackPressed
        )
    }
}
