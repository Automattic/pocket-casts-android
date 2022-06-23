package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentResetPasswordBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ResetPasswordError
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ResetPasswordState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ResetPasswordViewModel
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getTintedDrawable
import au.com.shiftyjelly.pocketcasts.views.extensions.addOnTextChanged
import au.com.shiftyjelly.pocketcasts.views.extensions.showKeyboard
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class ResetPasswordFragment : BaseFragment() {

    private val viewModel: ResetPasswordViewModel by viewModels()
    private var binding: FragmentResetPasswordBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.progress?.isVisible = false
        binding?.txtEmail?.showKeyboard()

        viewModel.clearValues()

        binding?.txtEmail?.addOnTextChanged {
            viewModel.updateEmail(it)
        }

        viewModel.resetPasswordState.observe(
            viewLifecycleOwner,
            Observer {
                val binding = binding ?: return@Observer
                val progress = binding.progress
                val txtError = binding.txtError
                when (it) {
                    is ResetPasswordState.Empty -> {
                        updateForm(false)
                    }
                    is ResetPasswordState.Failure -> {
                        progress.isVisible = false

                        val invalidEmail = it.errors.contains(ResetPasswordError.INVALID_EMAIL)
                        val serverFail = it.errors.contains(ResetPasswordError.SERVER)

                        updateForm(invalidEmail)

                        if (serverFail) {
                            val error = it.message ?: "Check your email address"
                            txtError.text = error
                            viewModel.clearServerError()
                        }
                    }
                    is ResetPasswordState.Loading -> {
                        txtError.text = ""
                        progress.isVisible = true
                    }
                    is ResetPasswordState.Success -> {
                        progress.isVisible = false
                        UiUtil.displayAlert(
                            activity,
                            getString(LR.string.profile_reset_password_sent),
                            getString(LR.string.profile_reset_password_check_email),
                            onComplete = {
                                if (isAdded) {
                                    parentFragmentManager.popBackStack()
                                }
                            }
                        )
                    }
                }
            }
        )

        val binding = binding ?: return
        binding.btnConfirm.setOnClickListener {
            binding.progress.isVisible = true
            UiUtil.hideKeyboard(binding.txtEmail)
            viewModel.resetPassword()
        }
    }

    private fun updateForm(invalidEmail: Boolean) {
        val binding = binding ?: return
        val context = binding.root.context

        val emailColor = context.getThemeColor(UR.attr.primary_interactive_01)
        val emailDrawable = context.getTintedDrawable(R.drawable.ic_mail, emailColor)
        val tickColor = context.getThemeColor(UR.attr.support_02)
        val tickDrawable = if (!invalidEmail)context.getTintedDrawable(IR.drawable.ic_tick_circle, tickColor) else null
        val max = 64
        emailDrawable?.setBounds(0, 0, max, max)
        tickDrawable?.setBounds(0, 0, max, max)
        binding.txtEmail.setCompoundDrawables(emailDrawable, null, tickDrawable, null)

        val valid = !invalidEmail
        binding.btnConfirm.isEnabled = valid
        binding.btnConfirm.alpha = if (valid) 1.0f else 0.2f
    }
}
