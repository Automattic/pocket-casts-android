package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentCreateDoneBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountError
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SubscriptionType
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CreateDoneFragment : BaseFragment() {

    private val viewModel: CreateAccountViewModel by activityViewModels()
    private var binding: FragmentCreateDoneBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCreateDoneBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UiUtil.hideKeyboard(view)

        viewModel.updateStateToFinished()
        viewModel.createAccountState.observe(
            viewLifecycleOwner,
            Observer {
                val progress = binding?.progress ?: return@Observer
                when (it) {
                    is CreateAccountState.CurrentlyValid -> {
                        progress.isVisible = false
                    }
                    is CreateAccountState.AccountCreated -> {
                        progress.isVisible = false
                        processCreated(viewModel.subscriptionType.value)
                    }
                    is CreateAccountState.Finished -> {
                        progress.isVisible = false
                        processCreated(viewModel.subscriptionType.value)
                    }
                    is CreateAccountState.Failure -> {
                        progress.isVisible = false
                        val serverFail = it.errors.contains(CreateAccountError.CANNOT_CREATE_ACCOUNT)
                        if (!serverFail) {
                            updateForm(complete = true, title = getString(LR.string.profile_create_failed_title), detail = getString(LR.string.please_try_again))
                        }
                    }
                    else -> {}
                }
            }
        )

        val binding = binding ?: return

        binding.imgDone.setImageResource(IR.drawable.ic_circle)
        binding.btnClose?.setOnClickListener {
            closeForm()
        }

        binding.btnNewsletter.setOnClickListener {
            binding.switchNewsletter.isChecked = !binding.switchNewsletter.isChecked
        }
        binding.switchNewsletter.isChecked = viewModel.newsletter.value ?: false
        binding.switchNewsletter.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateNewsletter(isChecked)
        }

        binding.btnDone.setOnClickListener {
            closeForm()
        }
    }

    private fun closeForm() {
        viewModel.onCloseDoneForm()
        if (findNavController().graph.startDestinationId == R.id.promoCodeFragment) {
            findNavController().popBackStack(R.id.promoCodeFragment, false)
        } else {
            activity?.finish()
        }
    }

    private fun processCreated(subscriptionType: SubscriptionType?) {
        var resourceId = 0
        val title = getString(LR.string.profile_account_created)
        var detail = ""
        if (subscriptionType != null) {
            if (subscriptionType == SubscriptionType.FREE) {
                resourceId = R.drawable.ic_created_free_account
                detail = getString(LR.string.profile_welcome_to_free)
            } else {
                resourceId = R.drawable.ic_created_plus_account
                detail = getString(LR.string.profile_welcome_to_plus)
            }
        }
        binding?.imgDone?.setImageResource(resourceId)
        updateForm(true, title, detail)
    }

    private fun updateForm(complete: Boolean, title: String, detail: String) {
        val binding = binding ?: return
        binding.imgDone.isVisible = true
        binding.txtTitle.text = title
        binding.txtDetail.text = detail
        binding.btnDone.isVisible = complete
    }
}
