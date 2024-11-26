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
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesActivity.StoriesSource
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarksContainerFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListFragment
import au.com.shiftyjelly.pocketcasts.profile.cloud.CloudFilesFragment
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsGuestPassFragment
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsGuestPassFragment.ReferralsPageType
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsViewModel
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

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {
    private val profileViewModel by viewModels<ProfileViewModel>()
    private val referralsViewModel by viewModels<ReferralsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        CallOnce {
            profileViewModel.onScreenShown()
        }
        val state = ProfilePageState(
            isSendReferralsEnabled = FeatureFlag.isEnabled(Feature.REFERRALS_SEND),
            isPlaybackEnabled = profileViewModel.isPlaybackAvailable.collectAsState().value,
            isClaimReferralsEnabled = FeatureFlag.isEnabled(Feature.REFERRALS_CLAIM),
            isUpgradeBannerVisible = profileViewModel.showUpgradeBanner.collectAsState(false).value,
            miniPlayerPadding = profileViewModel.miniPlayerInset.collectAsState().value.pxToDp(requireContext()).dp,
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
                fragmentHostListener.showBottomSheet(ReferralsGuestPassFragment.newInstance(ReferralsPageType.Send))
            },
            onReferralsTooltipClick = {
                referralsViewModel.onTooltipClick()
            },
            onReferralsTooltipShown = {
                referralsViewModel.onTooltipShown()
            },
            onSettingsClick = {
                profileViewModel.onSettingsClick()
                fragmentHostListener.addFragment(SettingsFragment())
            },
            onHeaderClick = {
                profileViewModel.onHeaderClick()
                if (profileViewModel.isSignedIn) {
                    fragmentHostListener.addFragment(AccountDetailsFragment.newInstance())
                } else {
                    OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.LoggedOut)
                }
            },
            onPlaybackClick = {
                profileViewModel.onPlaybackClick()
                (activity as? FragmentHostListener)?.showStoriesOrAccount(StoriesSource.PROFILE.value)
            },
            onClaimReferralsClick = {
                fragmentHostListener.showBottomSheet(ReferralsGuestPassFragment.newInstance(ReferralsPageType.Claim))
            },
            onHideReferralsCardClick = {
                referralsViewModel.onHideBannerClick()
            },
            onReferralsCardShown = {
                referralsViewModel.onBannerShown()
            },
            onShowReferralsSheet = {
                requireActivity().supportFragmentManager
                    .findFragmentByTag(ReferralsGuestPassFragment::class.java.name)
                    ?.let { fragmentHostListener.showBottomSheet(it) }
            },
            onSectionClick = { section ->
                goToSection(section)
            },
            onRefreshClick = {
                profileViewModel.refreshProfile()
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

    private val fragmentHostListener get() = requireActivity() as FragmentHostListener

    private fun goToSection(section: ProfileSection) {
        profileViewModel.onSectionClick(section)
        val fragment = when (section) {
            ProfileSection.Stats -> StatsFragment()
            ProfileSection.Downloads -> ProfileEpisodeListFragment.newInstance(ProfileEpisodeListFragment.Mode.Downloaded)
            ProfileSection.CloudFiles -> CloudFilesFragment()
            ProfileSection.Starred -> ProfileEpisodeListFragment.newInstance(ProfileEpisodeListFragment.Mode.Starred)
            ProfileSection.Bookmarks -> BookmarksContainerFragment.newInstance(sourceView = SourceView.PROFILE)
            ProfileSection.ListeningHistory -> ProfileEpisodeListFragment.newInstance(ProfileEpisodeListFragment.Mode.History)
            ProfileSection.Help -> HelpFragment()
        }
        fragmentHostListener.addFragment(fragment)
    }

    override fun onBackPressed(): Boolean {
        profileViewModel.refreshStats()
        return super.onBackPressed()
    }
}
