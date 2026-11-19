package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountError
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SubscriptionType
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CreateDoneFragment : BaseFragment() {

    private val viewModel: CreateAccountViewModel by activityViewModels()
    private var form by mutableStateOf(CreateDoneForm())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            val form = form
            val newsletter by viewModel.newsletter.observeAsState(false)
            CreateDonePage(
                state = CreateDonePageState(
                    isCar = Util.isCarUiMode(requireContext()),
                    imageRes = form.imageRes,
                    title = form.title,
                    detail = form.detail,
                    isNewsletterChecked = newsletter,
                    isProgressVisible = form.isProgressVisible,
                ),
                onNewsletterChange = { isChecked -> if (isChecked != viewModel.newsletter.value) viewModel.updateNewsletter(isChecked) },
                onCloseClick = ::closeForm,
                onDoneClick = ::closeForm,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UiUtil.hideKeyboard(view)

        form = CreateDoneForm()

        viewModel.updateStateToFinished()
        viewModel.createAccountState.observe(
            viewLifecycleOwner,
            Observer {
                when (it) {
                    is CreateAccountState.CurrentlyValid -> {
                        form = form.copy(isProgressVisible = false)
                    }

                    is CreateAccountState.AccountCreated -> {
                        form = form.copy(isProgressVisible = false)
                        processCreated(viewModel.subscriptionType.value)
                    }

                    is CreateAccountState.Finished -> {
                        form = form.copy(isProgressVisible = false)
                        processCreated(viewModel.subscriptionType.value)
                    }

                    is CreateAccountState.Failure -> {
                        form = form.copy(isProgressVisible = false)
                        val serverFail = it.errors.contains(CreateAccountError.CANNOT_CREATE_ACCOUNT)
                        if (!serverFail) {
                            form = form.copy(title = getString(LR.string.profile_create_failed_title), detail = getString(LR.string.please_try_again))
                        }
                    }

                    else -> {}
                }
            },
        )
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
        form = when (subscriptionType) {
            null -> form.copy(imageRes = null, title = "", detail = "")

            SubscriptionType.FREE -> form.copy(
                imageRes = R.drawable.ic_created_free_account,
                title = getString(LR.string.profile_account_created),
                detail = getString(LR.string.profile_welcome_to_free),
            )

            else -> form.copy(
                imageRes = R.drawable.ic_created_plus_account,
                title = getString(LR.string.profile_account_upgraded),
                detail = getString(LR.string.profile_welcome_to_plus),
            )
        }
    }
}

private data class CreateDoneForm(
    val imageRes: Int? = IR.drawable.ic_circle,
    val title: String = "",
    val detail: String = "",
    val isProgressVisible: Boolean = true,
)
