package au.com.shiftyjelly.pocketcasts.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.account.AccountActivity.AccountUpdatedSource
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentChangeEmailBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ChangeEmailError
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ChangeEmailState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ChangeEmailViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.DoneViewModel
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getTintedDrawable
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.addOnTextChanged
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class ChangeEmailFragment : BaseFragment() {
    companion object {
        fun newInstance(): ChangeEmailFragment {
            return ChangeEmailFragment()
        }
    }

    private val viewModel: ChangeEmailViewModel by viewModels()
    private val doneViewModel: DoneViewModel by activityViewModels()
    private var binding: FragmentChangeEmailBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentChangeEmailBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @Suppress("DEPRECATION")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // hack: enable scrolling upon keyboard
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onDetach() {
        super.onDetach()
        // hack: enable scrolling upon keyboard
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return
        val toolbar = binding.toolbar
        val progress = binding.progress
        val txtEmail = binding.txtEmail
        val txtEmailCurrent = binding.txtEmailCurrent
        val txtPwd = binding.txtPwd
        val txtError = binding.txtError
        val btnConfirm = binding.btnConfirm

        toolbar?.title = getString(LR.string.profile_change_email_address_title)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        progress.isVisible = false
        txtEmail.requestFocus()

        viewModel.clearValues()

        txtEmailCurrent.text = viewModel.existingEmail
        txtEmail.setText(viewModel.email.value?.toString())
        txtEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                txtPwd.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }
        txtEmail.addOnTextChanged {
            viewModel.updateEmail(it)
        }

        txtPwd.addOnTextChanged {
            viewModel.updatePassword(it)
        }

        viewModel.changeEmailState.observe(viewLifecycleOwner) {
            when (it) {
                is ChangeEmailState.Empty -> {
                    updateForm(invalidEmail = false, invalidPwd = false)
                }
                is ChangeEmailState.Failure -> {
                    progress.isVisible = false

                    val invalidEmail = it.errors.contains(ChangeEmailError.INVALID_EMAIL)
                    val invalidPwd = it.errors.contains(ChangeEmailError.INVALID_PASSWORD)
                    val serverFail = it.errors.contains(ChangeEmailError.SERVER)

                    updateForm(invalidEmail, invalidPwd)

                    if (serverFail) {
                        val error = it.message ?: "Check your email"
                        txtError.text = error
                        viewModel.clearServerError()
                    }
                }
                is ChangeEmailState.Loading -> {
                    txtError.text = ""
                    progress.isVisible = true
                }
                is ChangeEmailState.Success -> {
                    progress.isVisible = false

                    val second = viewModel.email.value ?: ""
                    doneViewModel.updateTitle(getString(LR.string.profile_email_address_changed))
                    doneViewModel.updateDetail(second)
                    doneViewModel.updateImage(R.drawable.ic_email_address_changed)
                    doneViewModel.trackShown(AccountUpdatedSource.CHANGE_EMAIL)

                    val activity = requireActivity()
                    activity.onBackPressed() // done fragment needs to back to profile page
                    val fragment = ChangeDoneFragment.newInstance()
                    (activity as FragmentHostListener).addFragment(fragment)
                }
            }
        }

        btnConfirm.setOnClickListener {
            progress.isVisible = true
            UiUtil.hideKeyboard(txtPwd)
            viewModel.changeEmail()
        }
    }

    override fun onPause() {
        super.onPause()

        val binding = binding ?: return
        UiUtil.hideKeyboard(binding.root)
    }

    private fun updateForm(invalidEmail: Boolean, invalidPwd: Boolean) {
        val binding = binding ?: return

        val view = binding.root
        val context = view.context
        val txtEmail = binding.txtEmail
        val txtPwd = binding.txtPwd
        val btnConfirm = binding.btnConfirm

        val emailColor = context.getThemeColor(UR.attr.primary_interactive_01)
        val emailDrawable = context.getTintedDrawable(R.drawable.ic_mail, emailColor)
        val passwordDrawable = context.getTintedDrawable(R.drawable.ic_password, emailColor)

        val tickColor = context.getThemeColor(UR.attr.support_02)
        val tickDrawable = if (!invalidEmail) context.getTintedDrawable(IR.drawable.ic_tick_circle, tickColor) else null
        val max = 32.dpToPx(context)
        emailDrawable?.setBounds(0, 0, max, max)
        tickDrawable?.setBounds(0, 0, max, max)
        passwordDrawable?.setBounds(0, 0, max, max)

        txtEmail.setCompoundDrawables(emailDrawable, null, tickDrawable, null)
        txtPwd.setCompoundDrawablesRelative(passwordDrawable, null, null, null)

        val bothMatch = !invalidEmail && !invalidPwd
        btnConfirm.isEnabled = bothMatch
        btnConfirm.alpha = if (bothMatch) 1.0f else 0.2f
    }
}
