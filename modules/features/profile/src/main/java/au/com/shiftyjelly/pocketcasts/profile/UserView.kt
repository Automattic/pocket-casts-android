package au.com.shiftyjelly.pocketcasts.profile

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.currentStateAsState
import au.com.shiftyjelly.pocketcasts.account.ProfileCircleView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.themeTypeToColors
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralDaysMonthsOrYears
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralSecondsMinutesHoursDaysOrYears
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import au.com.shiftyjelly.pocketcasts.utils.TimeConstants
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.days
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatLongStyle
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.gravatar.api.models.Profile
import com.gravatar.services.ProfileService
import com.gravatar.services.Result
import com.gravatar.types.Email
import com.gravatar.ui.GravatarTheme
import com.gravatar.ui.LocalGravatarTheme
import com.gravatar.ui.components.ComponentState
import com.gravatar.ui.components.LargeProfileSummary
import com.gravatar.ui.components.atomic.ViewProfileButton
import java.util.Date
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

open class UserView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {
    open val layoutResource = R.layout.view_user

    var signedInState: SignInState? = null
        set(value) {
            field = value
            update(value)
        }

    var accountStartDate: Date = Date()
    val maxSubscriptionExpiryMs = 30L * 24L * 60L * 60L * 1000L
    val lblUserEmail: TextView
    val lblSignInStatus: TextView?
    var imgProfilePicture: ProfileCircleView
    val btnAccount: Button?
    private val subscriptionBadge: ComposeView?
    private val isDarkTheme: Boolean
        get() = Theme.isDark(context)

    init {
        LayoutInflater.from(context).inflate(layoutResource, this, true)
        lblUserEmail = findViewById(R.id.lblUserEmail)
        lblSignInStatus = findViewById(R.id.lblSignInStatus)
        imgProfilePicture = findViewById(R.id.imgProfilePicture)
        btnAccount = findViewById(R.id.btnAccount)
        subscriptionBadge = findViewById(R.id.subscriptionBadge)
    }

    open fun update(signInState: SignInState?) {
        updateProfileImageAndDaysRemaining(signInState = signInState)
        updateEmail(signInState)
        updateSubscriptionBadge(signInState)
        updateAccountButton(signInState)
    }

    fun updateProfileImageAndDaysRemaining(
        signInState: SignInState?,
    ) {
        when (signInState) {
            is SignInState.SignedIn -> {
                val gravatarUrl = Gravatar.getUrl(signInState.email)
                var percent = 1.0f
                val daysLeft = daysLeft(signInState, 30)
                if (daysLeft != null && daysLeft > 0 && daysLeft <= 30) {
                    percent = daysLeft / 30f
                }
                imgProfilePicture.setup(
                    percent = percent,
                    plusOnly = signInState.isSignedInAsPlus,
                    isPatron = signInState.isSignedInAsPatron,
                    gravatarUrl = gravatarUrl,
                )
            }
            is SignInState.SignedOut -> imgProfilePicture.setup(
                percent = 0.0f,
                plusOnly = false,
                isPatron = false,
            )
            else -> imgProfilePicture.setup(
                percent = 0.0f,
                plusOnly = false,
                isPatron = false,
            )
        }
    }

    private fun daysLeft(signInState: SignInState.SignedIn, maxDays: Int): Int? {
        val timeInXDays = Date(Date().time + maxDays.days())
        val paidStatus = signInState.subscriptionStatus as? SubscriptionStatus.Paid
        if (paidStatus != null && paidStatus.expiry.before(timeInXDays)) {
            // probably shouldn't be do straight millisecond maths because of day light savings
            return ((paidStatus.expiry.time - Date().time) / TimeConstants.MILLISECONDS_IN_ONE_DAY).toInt()
        }
        return null
    }

    private fun setDaysRemainingTextIfNeeded(signInState: SignInState.SignedIn) {
        val status = ((signInState as? SignInState.SignedIn)?.subscriptionStatus as? SubscriptionStatus.Paid) ?: return
        if (status.autoRenew) {
            return
        }

        val timeLeftMs = status.expiry.time - Date().time
        if (timeLeftMs <= 0) {
            return
        }

        if (timeLeftMs <= maxSubscriptionExpiryMs) {
            val expiresIn = resources.getStringPluralSecondsMinutesHoursDaysOrYears(timeLeftMs)
            val messagesRes = if (signInState.isSignedInAsPatron) LR.string.profile_patron_expires_in else LR.string.profile_plus_expires_in
            lblUserEmail.text = context.getString(messagesRes, expiresIn).uppercase()
            lblUserEmail.setTextColor(lblUserEmail.context.getThemeColor(UR.attr.support_05))
        }
    }

    private fun updateEmail(signInState: SignInState?) {
        when (signInState) {
            is SignInState.SignedIn -> {
                lblUserEmail.text = signInState.email
                lblUserEmail.visibility = View.VISIBLE
                lblUserEmail.setTextColor(context.getThemeColor(UR.attr.primary_text_01))

                if (this !is ExpandedUserView) setDaysRemainingTextIfNeeded(signInState)
            }
            is SignInState.SignedOut -> {
                lblUserEmail.text = context.getString(LR.string.profile_set_up_account)
                lblUserEmail.visibility = View.GONE
            }
            null -> lblUserEmail.text = null
        }
    }

