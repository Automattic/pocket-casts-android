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
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentChangePwdBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ChangePasswordError
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ChangePasswordState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ChangePwdViewModel
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
class ChangePwdFragment : BaseFragment() {
    companion object {
        fun newInstance(): ChangePwdFragment {
            return ChangePwdFragment()
        }
    }

    private val viewModel: ChangePwdViewModel by viewModels()
    private val doneViewModel: DoneViewModel by activityViewModels()
    private var binding: FragmentChangePwdBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentChangePwdBinding.inflate(inflater, container, false)
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
        val txtPwdCurrent = binding.txtPwdCurrent
        val txtPwdNew = binding.txtPwdNew
        val txtPwdConfirm = binding.txtPwdConfirm
        val txtError = binding.txtError
        val btnConfirm = binding.btnConfirm

        toolbar?.setTitle(LR.string.profile_change_password_title)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        progress.isVisible = false
        txtPwdCurrent.requestFocus()

        viewModel.clearValues()

        txtPwdCurrent.setText(viewModel.pwdCurrent.value?.toString())
        txtPwdCurrent.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                txtPwdNew.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        txtPwdNew.setText(viewModel.pwdCurrent.value?.toString())
        txtPwdNew.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                txtPwdConfirm.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        txtPwdConfirm.setText(viewModel.pwdCurrent.value?.toString())

        txtPwdCurrent.addOnTextChanged {
            viewModel.updatePwdCurrent(it)
        }

        txtPwdNew.addOnTextChanged {
            viewModel.updatePwdNew(it)
        }

        txtPwdConfirm.addOnTextChanged {
            viewModel.updatePwdConfirm(it)
        }

        viewModel.changePasswordState.observe(viewLifecycleOwner) {
            when (it) {
                is ChangePasswordState.Empty -> {
                    updateForm(invalidPwdCurrent = false, invalidPwdNew = false, invalidPwdConfirm = false)
                }
                is ChangePasswordState.Failure -> {
                    progress.isVisible = false

                    val invalidPwdCurrent = it.errors.contains(ChangePasswordError.INVALID_PASSWORD_CURRENT)
                    val invalidPwdNew = it.errors.contains(ChangePasswordError.INVALID_PASSWORD_NEW)
                    val invalidPwdConfirm = it.errors.contains(ChangePasswordError.INVALID_PASSWORD_CONFIRM)
                    val serverFail = it.errors.contains(ChangePasswordError.SERVER)

                    updateForm(invalidPwdCurrent, invalidPwdNew, invalidPwdConfirm)

                    if (serverFail) {
                        val error = it.message ?: "Check your password"
                        txtError.text = error
                        viewModel.clearServerError()
                    }
                }
                is ChangePasswordState.Loading -> {
                    txtError.text = ""
                    progress.isVisible = true
                }
                is ChangePasswordState.Success -> {
                    progress.isVisible = false

                    doneViewModel.updateTitle(getString(LR.string.profile_password_changed))
                    doneViewModel.updateDetail(getString(LR.string.profile_password_changed_successful))
                    doneViewModel.updateImage(R.drawable.ic_password_changed)
                    doneViewModel.trackShown(AccountUpdatedSource.CHANGE_PASSWORD)

                    val fragment = ChangeDoneFragment.newInstance(closeParent = true)
                    (activity as FragmentHostListener).addFragment(fragment)
                }
            }
        }

        btnConfirm.setOnClickListener {
            progress.isVisible = true
            UiUtil.hideKeyboard(txtPwdConfirm)
            viewModel.changePassword()
        }
    }

    override fun onPause() {
        super.onPause()

        val binding = binding ?: return
        UiUtil.hideKeyboard(binding.root)
    }

    private fun updateForm(invalidPwdCurrent: Boolean, invalidPwdNew: Boolean, invalidPwdConfirm: Boolean) {
        val binding = binding ?: return

        val view = binding.root
        val context = view.context

        val allMatch = !invalidPwdCurrent && !invalidPwdNew && !invalidPwdConfirm
        binding.btnConfirm.isEnabled = allMatch
        binding.btnConfirm.alpha = if (allMatch) 1.0f else 0.2f

        val tintColor = context.getThemeColor(UR.attr.primary_interactive_01)
        val passwordDrawable = context.getTintedDrawable(IR.drawable.ic_password, tintColor)
        val iconSize = 32.dpToPx(context)
        passwordDrawable?.setBounds(0, 0, iconSize, iconSize)

        binding.txtPwdCurrent.setCompoundDrawablesRelative(passwordDrawable, null, null, null)
        binding.txtPwdNew.setCompoundDrawablesRelative(passwordDrawable, null, null, null)
        binding.txtPwdConfirm.setCompoundDrawablesRelative(passwordDrawable, null, null, null)
    }
}
