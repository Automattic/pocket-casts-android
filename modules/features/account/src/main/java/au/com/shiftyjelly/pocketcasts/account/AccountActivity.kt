package au.com.shiftyjelly.pocketcasts.account

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import au.com.shiftyjelly.pocketcasts.account.databinding.AccountActivityBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SubscriptionType
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import com.automattic.eventhorizon.AccountUpdatedDismissedEvent
import com.automattic.eventhorizon.AccountUpdatedShownEvent
import com.automattic.eventhorizon.CreateAccountDismissedLegacyEvent
import com.automattic.eventhorizon.CreateAccountShownLegacyEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.ForgotPasswordDismissedEvent
import com.automattic.eventhorizon.ForgotPasswordShownEvent
import com.automattic.eventhorizon.SetupAccountDismissedLegacyEvent
import com.automattic.eventhorizon.SetupAccountShownLegacyEvent
import com.automattic.eventhorizon.SigninDismissedEvent
import com.automattic.eventhorizon.SigninShownEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR

@AndroidEntryPoint
class AccountActivity : AppCompatActivity() {

    @Inject lateinit var theme: Theme

    @Inject lateinit var eventHorizon: EventHorizon

    private val viewModel: CreateAccountViewModel by viewModels()

    private lateinit var binding: AccountActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.setupThemeForConfig(this, resources.configuration)
        enableEdgeToEdge(navigationBarStyle = theme.getNavigationBarStyle(this))

        binding = AccountActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            binding.root.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = insets.bottom,
                top = insets.top,
            )
            windowInsets
        }

        val navController = findNavController(R.id.nav_host_fragment)
        binding.carHeader?.btnClose?.setOnClickListener {
            if (!navController.popBackStack()) {
                finish()
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackNavigation(navController)
                }
            },
        )

        if (savedInstanceState == null) {
            val navInflater = navController.navInflater
            val graph = navInflater.inflate(R.navigation.account_nav_graph)
            val arguments = Bundle()

            // Temporary workaround that that can be removed after the upgrade to Android 11 in August 2023.
            val navigateToSignIn = Build.MANUFACTURER.lowercase() == "mercedes-benz" && Build.VERSION.SDK_INT < Build.VERSION_CODES.R

            val accountAuthenticatorResponse = IntentCompat.getParcelableExtra(intent, KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, AccountAuthenticatorResponse::class.java)
            if (accountAuthenticatorResponse != null || navigateToSignIn) {
                graph.setStartDestination(R.id.signInFragment)
            } else if (isPromoCodeInstance(intent)) {
                graph.setStartDestination(R.id.promoCodeFragment)
                arguments.putString(PromoCodeFragment.ARG_PROMO_CODE, intent.getStringExtra(PROMO_CODE_VALUE))
            } else {
                if (isNewAutoSelectPlusInstance(intent)) {
                    viewModel.defaultSubscriptionType = SubscriptionType.PLUS
                }
                viewModel.clearValues()
                graph.setStartDestination(R.id.accountFragment)
            }

            navController.setGraph(graph, arguments)

            val navConfiguration = AppBarConfiguration(navController.graph)
            binding.toolbar?.setupWithNavController(navController, navConfiguration)
            binding.toolbar?.setNavigationOnClickListener { _ ->
                handleBackNavigation(navController)
            }

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

    private fun handleBackNavigation(navController: NavController) {
        val currentFragment = navController.currentDestination
        currentFragment?.trackDismissed()

        UiUtil.hideKeyboard(binding.root)
        if (currentFragment?.id == R.id.createDoneFragment || !navController.popBackStack()) {
            finish()
        }
    }

    private fun NavDestination.trackShown() {
        val analyticsEvent = when (id) {
            R.id.accountFragment -> SetupAccountShownLegacyEvent

            R.id.signInFragment -> SigninShownEvent

            R.id.createEmailFragment -> CreateAccountShownLegacyEvent

            R.id.resetPasswordFragment -> ForgotPasswordShownEvent

            R.id.createDoneFragment -> AccountUpdatedShownEvent(
                source = when (viewModel.createAccountState.value) {
                    CreateAccountState.AccountCreated -> AccountUpdatedSource.CREATE_ACCOUNT.analyticsValue
                    CreateAccountState.SubscriptionCreated -> AccountUpdatedSource.CONFIRM_PAYMENT.analyticsValue
                    else -> AnalyticsTracker.INVALID_OR_NULL_VALUE
                },
            )

            else -> null
        }
        if (analyticsEvent != null) {
            eventHorizon.track(analyticsEvent)
        }
    }

    private fun NavDestination.trackDismissed() {
        val analyticsEvent = when (id) {
            R.id.accountFragment -> SetupAccountDismissedLegacyEvent
            R.id.signInFragment -> SigninDismissedEvent
            R.id.createEmailFragment -> CreateAccountDismissedLegacyEvent
            R.id.resetPasswordFragment -> ForgotPasswordDismissedEvent
            R.id.createDoneFragment -> AccountUpdatedDismissedEvent
            else -> null
        }
        if (analyticsEvent != null) {
            eventHorizon.track(analyticsEvent)
        }
    }

    companion object {
        const val SKIP_FIRST = "account_activity.skip_first"
        fun newUpgradeInstance(context: Context?): Intent {
            val intent = Intent(context, AccountActivity::class.java)
            intent.putExtra(SKIP_FIRST, true)
            return intent
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
        private const val IS_PROMO_CODE = "account_activity.is_promo_code"
        const val PROMO_CODE_VALUE = "account_activity.promo_code"
        const val PROMO_CODE_RETURN_DESCRIPTION = "account_activity.promo_code_return_description"

        fun promoCodeInstance(context: Context?, code: String): Intent {
            val intent = Intent(context, AccountActivity::class.java)
            intent.putExtra(IS_PROMO_CODE, true)
            intent.putExtra(PROMO_CODE_VALUE, code)
            return intent
        }

        fun isPromoCodeInstance(intent: Intent): Boolean {
            return intent.getBooleanExtra(IS_PROMO_CODE, false)
        }
    }

    enum class AccountUpdatedSource(val analyticsValue: String) {
        CREATE_ACCOUNT("create_account"),
        CONFIRM_PAYMENT("confirm_payment"),
        CHANGE_EMAIL("change_email"),
        CHANGE_PASSWORD("change_password"),
    }
}
