package au.com.shiftyjelly.pocketcasts.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.account.ChangeEmailFragment
import au.com.shiftyjelly.pocketcasts.account.ChangePwdFragment
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.ProfileUpgradeBanner
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.databinding.FragmentAccountDetailsBinding
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.days
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Date
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.cartheme.R as CR
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class AccountDetailsFragment : BaseFragment() {
    companion object {
        fun newInstance(): AccountDetailsFragment {
            return AccountDetailsFragment()
        }
    }

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var folderManager: FolderManager
    @Inject lateinit var playlistManager: PlaylistManager
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var searchHistoryManager: SearchHistoryManager
    @Inject lateinit var settings: Settings
    @Inject lateinit var upNextQueue: UpNextQueue
    @Inject lateinit var userEpisodeManager: UserEpisodeManager
    @Inject lateinit var userManager: UserManager

    private val viewModel: AccountDetailsViewModel by viewModels()
    private var binding: FragmentAccountDetailsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAccountDetailsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        val toolbar = binding.toolbar
        toolbar?.setTitle(LR.string.profile_pocket_casts_account)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        toolbar?.setOnLongClickListener {
            theme.toggleDarkLightThemeActivity(requireActivity() as AppCompatActivity)
            true
        }

        viewModel.signInState.observe(viewLifecycleOwner) { signInState ->
            binding.userView.signedInState = signInState
        }

        viewModel.viewState.observe(viewLifecycleOwner) { (signInState, subscription, deleteAccountState) ->
            var giftExpiring = false
            (signInState as? SignInState.SignedIn)?.subscriptionStatus?.let { status ->
                val plusStatus = status as? SubscriptionStatus.Plus ?: return@let
                val daysLessThan30 = plusStatus.expiry.before(Date(Date().time + 30.days()))
                giftExpiring = (daysLessThan30 && !status.autoRenew)
            }

            binding.cancelViewGroup?.isVisible = signInState.isSignedInAsPlusPaid
            binding.btnCancelSub?.isVisible = signInState.isSignedInAsPlusPaid

            binding.userUpgradeComposeView?.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AppTheme(theme.activeTheme) {
                        if (subscription != null && (signInState.isSignedInAsFree || giftExpiring)) {
                            ProfileUpgradeBanner(
                                onClick = {
                                    val source = OnboardingUpgradeSource.PROFILE
                                    val onboardingFlow = OnboardingFlow.PlusAccountUpgrade(source)
                                    OnboardingLauncher.openOnboardingFlow(activity, onboardingFlow)
                                }
                            )
                        }
                    }
                }
            }

            updateDeleteAccountState(deleteAccountState)
        }

        viewModel.accountStartDate.observe(viewLifecycleOwner) { accountStartDate ->
            binding.userView.accountStartDate = accountStartDate
        }

        viewModel.marketingOptInState.observe(viewLifecycleOwner) { marketingOptIn ->
            binding.swtNewsletter?.isChecked = marketingOptIn
            binding.swtNewsletter?.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateNewsletter(isChecked)
            }
        }

        binding.btnChangeEmail?.setOnClickListener {
            val fragment = ChangeEmailFragment.newInstance()
            (activity as FragmentHostListener).addFragment(fragment)
        }

        binding.btnChangePwd?.setOnClickListener {
            val fragment = ChangePwdFragment.newInstance()
            (this.activity as FragmentHostListener).addFragment(fragment)
        }

        binding.btnCancelSub?.setOnClickListener {
            analyticsTracker.track(AnalyticsEvent.ACCOUNT_DETAILS_CANCEL_TAPPED)
            CancelConfirmationFragment.newInstance()
                .show(childFragmentManager, "cancel_subscription_confirmation_dialog")
        }

        binding.btnSignOut.setOnClickListener {
            signOut()
        }

        binding.btnDeleteAccount?.setOnClickListener {
            deleteAccount()
        }

        binding.btnNewsletter?.setOnClickListener {
            binding.swtNewsletter?.let {
                it.isChecked = !it.isChecked
            }
        }

        binding.btnPrivacyPolicy?.setOnClickListener {
            analyticsTracker.track(AnalyticsEvent.ACCOUNT_DETAILS_SHOW_PRIVACY_POLICY)
            context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Settings.INFO_PRIVACY_URL)))
        }

        binding.btnTermsOfUse?.setOnClickListener {
            analyticsTracker.track(AnalyticsEvent.ACCOUNT_DETAILS_SHOW_TOS)
            context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Settings.INFO_TOS_URL)))
        }
    }

    private fun signOut() {
        if (Util.isAutomotive(requireContext())) {
            signOutAutomotive()
            return
        }

        val body = getString(LR.string.profile_sign_out_confirm)
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Danger(getString(LR.string.profile_sign_out)))
            .setTitle(getString(LR.string.profile_sign_out))
            .setSummary(body)
            .setOnConfirm { performSignOut() }
            .setIconId(IR.drawable.ic_signout)
            .setIconTint(UR.attr.support_05)
            .show(childFragmentManager, "signout_warning")
    }

    private fun deleteAccount() {
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Danger(getString(LR.string.profile_account_delete)))
            .setTitle(getString(LR.string.profile_delete_account_title))
            .setSummary(getString(LR.string.profile_delete_account_question))
            .setOnConfirm { deleteAccountPermanent() }
            .setIconId(VR.drawable.ic_delete)
            .setIconTint(UR.attr.support_05)
            .show(childFragmentManager, "deleteaccount_warning")
    }

    private fun deleteAccountPermanent() {
        ConfirmationDialog()
            .setButtonType(ConfirmationDialog.ButtonType.Danger(getString(LR.string.profile_account_delete_yes)))
            .setTitle(getString(LR.string.profile_delete_account_title))
            .setSummary(getString(LR.string.profile_delete_account_permanent_question))
            .setOnConfirm { performDeleteAccount() }
            .setIconId(IR.drawable.ic_failedwarning)
            .setIconTint(UR.attr.support_05)
            .show(childFragmentManager, "deleteaccount_permanent_warning")
    }

    private fun updateDeleteAccountState(state: DeleteAccountState) {
        when (state) {
            is DeleteAccountState.Success -> {
                viewModel.clearDeleteAccountState()
                FirebaseAnalyticsTracker.accountDeleted()
                performSignOut()
            }
            is DeleteAccountState.Failure -> {
                viewModel.clearDeleteAccountState()
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(LR.string.profile_delete_account_failed_title))
                    .setMessage(state.message ?: getString(LR.string.profile_delete_account_failed_message))
                    .setPositiveButton(getString(LR.string.ok)) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
            is DeleteAccountState.Empty -> {}
        }
    }

    private fun performDeleteAccount() {
        viewModel.deleteAccount()
        Toast.makeText(requireContext(), LR.string.profile_deleting_account, Toast.LENGTH_LONG).show()
    }

    private fun signOutAutomotive() {
        val context = context ?: return
        val themedContext = if (Util.isAutomotive(context)) ContextThemeWrapper(context, CR.style.Theme_Car_NoActionBar) else context
        val builder = AlertDialog.Builder(themedContext)
        builder.setTitle(getString(LR.string.profile_sign_out))
            .setMessage(getString(LR.string.profile_sign_out_confirm))
            .setPositiveButton(getString(LR.string.profile_sign_out)) { _, _ -> clearDataAlert() }
            .setNegativeButton(getString(LR.string.cancel), null)
            .show()
    }

    private fun clearDataAlert() {
        val context = context ?: return
        val themedContext = if (Util.isAutomotive(context)) ContextThemeWrapper(context, CR.style.Theme_Car_NoActionBar) else context
        val builder = AlertDialog.Builder(themedContext)
        builder.setTitle(getString(LR.string.profile_clear_data_question))
            .setMessage(getString(LR.string.profile_clear_data_would_you_also_like_question))
            .setPositiveButton(getString(LR.string.profile_just_sign_out)) { _, _ -> performSignOut() }
            .setNegativeButton(getString(LR.string.profile_clear_data)) { _, _ ->
                signOutAndClearData()
            }
            .show()
    }

    private fun signOutAndClearData() {
        // Sign out first to make sure no data changes get synced
        userManager.signOut(playbackManager, wasInitiatedByUser = true)

        // Block while clearing data so that we don't return to the app until the users data has been cleared
        runBlocking(Dispatchers.IO) {
            upNextQueue.removeAllIncludingChanges()

            playlistManager.resetDb()
            folderManager.deleteAll()
            searchHistoryManager.clearAll()

            podcastManager.deleteAllPodcasts()

            userEpisodeManager.findUserEpisodes().forEach {
                userEpisodeManager.delete(it, playbackManager)
            }
            episodeManager.deleteAll()
        }

        activity?.finish()
    }

    private fun performSignOut() {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "User requested to sign out")
        userManager.signOut(playbackManager, wasInitiatedByUser = true)
        @Suppress("DEPRECATION")
        activity?.onBackPressed()
    }
}
