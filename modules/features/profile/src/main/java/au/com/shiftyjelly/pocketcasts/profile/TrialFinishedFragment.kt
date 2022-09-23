package au.com.shiftyjelly.pocketcasts.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.profile.databinding.FragmentTrialFinishedBinding
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrialFinishedFragment : BaseFragment() {

    private var binding: FragmentTrialFinishedBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTrialFinishedBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.btnDone.setOnClickListener {
            @Suppress("DEPRECATION")
            activity?.onBackPressed()
        }
        binding.btnUpgrade.setOnClickListener {
            @Suppress("DEPRECATION")
            activity?.onBackPressed()
            activity?.startActivity(AccountActivity.newUpgradeInstance(it.context))
        }
    }
}
