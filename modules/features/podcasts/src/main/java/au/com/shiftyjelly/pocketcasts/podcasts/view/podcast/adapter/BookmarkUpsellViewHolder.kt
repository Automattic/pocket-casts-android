package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.MessageViewColors
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.UpsellView
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity

class BookmarkUpsellViewHolder(
    private val composeView: ComposeView,
    private val sourceView: SourceView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {
    fun bind() {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                val context = LocalContext.current
                UpsellView(
                    style = MessageViewColors.Default,
                    sourceView = sourceView,
                    onClick = {
                        val source = OnboardingUpgradeSource.BOOKMARKS
                        val onboardingFlow = OnboardingFlow.Upsell(
                            source = source,
                            showPatronOnly = Feature.BOOKMARKS_ENABLED.tier == FeatureTier.Patron,
                        )
                        OnboardingLauncher.openOnboardingFlow(context.getActivity(), onboardingFlow)
                    }
                )
            }
        }
    }
}
