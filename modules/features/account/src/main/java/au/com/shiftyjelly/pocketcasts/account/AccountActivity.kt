package au.com.shiftyjelly.pocketcasts.account

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import au.com.shiftyjelly.pocketcasts.account.databinding.AccountActivityBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SubscriptionType
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR

@AndroidEntryPoint
class AccountActivity : AppCompatActivity() {

    @Inject lateinit var theme: Theme
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private val viewModel: CreateAccountViewModel by viewModels()
    private lateinit var binding: AccountActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(theme.activeTheme.resourceId)

        binding = AccountActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (savedInstanceState == null) {
            val navController = findNavController(R.id.nav_host_fragment)
            val navInflater = navController.navInflater
            val graph = navInflater.inflate(R.navigation.account_nav_graph)
            val arguments = Bundle()

            val accountAuthenticatorResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, AccountAuthenticatorResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
            }
            if (accountAuthenticatorResponse != null) {
                graph.setStartDestination(R.id.signInFragment)
            } else if (isNewUpgradeInstance(intent)) {
                viewModel.clearReadyForUpgrade()
                graph.setStartDestination(R.id.createFrequencyFragment)
            } else if (isPromoCodeInstance(intent)) {
                graph.setStartDestination(R.id.promoCodeFragment)
                arguments.putString(PromoCodeFragment.ARG_PROMO_CODE, intent.getStringExtra(PROMO_CODE_VALUE))
            } else if (isSignInInstance(intent)) {
                graph.setStartDestination(R.id.signInFragment)

                val successIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(SUCCESS_INTENT, Parcelable::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(SUCCESS_INTENT)
                }
                arguments.putParcelable(SignInFragment.EXTRA_SUCCESS_INTENT, successIntent)
            } else {
                if (isNewAutoSelectPlusInstance(intent)) {
                    viewModel.defaultSubscriptionType = SubscriptionType.PLUS
                }
                viewModel.clearValues()
                graph.setStartDestination(R.id.accountFragment)
            }

            viewModel.supporterInstance = intent.getBooleanExtra(SUPPORTER_INTENT, false)

            navController.setGraph(graph, arguments)

            val navConfiguration = AppBarConfiguration(navController.graph)
            binding.toolbar?.setupWithNavController(navController, navConfiguration)
            binding.toolbar?.setNavigationOnClickListener { _ -> onBackPressed() }
            binding.carHeader?.btnClose?.setOnClickListener { onBackPressed() }

