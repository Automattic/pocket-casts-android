package au.com.shiftyjelly.pocketcasts.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.collect
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class SubCancelledFragment : BaseFragment() {
    companion object {
        fun newInstance(): SubCancelledFragment {
            return SubCancelledFragment()
        }
    }

    @Inject
    lateinit var settings: Settings

    private val viewModel: AccountDetailsViewModel by viewModels()
    private val dateFormatter = SimpleDateFormat.getDateInstance()
    private var expiryHint by mutableStateOf("")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            SubCancelledPage(
                expiryHint = expiryHint,
                onDoneClick = { (activity as? FragmentHostListener)?.closeBottomSheet() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signInState.collect { signInState ->
                    val expiryDate = (signInState as? SignInState.SignedIn)?.subscription?.expiryDate ?: Instant.now()
                    runCatching {
                        dateFormatter.format(Date.from(expiryDate))
                    }.onSuccess {
                        expiryHint = getString(LR.string.profile_sub_cancel_hint0) + " " + it
                    }.onFailure {
                        Timber.w("Failed to format date $expiryDate: $it")
                    }.getOrNull()
                }
            }
        }

        settings.setCancelledAcknowledged(true)
    }
}
