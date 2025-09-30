package au.com.shiftyjelly.pocketcasts

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.UserAvatarConfig
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.databinding.FragmentAutomotiveSettingsBinding
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.profile.AccountDetailsFragment
import au.com.shiftyjelly.pocketcasts.profile.ProfileHeaderConfig
import au.com.shiftyjelly.pocketcasts.profile.ProfileHeaderState
import au.com.shiftyjelly.pocketcasts.profile.VerticalProfileHeader
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import au.com.shiftyjelly.pocketcasts.utils.extensions.windowed
import au.com.shiftyjelly.pocketcasts.utils.toDurationFromNow
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import au.com.shiftyjelly.pocketcasts.cartheme.R as CR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AutomotiveSettingsFragment : BaseFragment() {
    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var podcastManager: PodcastManager

    @Inject
    lateinit var episodeManager: EpisodeManager

    @Inject
    lateinit var folderManager: FolderManager

    @Inject
    lateinit var searchHistoryManager: SearchHistoryManager

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var upNextQueue: UpNextQueue

    private lateinit var binding: FragmentAutomotiveSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAutomotiveSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setupuserView()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userManager.getSignInState().asFlow().windowed(2)
                    .filter { (previous, current) -> !previous.isSignedIn && current.isSignedIn }
                    .collect {
                        // We have to close after signing in to meet Google UX requirements
                        requireActivity().finish()
                    }
            }
        }
    }

    private fun FragmentAutomotiveSettingsBinding.setupuserView() {
        val config = ProfileHeaderConfig(
            avatarConfig = UserAvatarConfig(
                imageSize = 144.dp,
                strokeWidth = 4.dp,
                imageContentPadding = 4.dp,
                badgeFontSize = 18.sp,
                badgeIconSize = 18.dp,
                badgeContentPadding = 6.dp,
            ),
            infoFontScale = 1.6f,
            spacingScale = 2f,
        )
        val headerStateFlow = userManager.getSignInState().asFlow().map { state ->
            when (state) {
                is SignInState.SignedIn -> ProfileHeaderState(
                    imageUrl = Gravatar.getUrl(state.email),
                    subscriptionTier = state.subscription?.tier,
                    email = state.email,
                    expiresIn = state.subscription?.expiryDate?.toDurationFromNow(),
                )

                is SignInState.SignedOut -> ProfileHeaderState(
                    imageUrl = null,
                    subscriptionTier = null,
                    email = null,
                    expiresIn = null,
                )
            }
        }
        val emptyState = ProfileHeaderState(
            imageUrl = null,
            subscriptionTier = null,
            email = null,
            expiresIn = null,
        )
        userView.setContentWithViewCompositionStrategy {
            val state by headerStateFlow.collectAsState(emptyState)
            AppTheme(theme.activeTheme) {
                VerticalProfileHeader(
                    state = state,
                    onClick = { onProfileAccountButtonClicked(loggedIn = state.email != null) },
                    config = config,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    private fun onProfileAccountButtonClicked(loggedIn: Boolean) {
        if (loggedIn) {
            openSettingsFragment()
        } else {
            launch {
                signIn()
            }
        }
    }

    private suspend fun signIn() {
        // ask the user if they want to clear their data before they sign in
        if (isAccountUsed()) {
            val context = context ?: return
            val themedContext = ContextThemeWrapper(context, CR.style.Theme_Car_NoActionBar)
            val builder = AlertDialog.Builder(themedContext)
            builder.setTitle(getString(LR.string.profile_clear_data_question))
                .setMessage(getString(LR.string.profile_clear_data_would_you_also_like_question))
                .setPositiveButton(getString(LR.string.sign_in)) { _, _ -> openSignInActivity() }
                .setNegativeButton(getString(LR.string.profile_clear_data)) { _, _ ->
                    userManager.signOutAndClearData(
                        playbackManager = playbackManager,
                        upNextQueue = upNextQueue,
                        folderManager = folderManager,
                        searchHistoryManager = searchHistoryManager,
                        episodeManager = episodeManager,
                        wasInitiatedByUser = false,
                    )
                    openSignInActivity()
                }
                .show()
        } else {
            openSignInActivity()
        }
    }

    private fun openSettingsFragment() {
        val fragment = AccountDetailsFragment.newInstance()
        (activity as? AutomotiveSettingsActivity)?.addFragment(fragment)
    }

    private fun openSignInActivity() {
        val intent = Intent(activity, AccountActivity::class.java)
        startActivity(intent)
    }

    private suspend fun isAccountUsed(): Boolean {
        return syncManager.isLoggedIn() ||
            !upNextQueue.isEmpty ||
            podcastManager.countSubscribed() > 0 ||
            episodeManager.countEpisodes() > 0
    }
}