            navController.addOnDestinationChangedListener { _, destination, _ ->
                destination.trackShown()
                if (!Util.isCarUiMode(this)) {
                    when (destination.id) {
                        R.id.createDoneFragment, R.id.accountFragment, R.id.promoCodeFragment -> {
                            binding.toolbar?.isVisible = false
                        }
                        else -> {
                            binding.toolbar?.isVisible = true
                        }
                    }
                } else {
                    val resource = when (destination.id) {
                        R.id.createDoneFragment, R.id.accountFragment, R.id.promoCodeFragment -> {
                            IR.drawable.ic_close
                        }
                        else -> {
                            IR.drawable.ic_arrow_back
                        }
                    }
                    binding.carHeader?.btnClose?.setImageResource(resource)
                }
            }
        }
    }

    override fun onBackPressed() {
        val currentFragment = findNavController(R.id.nav_host_fragment).currentDestination
        currentFragment?.trackDismissed()
        if (currentFragment?.id == R.id.createPayNowFragment || currentFragment?.id == R.id.createDoneFragment) {
            finish()
            return
        }
        if (currentFragment?.id == R.id.accountFragment) {
            FirebaseAnalyticsTracker.closeAccountMissingClicked()
        }

        UiUtil.hideKeyboard(binding.root)
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    private fun NavDestination.trackShown() {
        val analyticsEvent = when (id) {
            R.id.accountFragment -> AnalyticsEvent.SETUP_ACCOUNT_SHOWN
            R.id.signInFragment -> AnalyticsEvent.SIGNIN_SHOWN
            R.id.createAccountFragment -> AnalyticsEvent.SELECT_ACCOUNT_TYPE_SHOWN
            R.id.createEmailFragment -> AnalyticsEvent.CREATE_ACCOUNT_SHOWN
            R.id.createTOSFragment -> AnalyticsEvent.TERMS_OF_USE_SHOWN
            R.id.createFrequencyFragment -> AnalyticsEvent.SELECT_PAYMENT_FREQUENCY_SHOWN
            R.id.createPayNowFragment -> AnalyticsEvent.CONFIRM_PAYMENT_SHOWN
            R.id.resetPasswordFragment -> AnalyticsEvent.FORGOT_PASSWORD_SHOWN
            R.id.createDoneFragment -> AnalyticsEvent.ACCOUNT_UPDATED_SHOWN
            else -> null
        }
        val properties = when (id) {
            R.id.createPayNowFragment -> {
                val subscription = viewModel.subscription.value
                subscription?.let { mapOf(PRODUCT_KEY to it.productDetails.productId) }
            }
            R.id.createDoneFragment -> {
                val source = when (viewModel.createAccountState.value) {
                    CreateAccountState.AccountCreated -> AccountUpdatedSource.CREATE_ACCOUNT.analyticsValue
                    CreateAccountState.SubscriptionCreated -> AccountUpdatedSource.CONFIRM_PAYMENT.analyticsValue
                    else -> null
                }
                source?.let { mapOf(SOURCE_KEY to source) }
            }
            R.id.accountFragment -> mapOf(SOURCE_KEY to ACCOUNT_PROP_VALUE)
            else -> null
        } ?: emptyMap()
        analyticsEvent?.let { analyticsTracker.track(it, properties) }
    }

    private fun NavDestination.trackDismissed() {
        val analyticsEvent = when (id) {
            R.id.accountFragment -> AnalyticsEvent.SETUP_ACCOUNT_DISMISSED
            R.id.signInFragment -> AnalyticsEvent.SIGNIN_DISMISSED
            R.id.createAccountFragment -> AnalyticsEvent.SELECT_ACCOUNT_TYPE_DISMISSED
            R.id.createEmailFragment -> AnalyticsEvent.CREATE_ACCOUNT_DISMISSED
            R.id.createTOSFragment -> AnalyticsEvent.TERMS_OF_USE_DISMISSED
            R.id.createFrequencyFragment -> AnalyticsEvent.SELECT_PAYMENT_FREQUENCY_DISMISSED
            R.id.createPayNowFragment -> AnalyticsEvent.CONFIRM_PAYMENT_DISMISSED
            R.id.resetPasswordFragment -> AnalyticsEvent.FORGOT_PASSWORD_DISMISSED
            R.id.createDoneFragment -> AnalyticsEvent.ACCOUNT_UPDATED_DISMISSED
            else -> null
        }

        val properties = when (id) {
            R.id.accountFragment -> mapOf(SOURCE_KEY to ACCOUNT_PROP_VALUE)
            else -> emptyMap()
        }

        analyticsEvent?.let { analyticsTracker.track(it, properties) }
    }

    companion object {
        const val SKIP_FIRST = "account_activity.skip_first"
        fun newUpgradeInstance(context: Context?): Intent {
            val intent = Intent(context, AccountActivity::class.java)
            intent.putExtra(SKIP_FIRST, true)
            return intent
        }
        fun isNewUpgradeInstance(intent: Intent): Boolean {
            return intent.getBooleanExtra(SKIP_FIRST, false)
        }

        const val AUTO_SELECT_PLUS = "account_activity.autoSelectPlus"
        fun newAutoSelectPlusInstance(context: Context?): Intent {
            val intent = Intent(context, AccountActivity::class.java)
            intent.putExtra(AUTO_SELECT_PLUS, true)
            return intent
        }
        fun isNewAutoSelectPlusInstance(intent: Intent): Boolean {
            return intent.getBooleanExtra(AUTO_SELECT_PLUS, false)
        }
        private const val PRODUCT_KEY = "product"
        private const val SOURCE_KEY = "source"
        private const val ACCOUNT_PROP_VALUE = "account"
        const val IS_PROMO_CODE = "account_activity.is_promo_code"
        const val PROMO_CODE_VALUE = "account_activity.promo_code"
        const val PROMO_CODE_RETURN_DESCRIPTION = "account_activity.promo_code_return_description"
        const val SIGN_IN_ONLY = "account_activity.sign_in_only"
        const val SUCCESS_INTENT = "account_activity.success_intent"
        const val SUPPORTER_INTENT = "account_activity.supporter"

        fun promoCodeInstance(context: Context?, code: String): Intent {
            val intent = Intent(context, AccountActivity::class.java)
            intent.putExtra(IS_PROMO_CODE, true)
            intent.putExtra(PROMO_CODE_VALUE, code)
            return intent
        }

        fun isPromoCodeInstance(intent: Intent): Boolean {
            return intent.getBooleanExtra(IS_PROMO_CODE, false)
        }

        fun isSignInInstance(intent: Intent): Boolean {
            return intent.getBooleanExtra(SIGN_IN_ONLY, false)
        }
    }

    enum class AccountUpdatedSource(val analyticsValue: String) {
        CREATE_ACCOUNT("create_account"),
        CONFIRM_PAYMENT("confirm_payment"),
        CHANGE_EMAIL("change_email"),
        CHANGE_PASSWORD("change_password"),
    }
}
