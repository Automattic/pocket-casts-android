package au.com.shiftyjelly.pocketcasts.profile

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesFragment.StoriesSource
import au.com.shiftyjelly.pocketcasts.endofyear.views.EndOfYearPromptCard
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralSecondsMinutesHoursDaysOrYears
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.cloud.CloudFilesFragment
import au.com.shiftyjelly.pocketcasts.profile.databinding.FragmentProfileBinding
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.SettingsAdapter
import au.com.shiftyjelly.pocketcasts.settings.SettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsFragment
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeTintedDrawable
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {

    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var settings: Settings
    @Inject lateinit var userManager: UserManager
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private val viewModel: ProfileViewModel by viewModels()

    private var binding: FragmentProfileBinding? = null
    private val sections = listOf(
        SettingsAdapter.Item(LR.string.profile_navigation_stats, R.drawable.ic_stats, StatsFragment::class.java),
        SettingsAdapter.Item(LR.string.profile_navigation_downloads, R.drawable.ic_profile_download, ProfileEpisodeListFragment::class.java),
        SettingsAdapter.Item(LR.string.profile_navigation_files, R.drawable.ic_file, CloudFilesFragment::class.java),
        SettingsAdapter.Item(LR.string.profile_navigation_starred, R.drawable.ic_starred, ProfileEpisodeListFragment::class.java),
        SettingsAdapter.Item(LR.string.profile_navigation_listening_history, R.drawable.ic_listen_history, ProfileEpisodeListFragment::class.java)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.clearFailedRefresh()
    }

    override fun onPause() {
        super.onPause()
        viewModel.isFragmentChangingConfigurations = activity?.isChangingConfigurations ?: false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.btnSettings.setOnClickListener {
            analyticsTracker.track(AnalyticsEvent.PROFILE_SETTINGS_BUTTON_TAPPED)
            (activity as FragmentHostListener).addFragment(SettingsFragment())
        }

        val linearLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = linearLayoutManager
        val divider = DividerItemDecoration(context, linearLayoutManager.orientation)
        ContextCompat.getDrawable(recyclerView.context, UR.drawable.divider)?.let {
            divider.setDrawable(it)
        }
        recyclerView.addItemDecoration(divider)
        recyclerView.adapter = SettingsAdapter(sections) { section ->
            section.fragment?.let { fragmentClass ->
                when (fragmentClass) {
                    StatsFragment::class.java -> {
                        analyticsTracker.track(AnalyticsEvent.STATS_SHOWN)
                        (activity as? FragmentHostListener)?.addFragment(fragmentClass.newInstance())
                    }
                    CloudFilesFragment::class.java -> {
                        analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_SHOWN)
                        (activity as? FragmentHostListener)?.addFragment(fragmentClass.newInstance())
                    }
                    ProfileEpisodeListFragment::class.java -> {
                        val fragment = when (section.title) {
                            LR.string.profile_navigation_downloads -> {
                                analyticsTracker.track(AnalyticsEvent.DOWNLOADS_SHOWN)
                                ProfileEpisodeListFragment.newInstance(ProfileEpisodeListFragment.Mode.Downloaded)
                            }
                            LR.string.profile_navigation_starred -> {
                                analyticsTracker.track(AnalyticsEvent.STARRED_SHOWN)
                                ProfileEpisodeListFragment.newInstance(ProfileEpisodeListFragment.Mode.Starred)
                            }
                            LR.string.profile_navigation_listening_history -> {
                                analyticsTracker.track(AnalyticsEvent.LISTENING_HISTORY_SHOWN)
                                ProfileEpisodeListFragment.newInstance(ProfileEpisodeListFragment.Mode.History)
                            }
                            else -> throw IllegalStateException("Unknown row")
                        }
                        (activity as? FragmentHostListener)?.addFragment(fragment)
                    }
                    else -> Timber.e("Profile section is invalid")
                }
            }

            section.action?.invoke()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val isEligible = viewModel.isEndOfYearStoriesEligible()
                binding.setupEndOfYearPromptCard(isEligible)
            }
        }

        viewModel.podcastCount.observe(viewLifecycleOwner) {
            binding.lblPodcastCount.text = it.toString()
            // check if the stats have changed, causes the stats to change on first sign in
            viewModel.updateState()
        }

        viewModel.daysListenedCount.observe(viewLifecycleOwner) {
            val timeAndUnit = convertSecsToTimeAndUnit(it)
            binding.lblDaysListened.text = timeAndUnit.value
            binding.lblDaysListenedLabel.setText(timeAndUnit.listenedStringId)
        }

        viewModel.daysSavedCount.observe(viewLifecycleOwner) {
            val timeAndUnit = convertSecsToTimeAndUnit(it)
            binding.lblDaysSaved.text = timeAndUnit.value
            binding.lblDaysSavedLabel.setText(timeAndUnit.savedStringId)
        }

        viewModel.signInState.observe(viewLifecycleOwner) { state ->
            binding.userView.signedInState = state

            binding.upgradeLayout.root.isInvisible = settings.getUpgradeClosedProfile() || state.isSignedInAsPlus
            if (binding.upgradeLayout.root.isInvisible) {
                // We need this to get the correct padding below refresh
                binding.upgradeLayout.root.updateLayoutParams<ConstraintLayout.LayoutParams> { height = 16.dpToPx(view.context) }
            }
        }

        with(binding.userView) {
            imgProfilePicture.setOnClickListener { onProfileAccountButtonClicked() }
            btnAccount?.setOnClickListener { onProfileAccountButtonClicked() }
        }

        binding.btnRefresh.setOnClickListener {
            updateRefreshUI(RefreshState.Refreshing)
            podcastManager.refreshPodcasts("profile")
            analyticsTracker.track(AnalyticsEvent.PROFILE_REFRESH_BUTTON_TAPPED)
        }

        val upgradeLayout = binding.upgradeLayout
        upgradeLayout.btnClose.setOnClickListener {
            settings.setUpgradeClosedProfile(true)
            upgradeLayout.root.isVisible = false
        }

        upgradeLayout.lblGetMore.text = getString(LR.string.profile_help_support)
        upgradeLayout.root.setOnClickListener {
            OnboardingLauncher.openOnboardingFlow(
                activity = activity,
                onboardingFlow = OnboardingFlow.PlusUpsell(OnboardingUpgradeSource.PROFILE)
            )
        }

        viewModel.refreshObservable.observe(viewLifecycleOwner) { state ->
            updateRefreshUI(state)
        }

        if (!viewModel.isFragmentChangingConfigurations) {
            analyticsTracker.track(AnalyticsEvent.PROFILE_SHOWN)
        }
    }

    private fun onProfileAccountButtonClicked() {
        analyticsTracker.track(AnalyticsEvent.PROFILE_ACCOUNT_BUTTON_TAPPED)
        if (viewModel.isSignedIn) {
            val fragment = AccountDetailsFragment.newInstance()
            (activity as FragmentHostListener).addFragment(fragment)
        } else {
            OnboardingLauncher.openOnboardingFlow(activity, OnboardingFlow.LoggedOut)
        }
    }

    private fun FragmentProfileBinding.setupEndOfYearPromptCard(isEligible: Boolean) {
        endOfYearPromptCard.setContent {
            if (isEligible) {
                AppTheme(theme.activeTheme) {
                    EndOfYearPromptCard(
                        onClick = {
                            analyticsTracker.track(AnalyticsEvent.END_OF_YEAR_PROFILE_CARD_TAPPED)
                            (activity as? FragmentHostListener)?.showStoriesOrAccount(StoriesSource.PROFILE.value)
                        }
                    )
                }
            }
        }
    }

    private fun updateRefreshUI(state: RefreshState?) {
        val binding = binding ?: return
        val lblRefreshStatus = binding.lblRefreshStatus
        when (state) {
            is RefreshState.Never -> {
                lblRefreshStatus.text = getString(LR.string.profile_refreshed_never)
                lblRefreshStatus.setCompoundDrawables(null, null, null, null)
                lblRefreshStatus.setOnClickListener(null)
            }
            is RefreshState.Success -> {
                updateLastRefreshText(lblRefreshStatus, state.date)
                lblRefreshStatus.setCompoundDrawables(null, null, null, null)
                lblRefreshStatus.setOnClickListener(null)
            }
            is RefreshState.Refreshing -> {
                lblRefreshStatus.text = getString(LR.string.profile_refreshing)
                lblRefreshStatus.setCompoundDrawables(null, null, null, null)
                lblRefreshStatus.setOnClickListener(null)
            }
            is RefreshState.Failed -> {
                lblRefreshStatus.text = getString(LR.string.profile_refresh_failed)
                context?.let { context ->
                    val errorDrawable = context.getThemeTintedDrawable(IR.drawable.ic_alert_small, UR.attr.primary_icon_02)
                    lblRefreshStatus.setCompoundDrawablesWithIntrinsicBounds(errorDrawable, null, null, null)
                    lblRefreshStatus.compoundDrawablePadding = 8.dpToPx(context)
                    TextViewCompat.setCompoundDrawableTintList(
                        lblRefreshStatus,
                        ColorStateList.valueOf(
                            context.getThemeColor(
                                UR.attr.secondary_icon_01
                            )
                        )
                    )
                    lblRefreshStatus.setOnClickListener {
                        AlertDialog.Builder(context)
                            .setTitle(LR.string.profile_refresh_error)
                            .setMessage(state.error)
                            .setPositiveButton(LR.string.ok, null)
                            .show()
                    }
                }
            }
            else -> {
                lblRefreshStatus.setText(LR.string.profile_refresh_status_unknown)
                lblRefreshStatus.setCompoundDrawables(null, null, null, null)
                lblRefreshStatus.setOnClickListener(null)
            }
        }
    }

    private fun updateLastRefreshText(lblRefreshStatus: TextView, lastRefresh: Date) {
        val time = Date().time - lastRefresh.time
        val timeAmount = resources.getStringPluralSecondsMinutesHoursDaysOrYears(time)
        lblRefreshStatus.text = getString(LR.string.profile_last_refresh, timeAmount)
    }

    override fun onBackPressed(): Boolean {
        viewModel.updateState()
        return super.onBackPressed()
    }

    private fun convertSecsToTimeAndUnit(seconds: Long): TimeAndUnit {
        val days = seconds / 86400
        val hours = seconds / 3600
        val mins = seconds / 60
        val secs = seconds

        if (days > 0) {
            return TimeAndUnit(
                value = days.toString(),
                savedStringId = if (days == 1L) LR.string.profile_stats_day_saved else LR.string.profile_stats_days_saved,
                listenedStringId = if (days == 1L) LR.string.profile_stats_day_listened else LR.string.profile_stats_days_listened
            )
        }
        if (hours > 0) {
            return TimeAndUnit(
                value = hours.toString(),
                savedStringId = if (hours == 1L) LR.string.profile_stats_hour_saved else LR.string.profile_stats_hours_saved,
                listenedStringId = if (hours == 1L) LR.string.profile_stats_hour_listened else LR.string.profile_stats_hours_listened
            )
        }
        if (mins > 0 && days < 1) {
            return TimeAndUnit(
                value = mins.toString(),
                savedStringId = if (mins == 1L) LR.string.profile_stats_minute_saved else LR.string.profile_stats_minutes_saved,
                listenedStringId = if (mins == 1L) LR.string.profile_stats_minute_listened else LR.string.profile_stats_minutes_listened
            )
        }
        return TimeAndUnit(
            value = secs.toString(),
            savedStringId = if (secs == 1L) LR.string.profile_stats_second_saved else LR.string.profile_stats_seconds_saved,
            listenedStringId = if (secs == 1L) LR.string.profile_stats_second_listened else LR.string.profile_stats_seconds_listened
        )
    }

    data class TimeAndUnit(val value: String, @StringRes val savedStringId: Int, @StringRes val listenedStringId: Int)
}
