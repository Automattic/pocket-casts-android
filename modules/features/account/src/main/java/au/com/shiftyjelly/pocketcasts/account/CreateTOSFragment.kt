package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentCreateTosBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CreateTOSFragment : BaseFragment() {

    private val viewModel: CreateAccountViewModel by activityViewModels()
    private var binding: FragmentCreateTosBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCreateTosBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.btnAgree.setOnClickListener {
            viewModel.updateTermsOfUse(true)

            if (viewModel.upgradeMode.value == true) {
                viewModel.updateStateTotAccountCreated()
                it.findNavController().navigate(R.id.action_createTOSFragment_to_createPayNowFragment)
            } else {
                it.findNavController().navigate(R.id.action_createTOSFragment_to_createEmailFragment)
            }
        }

        binding.btnDisagree.setOnClickListener {
            viewModel.updateTermsOfUse(false)
            requireActivity().finish()
        }

        binding.btnTerms.setOnClickListener {
            val intent = WebViewActivity.newInstance(it.context, getString(LR.string.profile_terms_of_use_title), Settings.INFO_TOS_URL)
            startActivity(intent)
        }

        binding.btnPrivacy.setOnClickListener {
            val intent = WebViewActivity.newInstance(it.context, getString(LR.string.profile_privacy_policy_title), Settings.INFO_PRIVACY_URL)
            startActivity(intent)
        }
    }
}
