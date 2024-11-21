package au.com.shiftyjelly.pocketcasts.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesActivity.StoriesSource
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarksContainerFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.cloud.CloudFilesFragment
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsGuestPassFragment
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsGuestPassFragment.ReferralsPageType
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsViewModel
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.HelpFragment
import au.com.shiftyjelly.pocketcasts.settings.SettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {

    @Inject
    lateinit var podcastManager: PodcastManager

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    private val profileViewModel by viewModels<ProfileViewModel>()
    private val referralsViewModel by viewModels<ReferralsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val state = ProfilePageState(
            isSendReferralsEnabled = FeatureFlag.isEnabled(Feature.REFERRALS_SEND),
            isPlaybackEnabled = profileViewModel.isEndOfYearStoriesEligible.collectAsState().value,
            isClaimReferralsEnabled = FeatureFlag.isEnabled(Feature.REFERRALS_CLAIM),
            isUpgradeBannerVisible = profileViewModel.showUpgradeBanner.collectAsState(false).value,
            miniPlayerPadding = settings.bottomInset.collectAsState(0).value.pxToDp(requireContext()).dp,
            headerState = profileViewModel.profileHeaderState.collectAsState().value,
            statsState = profileViewModel.profileStatsState.collectAsState().value,
            referralsState = referralsViewModel.state.collectAsState().value,
            refreshState = profileViewModel.refreshState.collectAsState().value,
        )

        ProfilePage(
            state = state,
            themeType = theme.activeTheme,
            onSendReferralsClick = {
                referralsViewModel.onIconClick()
                val fragment = ReferralsGuestPassFragment.newInstance(ReferralsPageType.Send)
                (requireActivity() as FragmentHostListener).showBottomSheet(fragment)
            },
            onReferralsTooltipClick = {
                referralsViewModel.onTooltipClick()
            },
            onReferralsTooltipShown = {
                referralsViewModel.onTooltipShown()
            },
            onSettingsClick = {
                analyticsTracker.track(AnalyticsEvent.PROFILE_SETTINGS_BUTTON_TAPPED)
                (requireActivity() as FragmentHostListener).addFragment(SettingsFragment())
            },
            onHeaderClick = {
                analyticsTracker.track(AnalyticsEvent.PROFILE_ACCOUNT_BUTTON_TAPPED)
                if (profileViewModel.isSignedIn) {
                    val fragment = AccountDetailsFragment.newInstance()
                    (activity as FragmentHostListener).addFragment(fragment)
                } else {
                    OnboardingLauncher.openOnboardingFlow(activity, OnboardingFlow.LoggedOut)
                }
            },
            onPlaybackClick = {
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
            onClaimReferralsClick = {
                val fragment = ReferralsGuestPassFragment.newInstance(ReferralsPageType.Claim)
                (requireActivity() as FragmentHostListener).showBottomSheet(fragment)
            },
            onHideReferralsCardClick = {
                referralsViewModel.onHideBannerClick()
            },
            onReferralsCardShown = {
                referralsViewModel.onBannerShown()
            },
            onShowReferralsSheet = {
                val activity = requireActivity()
                activity.supportFragmentManager.findFragmentByTag(ReferralsGuestPassFragment::class.java.name)?.let {
                    (activity as FragmentHostListener).showBottomSheet(it)
                }
            },
            onSectionClick = { section ->
                goToSection(section)
            },
            onRefreshClick = {
                podcastManager.refreshPodcasts("profile")
                analyticsTracker.track(AnalyticsEvent.PROFILE_REFRESH_BUTTON_TAPPED)
            },
            onUpgradeProfileClick = {
                OnboardingLauncher.openOnboardingFlow(
                    activity = requireActivity(),
                    onboardingFlow = OnboardingFlow.Upsell(OnboardingUpgradeSource.PROFILE),
                )
            },
            onCloseUpgradeProfileClick = {
                profileViewModel.closeUpgradeProfile()
            },
            modifier = Modifier.fillMaxSize(),
        )
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.clearFailedRefresh()
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
        profileViewModel.refreshStats()
        return super.onBackPressed()
    }
}
