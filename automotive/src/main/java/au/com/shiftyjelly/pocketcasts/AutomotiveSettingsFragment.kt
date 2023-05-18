package au.com.shiftyjelly.pocketcasts

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.databinding.FragmentAutomotiveSettingsBinding
import au.com.shiftyjelly.pocketcasts.profile.AccountDetailsFragment
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.cartheme.R as CR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AutomotiveSettingsFragment : Fragment(), CoroutineScope {
    @Inject lateinit var userManager: UserManager
    @Inject lateinit var syncManager: SyncManager
    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var folderManager: FolderManager
    @Inject lateinit var playlistManager: PlaylistManager
    @Inject lateinit var searchHistoryManager: SearchHistoryManager
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var upNextQueue: UpNextQueue

    private lateinit var binding: FragmentAutomotiveSettingsBinding

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAutomotiveSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userView = binding.userView

        userManager.getSignInState()
            .toLiveData()
            .observe(viewLifecycleOwner) { signInState ->
                val loggedIn = signInState.isSignedIn

                if ((userView.signedInState != null && userView.signedInState?.isSignedIn == false) && loggedIn) {
                    // We have to close after signing in to meet Google UX requirements
                    activity?.finish()
                }

                userView.signedInState = signInState
                userView.lblUserEmail.setOnClickListener { onProfileAccountButtonClicked(loggedIn) }
                userView.imgProfilePicture.setOnClickListener { onProfileAccountButtonClicked(loggedIn) }
                userView.btnAccount?.setOnClickListener { onProfileAccountButtonClicked(loggedIn) }
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
                        playlistManager = playlistManager,
                        folderManager = folderManager,
                        searchHistoryManager = searchHistoryManager,
                        episodeManager = episodeManager,
                        wasInitiatedByUser = false
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