    private fun updateSubscriptionBadge(signInState: SignInState?) {
        val fontSize = if (Util.isAutomotive(context)) 20.sp else 14.sp
        val iconSize = if (Util.isAutomotive(context)) 20.dp else 14.dp
        val padding = if (Util.isAutomotive(context)) 6.dp else 4.dp
        subscriptionBadge?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme(if (isDarkTheme) Theme.ThemeType.DARK else Theme.ThemeType.LIGHT) {
                    if (signInState is SignInState.SignedIn) {
                        val isExpandedUserView = this@UserView is ExpandedUserView
                        val modifier = Modifier.padding(top = 16.dp)
                        if (signInState.isSignedInAsPatron) {
                            SubscriptionBadge(
                                iconRes = IR.drawable.ic_patron,
                                shortNameRes = LR.string.pocket_casts_patron_short,
                                iconColor = if (!isExpandedUserView) Color.White else Color.Unspecified,
                                backgroundColor = if (!isExpandedUserView) colorResource(UR.color.patron_purple) else null,
                                textColor = if (!isExpandedUserView) colorResource(UR.color.patron_purple_light) else null,
                                modifier = if (isExpandedUserView) modifier else Modifier,
                                iconSize = iconSize,
                                fontSize = fontSize,
                                padding = padding,
                            )
                        } else if (signInState.isSignedInAsPlus && isExpandedUserView) {
                            SubscriptionBadge(
                                iconRes = IR.drawable.ic_plus,
                                shortNameRes = LR.string.pocket_casts_plus_short,
                                iconColor = colorResource(UR.color.plus_gold),
                                iconSize = iconSize,
                                fontSize = fontSize,
                                padding = padding,
                                modifier = modifier,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateAccountButton(signInState: SignInState?) {
        btnAccount?.text = when (signInState) {
            is SignInState.SignedIn -> context.getString(LR.string.profile_account)
            is SignInState.SignedOut -> context.getString(LR.string.profile_set_up_account)
            else -> null
        }
    }
}

class ExpandedUserView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : UserView(context, attrs, defStyleAttr) {
    override val layoutResource: Int
        get() = R.layout.view_expanded_user
    val lblPaymentStatus: TextView
        get() = findViewById(R.id.lblPaymentStatus)

    // For Gravatar Profile
    var appTheme: Theme? = null
    private val gravatarProfileCard: ComposeView
        get() = findViewById(R.id.gravatarProfileCard)
    private val legacyImgProfilePicture: ProfileCircleView = imgProfilePicture

    init {
        if (FeatureFlag.isEnabled(Feature.GRAVATAR_PROFILE)) {
            imgProfilePicture = ProfileCircleView(context)
        }
    }

    override fun update(signInState: SignInState?) {
        super.update(signInState)

        val status = (signInState as? SignInState.SignedIn)?.subscriptionStatus ?: return

        if (FeatureFlag.isEnabled(Feature.GRAVATAR_PROFILE)) {
            gravatarProfileCard.visibility = View.VISIBLE
            legacyImgProfilePicture.visibility = View.GONE
            setGravatarProfileCard(signInState)
        }

        when (status) {
            is SubscriptionStatus.Free -> {
                lblPaymentStatus.text = context.getString(LR.string.profile_free_account)
                lblSignInStatus?.text = ""
            }
            is SubscriptionStatus.Paid -> {
                val activeSubscription = status.subscriptions.getOrNull(status.index)
                if (activeSubscription == null ||
                    activeSubscription.tier in listOf(
                        SubscriptionTier.PATRON,
                        SubscriptionTier.PLUS,
                    )
                ) {
                    setupLabelsForPaidUser(status, signInState)
                } else {
                    setupLabelsForSupporter(activeSubscription)
                }
            }
        }
    }

    private fun setupLabelsForPaidUser(status: SubscriptionStatus.Paid, signInState: SignInState) {
        if (status.autoRenew) {
            val strMonthly = context.getString(LR.string.profile_monthly)
            val strYearly = context.getString(LR.string.profile_yearly)
            lblPaymentStatus.text = context.getString(LR.string.profile_next_payment, status.expiry.toLocalizedFormatLongStyle())
            lblSignInStatus?.text = when (status.frequency) {
                SubscriptionFrequency.MONTHLY -> strMonthly
                SubscriptionFrequency.YEARLY -> strYearly
                else -> null
            }
            lblSignInStatus?.setTextColor(context.getThemeColor(UR.attr.primary_text_02))
        } else {
            if (status.platform == SubscriptionPlatform.GIFT) {
                if (signInState.isLifetimePlus) {
                    lblPaymentStatus.text = context.resources.getString(LR.string.plus_thanks_for_your_support_bang)
                } else {
                    val giftDaysString = context.resources.getStringPluralDaysMonthsOrYears(status.giftDays)
                    lblPaymentStatus.text = context.resources.getString(LR.string.profile_time_free, giftDaysString)
                }
            } else {
                lblPaymentStatus.text = context.getString(LR.string.profile_payment_cancelled)
            }

            if (signInState.isLifetimePlus) {
                lblSignInStatus?.text = context.resources.getString(LR.string.plus_lifetime_member)
                lblSignInStatus?.setTextColor(lblSignInStatus.context.getThemeColor(UR.attr.support_02))
            } else {
                lblSignInStatus?.text = context.getString(LR.string.profile_plus_expires, status.expiry.toLocalizedFormatLongStyle())
                lblSignInStatus?.setTextColor(lblSignInStatus.context.getThemeColor(UR.attr.primary_text_02))
            }
        }
    }

    private fun setupLabelsForSupporter(subscription: SubscriptionStatus.Subscription) {
        if (subscription.autoRenewing) {
            lblPaymentStatus.text = context.getString(LR.string.supporter)
            lblPaymentStatus.setTextColor(lblPaymentStatus.context.getThemeColor(UR.attr.support_02))

            lblSignInStatus?.text = context.getString(LR.string.supporter_check_contributions)
            lblSignInStatus?.setTextColor(context.getThemeColor(UR.attr.primary_text_02))
        } else {
            lblPaymentStatus.text = context.getString(LR.string.supporter_payment_cancelled)
            lblPaymentStatus.setTextColor(lblPaymentStatus.context.getThemeColor(UR.attr.support_05))

            val expiryDate = subscription.expiryDate?.let { it.toLocalizedFormatLongStyle() } ?: context.getString(LR.string.profile_expiry_date_unknown)
            lblSignInStatus?.text = context.getString(LR.string.supporter_subscription_ends, expiryDate)
            lblSignInStatus?.setTextColor(context.getThemeColor(UR.attr.primary_text_02))
        }
    }

    private fun setGravatarProfileCard(signInState: SignInState.SignedIn) {
        gravatarProfileCard.setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
            val scope = rememberCoroutineScope()
            val profileService = ProfileService()
            var profileState: ComponentState<Profile> by remember { mutableStateOf(ComponentState.Loading, neverEqualPolicy()) }

            val colorScheme = appTheme?.activeTheme?.let { themeTypeToColorsScheme(it) } ?: lightColorScheme()

            CompositionLocalProvider(
                LocalGravatarTheme provides object : GravatarTheme {
                    // Override theme colors
                    override val colorScheme: ColorScheme
                        @Composable
                        get() = colorScheme
                },
            ) {
                LaunchedEffect(lifecycleState) {
                    if (lifecycleState == Lifecycle.State.RESUMED) {
                        scope.launch {
                            when (val result = profileService.fetch(Email(signInState.email))) {
                                is Result.Success -> {
                                    result.value.let {
                                        profileState = ComponentState.Loaded(it)
                                    }
                                }

                                is Result.Failure -> {
                                    // TODO: Handle error
                                    profileState = ComponentState.Empty
                                }
                            }
                        }
                    }
                }
                GravatarProfileCard(profileState = profileState, signInState = signInState)
            }
        }
    }

    @Composable
    private fun GravatarProfileCard(profileState: ComponentState<Profile>, signInState: SignInState.SignedIn) {
        GravatarTheme {
            Surface(
                modifier = Modifier
                    .padding(vertical = 24.dp, horizontal = 32.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    when (profileState) {
                        is ComponentState.Loaded -> {
                            LargeProfileSummary(
                                profileState,
                                avatar = { PocketCastAvatar(signInState) },
                                viewProfile = {
                                    ViewProfileButton(
                                        profileState.loadedValue,
                                        buttonText = stringResource(id = LR.string.profile_gravatar_edit),
                                    )
                                },
                            )
                        }

                        ComponentState.Loading -> LargeProfileSummary(profileState, Modifier.padding(top = 16.dp))
                        ComponentState.Empty,
                        -> {
                            LargeProfileSummary(
                                profileState,
                                avatar = { PocketCastAvatar(signInState) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PocketCastAvatar(signInState: SignInState.SignedIn) {
        var updated by remember { mutableStateOf(false) }
        AndroidView(
            factory = { _ -> imgProfilePicture },
            modifier = Modifier
                .size(132.dp)
                .onGloballyPositioned {
                    if (!updated) {
                        updated = true
                        updateProfileImageAndDaysRemaining(signInState)
                    }
                }
                .padding(8.dp),
        )
    }

    @Composable
    private fun themeTypeToColorsScheme(themeType: Theme.ThemeType): ColorScheme {
        return themeTypeToColors(themeType).let { colors ->
            val baseColorScheme = if (themeType.darkTheme) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }

            baseColorScheme.copy(
                primary = colors.primaryInteractive01,
                secondary = colors.primaryInteractive01,
                surface = colors.primaryUi01,
                error = colors.support05,
                onPrimary = colors.primaryInteractive02,
                onSecondary = colors.primaryInteractive02,
                onBackground = colors.primaryInteractive01,
                onSurface = colors.primaryInteractive01,
                onError = colors.secondaryIcon01,
            )
        }
    }
}
