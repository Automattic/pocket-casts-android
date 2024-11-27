package au.com.shiftyjelly.pocketcasts.profile

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.widget.ConstraintLayout
import au.com.shiftyjelly.pocketcasts.account.ProfileCircleView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import au.com.shiftyjelly.pocketcasts.utils.TimeConstants
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.days
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class UserView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {
    var signedInState: SignInState? = null
        set(value) {
            field = value
            update(value)
        }

    val lblUserEmail: TextView
    val imgProfilePicture: ProfileCircleView
    val btnAccount: Button?
    private val subscriptionBadge: ComposeView?
    private val isDarkTheme: Boolean
        get() = Theme.isDark(context)

    init {
        LayoutInflater.from(context).inflate(R.layout.view_user, this, true)
        lblUserEmail = findViewById(R.id.lblUserEmail)
        imgProfilePicture = findViewById(R.id.imgProfilePicture)
        btnAccount = findViewById(R.id.btnAccount)
        subscriptionBadge = findViewById(R.id.subscriptionBadge)
    }

    fun update(signInState: SignInState?) {
        updateProfileImageAndDaysRemaining(signInState)
        updateEmail(signInState)
        updateSubscriptionBadge(signInState)
        updateAccountButton(signInState)
    }

    private fun updateProfileImageAndDaysRemaining(
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
        if (paidStatus != null && paidStatus.expiryDate.before(timeInXDays)) {
            // probably shouldn't be do straight millisecond maths because of day light savings
            return ((paidStatus.expiryDate.time - Date().time) / TimeConstants.MILLISECONDS_IN_ONE_DAY).toInt()
        }
        return null
    }

    private fun updateEmail(signInState: SignInState?) {
        when (signInState) {
            is SignInState.SignedIn -> {
                lblUserEmail.text = signInState.email
                lblUserEmail.visibility = View.VISIBLE
                lblUserEmail.setTextColor(context.getThemeColor(UR.attr.primary_text_01))
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
        subscriptionBadge?.setContentWithViewCompositionStrategy {
            AppTheme(if (isDarkTheme) Theme.ThemeType.DARK else Theme.ThemeType.LIGHT) {
                if (signInState is SignInState.SignedIn) {
                    val modifier = Modifier.padding(top = 16.dp)
                    if (signInState.isSignedInAsPatron) {
                        SubscriptionBadge(
                            iconRes = IR.drawable.ic_patron,
                            shortNameRes = LR.string.pocket_casts_patron_short,
                            iconColor = Color.White,
                            backgroundColor = colorResource(UR.color.patron_purple),
                            textColor = colorResource(UR.color.patron_purple_light),
                            modifier = modifier,
                            iconSize = iconSize,
                            fontSize = fontSize,
                            padding = padding,
                        )
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
