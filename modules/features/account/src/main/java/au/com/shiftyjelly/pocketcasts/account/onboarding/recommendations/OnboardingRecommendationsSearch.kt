package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingRecommendationsSearch(
    onBackPressed: () -> Unit,
) {
    BackHandler {
        onBackPressed()
    }

    Column(
        Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.onboarding_find_podcasts),
            onNavigationClick = onBackPressed
        )
    }
}
