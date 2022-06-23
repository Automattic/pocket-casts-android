package au.com.shiftyjelly.pocketcasts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveDataReactiveStreams
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.profile.AccountDetailsFragment
import au.com.shiftyjelly.pocketcasts.profile.UserView
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AutomotiveSettingsFragment : Fragment() {
    @Inject lateinit var userManager: UserManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_automotive_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userView = view.findViewById<UserView>(R.id.userView)

        LiveDataReactiveStreams.fromPublisher(userManager.getSignInState()).observe(viewLifecycleOwner) { signInState ->
            val loggedIn = signInState.isSignedIn

            if ((userView.signedInState != null && userView.signedInState?.isSignedIn == false) && loggedIn) {
                // We have to close after signing in to meet Google UX requirements
                activity?.finish()
            }

            userView.signedInState = signInState
            userView.setOnClickListener {
                if (loggedIn) {
                    val fragment = AccountDetailsFragment.newInstance()
                    (activity as? FragmentHostListener)?.addFragment(fragment)
                } else {
                    signIn()
                }
            }
        }
    }

    fun signIn() {
        val loginIntent = Intent(activity, AccountActivity::class.java)
        startActivity(loginIntent)
    }
}
