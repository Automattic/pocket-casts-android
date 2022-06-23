package au.com.shiftyjelly.pocketcasts.account

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentPromocodeBinding
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PromoCodeFragment : BaseFragment() {
    companion object {
        const val ARG_PROMO_CODE = "promocode"
    }

    @Inject lateinit var settings: Settings

    private val viewModel: PromoCodeViewModel by viewModels()

    private val promoCode: String
        get() = arguments?.getString(ARG_PROMO_CODE)!!

    private var binding: FragmentPromocodeBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setup(promoCode)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPromocodeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(
            viewLifecycleOwner,
            Observer {
                val binding = binding ?: return@Observer
                when (it) {
                    is PromoCodeViewModel.ViewState.Loading -> {
                        showLoading()
                    }
                    is PromoCodeViewModel.ViewState.Success -> {
                        binding.loadedGroup.isVisible = false
                        // We need to set this to true here so we know not to show the gift dialog.
                        // There is no way to know what type of gift the upgrade came via the API
                        settings.setFreeGiftAcknowledged(true)
                        settings.setFreeGiftAcknowledgedNeedsSync(true)

                        val result = Intent()
                        result.putExtra(AccountActivity.PROMO_CODE_RETURN_DESCRIPTION, it.response.description)
                        activity?.setResult(RESULT_OK, result)
                        activity?.finish()
                    }
                    is PromoCodeViewModel.ViewState.NotSignedIn -> {
                        binding.progress.isVisible = false
                        binding.loadedGroup.isVisible = true
                        binding.btnSignIn.isVisible = true
                        binding.btnDone.setText(LR.string.profile_promo_create_account)
                        binding.btnCreateOnError.isVisible = false

                        binding.lblTitle.setText(LR.string.profile_promo_sign_in_or_create)
                        binding.lblDetail.setText(LR.string.profile_promo_sign_in_or_create_summary)
                        binding.imgDone.setup(ContextCompat.getDrawable(binding.imgDone.context, R.drawable.ic_plus_account))

                        binding.btnSignIn.setOnClickListener {
                            findNavController().navigate(R.id.action_promoCodeFragment_to_signInFragment)
                            showLoading()
                        }
                        binding.btnDone.setOnClickListener {
                            findNavController().navigate(R.id.action_promoCodeFragment_to_createEmailFragment)
                            showLoading()
                        }
                    }
                    is PromoCodeViewModel.ViewState.Failed -> {
                        binding.progress.isVisible = false
                        binding.loadedGroup.isVisible = true
                        binding.btnSignIn.isVisible = false
                        binding.btnCreateOnError.isVisible = it.shouldShowSignup

                        binding.lblTitle.text = it.title
                        binding.lblDetail.text = it.errorMessage
                        val imgDone = binding.imgDone
                        val overlay = if (it.errorOverlayRes != null) ContextCompat.getDrawable(imgDone.context, it.errorOverlayRes) else null
                        imgDone.setup(ContextCompat.getDrawable(imgDone.context, it.errorImageRes), overlay = overlay)

                        binding.btnDone.setText(LR.string.done)
                        binding.btnDone.setOnClickListener { activity?.finish() }

                        binding.btnCreateOnError.setText(if (it.isSignedIn) LR.string.profile_promo_sign_up_for_plus else LR.string.profile_promo_create_pocket_casts_account)
                        binding.btnCreateOnError.setOnClickListener { _ ->
                            if (it.isSignedIn) {
                                activity?.finish()
                                val intent = AccountActivity.newUpgradeInstance(view.context)
                                view.context.startActivity(intent)
                            } else {
                                activity?.finish()
                                val intent = AccountActivity.newAutoSelectPlusInstance(view.context)
                                view.context.startActivity(intent)
                            }
                        }
                    }
                }
            }
        )

        binding?.btnClose?.setOnClickListener { activity?.finish() }
    }

    private fun showLoading() {
        val binding = binding ?: return

        binding.progress.isVisible = true
        binding.btnSignIn.isVisible = false
        binding.loadedGroup.isVisible = false
        binding.btnCreateOnError.isVisible = false

        binding.lblTitle.text = null
        binding.lblDetail.text = null
    }
}
