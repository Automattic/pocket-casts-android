package au.com.shiftyjelly.pocketcasts.account

import android.app.PendingIntent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentSignInBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInError
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInViewModel
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getTintedDrawable
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.addOnTextChanged
import au.com.shiftyjelly.pocketcasts.views.extensions.showKeyboard
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class SignInFragment : BaseFragment() {
    companion object {
        const val EXTRA_SUCCESS_INTENT = "success_intent"

        fun newInstance(): SignInFragment {
            return SignInFragment()
        }
    }

    private val viewModel: SignInViewModel by viewModels()
    private var binding: FragmentSignInBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return
        val progress = binding.progress
        val txtEmail = binding.txtEmail
        val txtPwd = binding.txtPwd
        val txtError = binding.txtError
        val btnSignIn = binding.btnSignIn
        val btnReset = binding.btnReset

        progress.isVisible = false

        viewModel.clearValues()

        txtEmail.setText(viewModel.email.value?.toString())
        txtEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                txtPwd.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        txtPwd.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                signIn(viewModel)
                return@setOnEditorActionListener true
            }
            false
        }

        txtEmail.showKeyboard()
        txtEmail.addOnTextChanged {
            viewModel.updateEmail(it)
        }

        txtPwd.addOnTextChanged {
            viewModel.updatePassword(it)
        }

        viewModel.signInState.observe(viewLifecycleOwner) {
            when (it) {
                is SignInState.Empty -> {
                    updateForm(invalidEmail = false, invalidPwd = false, loading = false)
                }
                is SignInState.Failure -> {
                    progress.isVisible = false

                    val invalidEmail = it.errors.contains(SignInError.INVALID_EMAIL)
                    val invalidPwd = it.errors.contains(SignInError.INVALID_PASSWORD)
                    val serverFail = it.errors.contains(SignInError.SERVER)

                    updateForm(invalidEmail, invalidPwd, loading = false)

                    if (serverFail) {
                        val error = it.message ?: "Check your email and password"
                        txtError.text = error
                        txtError.isVisible = true
                        viewModel.clearServerError()
                    }
                }
                is SignInState.Loading -> {
                    txtError.text = ""
                    progress.isVisible = true

                    updateForm(invalidEmail = false, invalidPwd = false, loading = true)
                }
                is SignInState.Success -> {
                    progress.isVisible = false
                    if (findNavController().graph.startDestinationId == R.id.promoCodeFragment) {
                        findNavController().popBackStack(R.id.promoCodeFragment, false)
                    } else {
                        activity?.finish()

                        if (arguments?.containsKey(EXTRA_SUCCESS_INTENT) == true) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arguments?.getParcelable(EXTRA_SUCCESS_INTENT, PendingIntent::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                arguments?.getParcelable(EXTRA_SUCCESS_INTENT)
                            }?.send()
                        }
                    }
                }
            }
        }

        btnSignIn.setOnClickListener {
            signIn(viewModel)
        }

        btnReset.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_resetPasswordFragment)
        }
    }

    private fun signIn(viewModel: SignInViewModel) {
        val binding = binding ?: return
        binding.progress.isVisible = true
        UiUtil.hideKeyboard(binding.txtPwd)
        viewModel.signIn()
    }

    override fun onPause() {
        super.onPause()

        val binding = binding ?: return
        UiUtil.hideKeyboard(binding.root)
    }

    private fun updateForm(invalidEmail: Boolean, invalidPwd: Boolean, loading: Boolean) {
        val binding = binding ?: return

        val view = binding.root
        val context = view.context

        val emailColor = context.getThemeColor(UR.attr.primary_interactive_01)
        val emailDrawable = context.getTintedDrawable(IR.drawable.ic_mail, emailColor)
        val passwordDrawable = context.getTintedDrawable(IR.drawable.ic_password, emailColor)

        val tickColor = context.getThemeColor(UR.attr.support_02)
        val tickDrawable = if (!invalidEmail) context.getTintedDrawable(IR.drawable.ic_tick_circle, tickColor) else null
        val max = 32.dpToPx(context)
        emailDrawable?.setBounds(0, 0, max, max)
        passwordDrawable?.setBounds(0, 0, max, max)
        tickDrawable?.setBounds(0, 0, max, max)

        binding.txtEmail.setCompoundDrawables(emailDrawable, null, tickDrawable, null)
        binding.txtPwd.setCompoundDrawablesRelative(passwordDrawable, null, null, null)

        val bothMatch = !invalidEmail && !invalidPwd
        binding.btnSignIn.isEnabled = bothMatch && !loading
        binding.btnSignIn.alpha = if (bothMatch) 1.0f else 0.2f
    }
}
