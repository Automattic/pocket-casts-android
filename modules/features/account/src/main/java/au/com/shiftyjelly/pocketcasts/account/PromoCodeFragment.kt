package au.com.shiftyjelly.pocketcasts.account

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
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

    private var form by mutableStateOf<PromoCodePageState?>(null)
    private var onDoneClick: () -> Unit = {}
    private var onCreateClick: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setup(promoCode, requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            PromoCodePage(
                state = form ?: initialState(),
                onCloseClick = { activity?.finish() },
                onDoneClick = { onDoneClick() },
                onCreateClick = { onCreateClick() },
                onSignInClick = {
                    findNavController().navigate(R.id.action_promoCodeFragment_to_signInFragment)
                    showLoading()
                },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        form = initialState()
        viewModel.state.observe(
            viewLifecycleOwner,
            Observer {
                val form = form ?: return@Observer
                when (it) {
                    is PromoCodeViewModel.ViewState.Loading -> {
                        showLoading()
                    }

                    is PromoCodeViewModel.ViewState.Success -> {
                        this.form = form.copy(isLoaded = false)
                        // We need to set this to true here so we know not to show the gift dialog.
                        // There is no way to know what type of gift the upgrade came via the API
                        settings.freeGiftAcknowledged.set(true, updateModifiedAt = true)

                        val result = Intent()
                        result.putExtra(AccountActivity.PROMO_CODE_RETURN_DESCRIPTION, it.response.description)
                        activity?.setResult(RESULT_OK, result)
                        activity?.finish()
                    }

                    is PromoCodeViewModel.ViewState.NotSignedIn -> {
                        this.form = form.copy(
                            isProgressVisible = false,
                            isLoaded = true,
                            isSignInVisible = true,
                            doneText = getString(LR.string.profile_promo_create_account),
                            createText = null,
                            title = getString(LR.string.profile_promo_sign_in_or_create),
                            detail = getString(LR.string.profile_promo_sign_in_or_create_summary),
                            imageRes = R.drawable.ic_plus_account,
                            overlayRes = null,
                        )
                        onDoneClick = {
                            findNavController().navigate(R.id.action_promoCodeFragment_to_createEmailFragment)
                            showLoading()
                        }
                    }

                    is PromoCodeViewModel.ViewState.Failed -> {
                        this.form = form.copy(
                            isProgressVisible = false,
                            isLoaded = true,
                            isSignInVisible = false,
                            createText = if (it.shouldShowSignup) getString(if (it.isSignedIn) LR.string.profile_promo_sign_up_for_plus else LR.string.profile_promo_create_pocket_casts_account) else null,
                            title = it.title,
                            detail = it.errorMessage,
                            imageRes = it.errorImageRes,
                            overlayRes = it.errorOverlayRes,
                            doneText = getString(LR.string.done),
                        )
                        onDoneClick = { activity?.finish() }
                        onCreateClick = {
                            activity?.finish()
                            val intent = if (it.isSignedIn) AccountActivity.newUpgradeInstance(view.context) else AccountActivity.newAutoSelectPlusInstance(view.context)
                            view.context.startActivity(intent)
                        }
                    }
                }
            },
        )
    }

    private fun showLoading() {
        form = form?.copy(isProgressVisible = true, isSignInVisible = false, isLoaded = false, createText = null, title = "", detail = "")
    }

    private fun initialState() = PromoCodePageState(
        isProgressVisible = true,
        isLoaded = true,
        imageRes = R.drawable.ic_create_new_account,
        overlayRes = null,
        title = "",
        detail = "",
        doneText = getString(LR.string.done),
        createText = null,
        isSignInVisible = false,
    )
}
