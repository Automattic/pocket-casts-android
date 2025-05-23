package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class BookmarkUpsellViewHolder(
    private val composeView: ComposeView,
    private val onGetBookmarksClicked: () -> Unit,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {
    fun bind() {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                val context = LocalContext.current
                NoContentBanner(
                    title = stringResource(LR.string.bookmarks_empty_state_title),
                    body = stringResource(LR.string.bookmarks_free_user_empty_state_message),
                    iconResourceId = IR.drawable.ic_bookmark,
                    primaryButtonText = stringResource(LR.string.bookmarks_free_user_empty_state_button),
                    onPrimaryButtonClick = {
                        onGetBookmarksClicked()
                        val onboardingFlow = OnboardingFlow.Upsell(
                            source = OnboardingUpgradeSource.BOOKMARKS,
                        )
                        OnboardingLauncher.openOnboardingFlow(requireNotNull(context.getActivity()), onboardingFlow)
                    },
                    modifier = Modifier.padding(top = 56.dp),
                )
            }
        }
    }
}
