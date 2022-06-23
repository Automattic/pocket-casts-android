package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.SystemBatteryRestrictions
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment() {

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var batteryRestrictions: SystemBatteryRestrictions

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                AppTheme(theme.activeTheme) {

                    var isUnrestrictedBattery by remember { mutableStateOf(batteryRestrictions.isUnrestricted()) }
                    DisposableEffect(this) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                isUnrestrictedBattery = batteryRestrictions.isUnrestricted()
                            }
                        }

                        lifecycle.addObserver(observer)
                        onDispose {
                            lifecycle.removeObserver(observer)
                        }
                    }

                    userManager
                        .getSignInState()
                        .subscribeAsState(null)
                        .value
                        ?.let { signInState ->

                            SettingsFragmentPage(
                                signInState = signInState,
                                onBackPressed = { activity?.onBackPressed() },
                                isDebug = BuildConfig.DEBUG,
                                isUnrestrictedBattery = isUnrestrictedBattery,
                                openFragment = { fragment ->
                                    (activity as? FragmentHostListener)?.addFragment(fragment)
                                }
                            )
                        }
                }
            }
        }
}
