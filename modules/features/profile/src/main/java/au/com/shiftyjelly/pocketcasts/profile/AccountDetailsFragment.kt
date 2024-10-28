package au.com.shiftyjelly.pocketcasts.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.account.ChangeEmailFragment
import au.com.shiftyjelly.pocketcasts.account.ChangePwdFragment
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ProfileUpgradeBannerViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.BuildConfig.GRAVATAR_APP_ID
import au.com.shiftyjelly.pocketcasts.profile.champion.PocketCastsChampionBottomSheetDialog
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.gravatar.quickeditor.GravatarQuickEditor
import com.gravatar.quickeditor.ui.editor.AuthenticationMethod
import com.gravatar.quickeditor.ui.editor.AvatarPickerContentLayout
import com.gravatar.quickeditor.ui.editor.GravatarQuickEditorParams
import com.gravatar.quickeditor.ui.oauth.OAuthParams
import com.gravatar.types.Email
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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

    @Inject lateinit var analyticsTracker: AnalyticsTracker

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

    @Inject lateinit var syncManager: SyncManager

    private val accountViewModel by viewModels<AccountDetailsViewModel>()
    private val upgradeBannerViewModel by viewModels<ProfileUpgradeBannerViewModel>()

    private val gravatarQuickEditorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        accountViewModel.gravatarUpdated()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val upgradeBannerState = remember {
            combine(
                accountViewModel.showUpgradeBanner,
                upgradeBannerViewModel.state,
            ) { showBanner, state -> state.takeIf { showBanner } }
        }

        val state = AccountDetailsPageState(
            isAutomotive = remember { Util.isAutomotive(requireContext()) },
            miniPlayerPadding = accountViewModel.miniPlayerInset.collectAsState().value.pxToDp(requireContext()).dp,
            headerState = accountViewModel.headerState.collectAsState().value,
            upgradeBannerState = upgradeBannerState.collectAsState(null).value,
            sectionsState = accountViewModel.sectionsState.collectAsState().value,
        )
        AccountDetailsPage(
            state = state,
            theme = theme.activeTheme,
            onNavigateBack = {
                @Suppress("DEPRECATION")
                requireActivity().onBackPressed()
            },
            onClickHeader = {
                if (state.headerState.subscription.isChampion) {
                    PocketCastsChampionBottomSheetDialog().show(childFragmentManager, "pocket_casts_champion_dialog")
                }
            },
            onClickUpgradeBanner = {
                analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_UPGRADE_BUTTON_TAPPED)
                val source = OnboardingUpgradeSource.PROFILE
                val onboardingFlow = OnboardingFlow.PlusAccountUpgrade(source)
                OnboardingLauncher.openOnboardingFlow(activity, onboardingFlow)
            },
            onFeatureCardChanged = { featureCard ->
                upgradeBannerViewModel.onFeatureCardChanged(featureCard)
            },
            onChangeAvatar = { email ->
                openGravatarQuickEditor(email)
            },
            onChangeEmail = {
                (requireActivity() as FragmentHostListener).addFragment(ChangeEmailFragment.newInstance())
            },
            onChangePassword = {
                (requireActivity() as FragmentHostListener).addFragment(ChangePwdFragment.newInstance())
            },
            onUpgradeToPatron = {
                val source = OnboardingUpgradeSource.ACCOUNT_DETAILS
                val onboardingFlow = OnboardingFlow.PatronAccountUpgrade(source)
                OnboardingLauncher.openOnboardingFlow(activity, onboardingFlow)
            },
            onCancelSubscription = {
                analyticsTracker.track(AnalyticsEvent.ACCOUNT_DETAILS_CANCEL_TAPPED)
                CancelConfirmationFragment
                    .newInstance()
                    .show(childFragmentManager, "cancel_subscription_confirmation_dialog")
            },
            onChangeNewsletterSubscription = { isChecked ->
                accountViewModel.updateNewsletter(isChecked)
            },
            onShowPrivacyPolicy = {
                analyticsTracker.track(AnalyticsEvent.ACCOUNT_DETAILS_SHOW_PRIVACY_POLICY)
                requireActivity().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Settings.INFO_PRIVACY_URL)))
            },
            onShowTermsOfUse = {
                analyticsTracker.track(AnalyticsEvent.ACCOUNT_DETAILS_SHOW_TOS)
                requireActivity().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Settings.INFO_TOS_URL)))
            },
            onSignOut = { signOut() },
            onDeleteAccount = { deleteAccount() },
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                accountViewModel.deleteAccountState.collect {
                    updateDeleteAccountState(it)
                }
            }
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
                accountViewModel.clearDeleteAccountState()
                performSignOut()
            }
            is DeleteAccountState.Failure -> {
                accountViewModel.clearDeleteAccountState()
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
        accountViewModel.deleteAccount()
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
        userManager.signOutAndClearData(
            playbackManager = playbackManager,
            upNextQueue = upNextQueue,
            playlistManager = playlistManager,
            folderManager = folderManager,
            searchHistoryManager = searchHistoryManager,
            episodeManager = episodeManager,
            wasInitiatedByUser = true,
        )
        activity?.finish()
    }

    private fun openGravatarQuickEditor(email: String) {
        analyticsTracker.track(AnalyticsEvent.ACCOUNT_DETAILS_CHANGE_AVATAR)
        if (FeatureFlag.isEnabled(Feature.GRAVATAR_NATIVE_QUICK_EDITOR)) {
            GravatarQuickEditor.show(
                activity = requireActivity(),
                gravatarQuickEditorParams = GravatarQuickEditorParams {
                    this.email = Email(email)
                    avatarPickerContentLayout = AvatarPickerContentLayout.Horizontal
                },
                authenticationMethod = AuthenticationMethod.OAuth(
                    OAuthParams {
                        clientId = GRAVATAR_APP_ID
                        redirectUri = Gravatar.GRAVATAR_QE_REDIRECT_URL
                    },
                ),
                onAvatarSelected = {
                    accountViewModel.gravatarUpdated()
                },
                onDismiss = {},
            )
        } else {
            gravatarQuickEditorLauncher.launch(Intent(Intent.ACTION_VIEW, Uri.parse(Gravatar.getGravatarChangeAvatarUrl(email))))
        }
    }

    private fun performSignOut() {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "User requested to sign out")
        userManager.signOut(playbackManager, wasInitiatedByUser = true)
        @Suppress("DEPRECATION")
        activity?.onBackPressed()
    }
}
