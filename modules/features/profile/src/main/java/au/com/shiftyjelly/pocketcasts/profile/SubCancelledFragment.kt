package au.com.shiftyjelly.pocketcasts.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.databinding.FragmentCancelledSubBinding
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class SubCancelledFragment : BaseFragment() {
    companion object {
        fun newInstance(): SubCancelledFragment {
            return SubCancelledFragment()
        }
    }

    @Inject lateinit var settings: Settings

    private val viewModel: AccountDetailsViewModel by viewModels()
    private val dateFormatter = SimpleDateFormat.getDateInstance()
    private var binding: FragmentCancelledSubBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCancelledSubBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.viewState.observe(viewLifecycleOwner) { (signInState, _) ->
            val expiryDate = ((signInState as? SignInState.SignedIn)?.subscriptionStatus as? SubscriptionStatus.Plus)?.expiry ?: Date()
            val endDate = dateFormatter.format(expiryDate)
            binding?.txtHint0?.text = getString(LR.string.profile_sub_cancel_hint0) + " " + endDate
        }

        binding?.btnDone?.setOnClickListener {
            (activity as? FragmentHostListener)?.bottomSheetClosePressed(this)
        }

        settings.setCancelledAcknowledged(true)
    }
}
