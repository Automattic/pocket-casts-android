package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.MessageViewColors
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.UpsellView
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity

class BookmarkUpsellViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {
    fun bind() {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                val context = LocalContext.current
                UpsellView(
                    style = MessageViewColors.Default,
                    onClick = {
                        val source = OnboardingUpgradeSource.BOOKMARKS
                        val onboardingFlow = OnboardingFlow.Upsell(source, true)
                        OnboardingLauncher.openOnboardingFlow(context.getActivity(), onboardingFlow)
                    }
                )
            }
        }
    }
}
