package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentPromocodeUpgradedBinding
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ARG_DESCRIPTION = "description"

@AndroidEntryPoint
class PromoCodeUpgradedFragment : BaseDialogFragment() {
    companion object {
        fun newInstance(codeDescription: String): PromoCodeUpgradedFragment {
            val instance = PromoCodeUpgradedFragment()
            instance.arguments = bundleOf(
                ARG_DESCRIPTION to codeDescription
            )
            return instance
        }
    }

    val codeDescription: String
        get() = arguments?.getString(ARG_DESCRIPTION) ?: ""

    private var binding: FragmentPromocodeUpgradedBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPromocodeUpgradedBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.imgLogo.setImageDrawable(theme.verticalPlusLogo(binding.imgLogo.context))
        binding.lblDescription.text = "$codeDescription\n${getString(LR.string.profile_promo_upgraded_welcome)}"

        binding.btnDone.setOnClickListener { dismiss() }
    }
}
