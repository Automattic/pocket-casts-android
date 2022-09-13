package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentCreateEmailBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountError
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SubscriptionType
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getTintedDrawable
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.addOnTextChanged
import au.com.shiftyjelly.pocketcasts.views.extensions.showKeyboard
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class CreateEmailFragment : BaseFragment() {
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private val viewModel: CreateAccountViewModel by activityViewModels()
    private var currentEditText: TextInputEditText? = null
    private var binding: FragmentCreateEmailBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCreateEmailBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return
        val txtEmail = binding.txtEmail
        val txtPassword = binding.txtPassword
        val progress = binding.progress
        val txtError = binding.txtError

        viewModel.clearError(CreateAccountError.CANNOT_CREATE_ACCOUNT)

        val startingEmailValue = viewModel.email.value ?: ""
        val startingPasswordValue = viewModel.password.value ?: ""
        viewModel.updateEmail(startingEmailValue)
        viewModel.updatePassword(startingPasswordValue)

        txtEmail.setText(viewModel.email.value?.toString())
        txtPassword.setText(viewModel.password.value?.toString())

        txtEmail.showKeyboard()
        currentEditText = txtEmail

        txtEmail.setOnFocusChangeListener { _, _ ->
            currentEditText = txtEmail
            viewModel.updateEmailRefresh()
        }

        txtPassword.setOnFocusChangeListener { _, _ ->
            currentEditText = txtPassword
            viewModel.updatePasswordRefresh()
        }

        txtEmail.addOnTextChanged {
            viewModel.updateEmail(it)
        }

        txtPassword.addOnTextChanged {
            viewModel.updatePassword(it)
        }

        viewModel.createAccountState.observe(viewLifecycleOwner) {
            when (it) {
                is CreateAccountState.CurrentlyValid -> {
                    progress.isVisible = false
                    updateForm(invalidEmail = false, invalidPassword = false)
                }
                is CreateAccountState.AccountCreating -> {
                    progress.isVisible = true
                }
                is CreateAccountState.AccountCreated -> {
                    progress.isVisible = false
                    if (viewModel.subscriptionType.value == SubscriptionType.FREE) {
                        view.findNavController().navigate(R.id.action_createEmailFragment_to_createDoneFragment)
                    } else {
                        view.findNavController().navigate(R.id.action_createEmailFragment_to_createPayNowFragment)
                    }
                }
                is CreateAccountState.Failure -> {
                    progress.isVisible = false
                    val invalidEmail = viewModel.currentStateHasError(CreateAccountError.INVALID_EMAIL)
                    val invalidPassword = viewModel.currentStateHasError(CreateAccountError.INVALID_PASSWORD)
                    val serverFail = viewModel.currentStateHasError(CreateAccountError.CANNOT_CREATE_ACCOUNT)
                    updateForm(invalidEmail, invalidPassword)

                    if (serverFail) {
                        val error = it.message ?: getString(LR.string.profile_create_failed)
                        txtError.text = error
                        viewModel.clearError(CreateAccountError.CANNOT_CREATE_ACCOUNT)
                    }
                }
                else -> {}
            }
        }

        binding.btnNext.setOnClickListener { v ->
            if (!viewModel.currentStateHasError(CreateAccountError.INVALID_EMAIL) &&
                !viewModel.currentStateHasError(CreateAccountError.INVALID_PASSWORD)
            ) {
                txtError.text = ""
                UiUtil.hideKeyboard(v)
                analyticsTracker.track(AnalyticsEvent.CREATE_ACCOUNT_NEXT_BUTTON_TAPPED)
                viewModel.sendCreateAccount()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        val binding = binding ?: return
        UiUtil.hideKeyboard(binding.root)
    }

    private fun updateForm(invalidEmail: Boolean, invalidPassword: Boolean) {
        val binding = binding ?: return

        val view = binding.root
        val context = view.context
        val txtEmail = binding.txtEmail
        val txtPassword = binding.txtPassword
        val lblPasswordRequirements = binding.lblPasswordRequirements

        val finalInvalidEmail = invalidEmail || txtEmail.length() == 0
        val finalInvalidPassword = invalidPassword && txtPassword.length() > 0

        val emailColor = if (currentEditText == txtEmail) context.getThemeColor(UR.attr.primary_icon_03_active) else context.getThemeColor(UR.attr.primary_icon_03)
        val emailDrawable = context.getTintedDrawable(R.drawable.ic_mail, emailColor)
        val tickColor = context.getThemeColor(UR.attr.support_02)
        val tickDrawable = if (!finalInvalidEmail) context.getTintedDrawable(IR.drawable.ic_tick_circle, tickColor) else null
        val passwordColor = if (currentEditText == txtPassword) context.getThemeColor(UR.attr.primary_icon_03_active) else context.getThemeColor(UR.attr.primary_icon_03)
        val passwordDrawable = context.getTintedDrawable(R.drawable.ic_password, passwordColor)

        val iconSize = 32.dpToPx(context)
        val tickSize = 24.dpToPx(context)
        emailDrawable?.setBounds(0, 0, iconSize, iconSize)
        tickDrawable?.setBounds(0, 0, tickSize, tickSize)
        passwordDrawable?.setBounds(0, 0, iconSize, iconSize)

        txtEmail.setCompoundDrawables(emailDrawable, null, tickDrawable, null)
        txtPassword.setCompoundDrawablesRelative(passwordDrawable, null, null, null)

        var passwordTextColor = context.getThemeColor(UR.attr.primary_text_02)
        if (finalInvalidPassword) {
            passwordTextColor = context.getThemeColor(UR.attr.support_05)
        }
        lblPasswordRequirements.setTextColor(passwordTextColor)

        updateButton(!invalidEmail && !invalidPassword)
    }

    private fun updateButton(show: Boolean) {
        val binding = binding ?: return

        binding.btnNext.isEnabled = show
        binding.btnNext.alpha = if (show) 1.0f else 0.2f
    }
}
