package au.com.shiftyjelly.pocketcasts.profile

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesActivity.StoriesSource
import au.com.shiftyjelly.pocketcasts.endofyear.ui.EndOfYearPromptCard
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarksContainerFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.cloud.CloudFilesFragment
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsClaimGuestPassBannerCard
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsIconWithTooltip
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.HelpFragment
import au.com.shiftyjelly.pocketcasts.settings.SettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {

    @Inject
    lateinit var podcastManager: PodcastManager

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    private val viewModel: ProfileViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        viewModel.clearFailedRefresh()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        AppTheme(theme.activeTheme) {
            CallOnce {
                analyticsTracker.track(AnalyticsEvent.PROFILE_SHOWN)
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(MaterialTheme.theme.colors.primaryUi03)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Toolbar()
                HeaderWithStats()
                EndOfYearBanner()
                ReferralsClaimGuestPassBannerCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Sections()
                Refresh()
                UpgradeProfile()
                MiniPlayerPadding()
            }
        }
    }

    @Composable
    private fun Toolbar() {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
        ) {
            if (FeatureFlag.isEnabled(Feature.REFERRALS_SEND)) {
                ReferralsIconWithTooltip()
            }
            if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                Spacer(
                    modifier = Modifier.weight(1f),
                )
            }
            IconButton(
                onClick = {
                    analyticsTracker.track(AnalyticsEvent.PROFILE_SETTINGS_BUTTON_TAPPED)
                    (requireActivity() as FragmentHostListener).addFragment(SettingsFragment())
                },
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_profile_settings),
                    contentDescription = stringResource(LR.string.settings),
                    tint = MaterialTheme.theme.colors.primaryIcon01,
                )
            }
        }
    }

    @Composable
    private fun ColumnScope.HeaderWithStats() {
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            Header(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Stats(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Header(
                    modifier = Modifier.weight(1f),
                )
                Stats(
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    @Composable
    private fun Header(
        modifier: Modifier = Modifier,
    ) {
        val headerState by viewModel.profileHeaderState
            .collectAsStateWithLifecycle(
                ProfileHeaderState(
                    imageUrl = null,
                    subscriptionTier = SubscriptionTier.NONE,
                    email = null,
                    expiresIn = null,
                ),
            )

        ProfileHeader(
            state = headerState,
            onClick = {
                analyticsTracker.track(AnalyticsEvent.PROFILE_ACCOUNT_BUTTON_TAPPED)
                if (viewModel.isSignedIn) {
                    val fragment = AccountDetailsFragment.newInstance()
                    (activity as FragmentHostListener).addFragment(fragment)
                } else {
                    OnboardingLauncher.openOnboardingFlow(activity, OnboardingFlow.LoggedOut)
                }
            },
            modifier = modifier,
        )
    }

    @Composable
    private fun Stats(
        modifier: Modifier = Modifier,
    ) {
        val headerState by viewModel.profileStatsState
            .collectAsStateWithLifecycle(
                ProfileStatsState(
                    podcastsCount = 0,
                    listenedDuration = Duration.ZERO,
                    savedDuration = Duration.ZERO,
                ),
            )

        ProfileStats(
            state = headerState,
            modifier = modifier,
        )
    }

    @Composable
    private fun ColumnScope.EndOfYearBanner() {
        val isEligibleForEoY by viewModel.isEndOfYearStoriesEligible.collectAsState()

        if (isEligibleForEoY) {
            EndOfYearPromptCard(
                onClick = {
                    analyticsTracker.track(
                        AnalyticsEvent.END_OF_YEAR_PROFILE_CARD_TAPPED,
                        mapOf("year" to EndOfYearManager.YEAR_TO_SYNC.value),
                    )
                    // once stories prompt card is tapped, we don't want to show stories launch modal if not already shown
                    if (settings.getEndOfYearShowModal()) {
                        settings.setEndOfYearShowModal(false)
                    }
                    (activity as? FragmentHostListener)?.showStoriesOrAccount(StoriesSource.PROFILE.value)
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }

    @Composable
    private fun Sections() {
        ProfileSections(
            sections = ProfileSection.entries,
            onClick = ::goToSection,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    @Composable
    private fun Refresh() {
        val viewModelState by viewModel.refreshState.collectAsState(null)
        var state by remember(viewModelState) { mutableStateOf(viewModelState) }

        RefreshSection(
            refreshState = state,
            onClick = {
                state = RefreshState.Refreshing
                podcastManager.refreshPodcasts("profile")
                analyticsTracker.track(AnalyticsEvent.PROFILE_REFRESH_BUTTON_TAPPED)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    @Composable
    private fun ColumnScope.UpgradeProfile() {
        val showUpgradeBanner by viewModel.showUpgradeBanner.collectAsState(false)
        ProfileUpgradeSection(
            isVisible = showUpgradeBanner,
            contentPadding = PaddingValues(horizontal = 64.dp, vertical = 16.dp),
            onClick = {
                OnboardingLauncher.openOnboardingFlow(
                    activity = activity,
                    onboardingFlow = OnboardingFlow.Upsell(OnboardingUpgradeSource.PROFILE),
                )
            },
            onCloseClick = {
                viewModel.closeUpgradeProfile()
            },
            modifier = Modifier
                .background(MaterialTheme.colors.background)
                .fillMaxWidth(),
        )
    }

    @Composable
    private fun MiniPlayerPadding() {
        val bottomPadding by settings.bottomInset.collectAsState(0)
        val showUpgradeBanner by viewModel.showUpgradeBanner.collectAsState(false)
        Box(
            modifier = Modifier
                .background(if (showUpgradeBanner) MaterialTheme.colors.background else Color.Transparent)
                .fillMaxWidth()
                .height(LocalDensity.current.run { bottomPadding.toDp() }),
        )
    }

    private fun goToSection(section: ProfileSection) {
        val fragment = when (section) {
            ProfileSection.Stats -> {
                analyticsTracker.track(AnalyticsEvent.STATS_SHOWN)
                StatsFragment()
            }

            ProfileSection.Downloads -> {
                analyticsTracker.track(AnalyticsEvent.DOWNLOADS_SHOWN)
                ProfileEpisodeListFragment.newInstance(ProfileEpisodeListFragment.Mode.Downloaded)
            }

            ProfileSection.CloudFiles -> {
                analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_SHOWN)
                CloudFilesFragment()
            }

            ProfileSection.Starred -> {
                analyticsTracker.track(AnalyticsEvent.STARRED_SHOWN)
                ProfileEpisodeListFragment.newInstance(ProfileEpisodeListFragment.Mode.Starred)
            }

            ProfileSection.Bookmarks -> {
                analyticsTracker.track(AnalyticsEvent.PROFILE_BOOKMARKS_SHOWN)
                BookmarksContainerFragment.newInstance(sourceView = SourceView.PROFILE)
            }

            ProfileSection.ListeningHistory -> {
                analyticsTracker.track(AnalyticsEvent.LISTENING_HISTORY_SHOWN)
                ProfileEpisodeListFragment.newInstance(ProfileEpisodeListFragment.Mode.History)
            }

            ProfileSection.Help -> {
                analyticsTracker.track(AnalyticsEvent.SETTINGS_HELP_SHOWN)
                HelpFragment()
            }
        }
        (requireActivity() as FragmentHostListener).addFragment(fragment)
    }

    override fun onBackPressed(): Boolean {
        viewModel.refreshStats()
        return super.onBackPressed()
    }
}
