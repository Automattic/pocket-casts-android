package au.com.shiftyjelly.pocketcasts.account

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.text.InputType
import android.util.SparseArray
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.BundleCompat
import au.com.shiftyjelly.pocketcasts.compose.buttons.ViewMaterialButton
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getTintedDrawable
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.addOnTextChanged
import au.com.shiftyjelly.pocketcasts.views.extensions.showKeyboard
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import au.com.shiftyjelly.pocketcasts.cartheme.R as CR
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

internal data class SignInPageState(
    val isCar: Boolean,
    val isLandscape: Boolean,
    val isFormValid: Boolean,
    val isLoading: Boolean,
    val isProgressVisible: Boolean,
    val errorText: String?,
)

@Composable
internal fun SignInPage(
    state: SignInPageState,
    onSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
    fields: @Composable () -> Unit = {},
) {
    val background = Modifier
        .fillMaxSize()
        .background(MaterialTheme.theme.colors.primaryUi01)
    when {
        state.isCar -> CarSignIn(state, onSignInClick, onForgotPasswordClick, fields, modifier.then(background))
        state.isLandscape -> LandscapeSignIn(state, onSignInClick, onForgotPasswordClick, fields, modifier.then(background))
        else -> PortraitSignIn(state, onSignInClick, onForgotPasswordClick, fields, modifier.then(background))
    }
}

@Composable
private fun PortraitSignIn(
    state: SignInPageState,
    onSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    fields: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clipToBounds()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(modifier = Modifier.padding(top = 8.dp)) {
                fields()
            }
            PhoneErrorText(state.errorText.orEmpty())
            ForgotPasswordButton(onForgotPasswordClick, Modifier.align(Alignment.CenterHorizontally).padding(start = 8.dp, top = 8.dp, end = 8.dp))
            SignInButtonRow(state, onSignInClick, Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun LandscapeSignIn(
    state: SignInPageState,
    onSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    fields: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .clipToBounds(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(modifier = Modifier.padding(top = 16.dp)) {
                fields()
            }
            PhoneErrorText(state.errorText.orEmpty())
            ForgotPasswordButton(onForgotPasswordClick, Modifier.align(Alignment.CenterHorizontally).padding(start = 8.dp, top = 32.dp, end = 8.dp))
        }
        SignInButtonRow(state, onSignInClick)
    }
}

@Composable
private fun CarSignIn(
    state: SignInPageState,
    onSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    fields: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    BoxWithConstraints(modifier = modifier) {
        val viewportHeight = maxHeight
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (state.isLandscape) Arrangement.Top else Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 128.dp)
                .clipToBounds(),
        ) {
            TextViewText(
                text = stringResource(LR.string.profile_sign_in),
                fontSize = 56.sp,
                color = colors.primaryText01,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 24.dp, top = if (state.isLandscape) 0.dp else 44.dp, end = 24.dp),
            )
            Box(
                modifier = Modifier
                    .padding(start = 220.dp, top = if (state.isLandscape) 16.dp else 96.dp, end = 220.dp)
                    .fillMaxWidth(),
            ) {
                fields()
            }
            if (state.errorText != null || !state.isLandscape) {
                TextViewText(
                    text = state.errorText.orEmpty(),
                    fontSize = dimensionResource(CR.dimen.car_body1_size).value.sp,
                    color = colors.support05,
                    modifier = Modifier.padding(top = 8.dp, end = 4.dp),
                )
            }
            ForgotPasswordButton(
                onClick = onForgotPasswordClick,
                fontSize = if (state.isLandscape) 24.sp else 18.sp,
                letterSpacing = 0.03125.em,
                modifier = Modifier.padding(start = 8.dp, top = if (state.isLandscape) 8.dp else 120.dp, end = 8.dp),
            )
            SignInButtonRow(
                state = state,
                onClick = onSignInClick,
                modifier = Modifier.padding(top = if (state.isLandscape) 32.dp else 54.dp),
                buttonModifier = Modifier.width(591.dp),
            )
        }
    }
}

@Composable
private fun PhoneErrorText(text: String) {
    TextViewText(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.theme.colors.support05,
    )
}

@Composable
private fun ForgotPasswordButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    letterSpacing: TextUnit = 0.02.em,
) {
    ViewMaterialButton(
        text = stringResource(LR.string.profile_forgot_your_password),
        onClick = onClick,
        backgroundColor = Color.Transparent,
        textColor = MaterialTheme.theme.colors.primaryInteractive01,
        fontSize = fontSize,
        letterSpacing = letterSpacing,
        elevation = 0.dp,
        horizontalPadding = 8.dp,
        fontWeight = FontWeight.Normal,
        modifier = modifier.width(IntrinsicSize.Max),
    )
}

@Composable
private fun SignInButtonRow(
    state: SignInPageState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier.fillMaxWidth(),
) {
    val colors = MaterialTheme.theme.colors
    val enabled = state.isFormValid && !state.isLoading
    val disabledColor = Color(LocalContext.current.getThemeColor(UR.attr.primary_interactive_01_disabled))
    Box(modifier = modifier.fillMaxWidth()) {
        ViewMaterialButton(
            text = stringResource(LR.string.sign_in),
            onClick = onClick,
            backgroundColor = if (enabled) colors.primaryInteractive01 else disabledColor.compositeOver(disabledColor),
            textColor = colors.primaryInteractive02,
            fontSize = if (state.isCar) dimensionResource(CR.dimen.car_body2_size).value.sp else 18.sp,
            letterSpacing = 0.02.em,
            cornerRadius = 12.dp,
            elevation = if (enabled) 2.dp else 0.dp,
            verticalPadding = 16.dp,
            enabled = enabled,
            modifier = buttonModifier
                .align(Alignment.Center)
                .alpha(if (state.isFormValid) 1f else 0.2f),
        )
        if (state.isProgressVisible) {
            val tint = colors.primaryInteractive02.toArgb()
            AndroidView(
                factory = { context ->
                    ProgressBar(context, null, 0, androidx.appcompat.R.style.Widget_AppCompat_ProgressBar).apply {
                        isIndeterminate = true
                    }
                },
                update = { it.indeterminateTintList = ColorStateList.valueOf(tint) },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(24.dp),
            )
        }
    }
}

internal class SignInFieldsState(val restoredViewState: SparseArray<Parcelable>?) {
    var view: View? = null

    fun updateIcons(isEmailInvalid: Boolean) {
        view?.let { updateFieldIcons(it, isEmailInvalid) }
    }
}

@Composable
internal fun rememberSignInFieldsState(): SignInFieldsState {
    return rememberSaveable(saver = SignInFieldsStateSaver) { SignInFieldsState(restoredViewState = null) }
}

private val SignInFieldsStateSaver = Saver<SignInFieldsState, Bundle>(
    save = { state ->
        state.view?.let { view ->
            Bundle().apply { putSparseParcelableArray(FIELDS_STATE_KEY, SparseArray<Parcelable>().also(view::saveHierarchyState)) }
        }
    },
    restore = { bundle -> SignInFieldsState(BundleCompat.getSparseParcelableArray(bundle, FIELDS_STATE_KEY, Parcelable::class.java)) },
)

private const val FIELDS_STATE_KEY = "fields"

@Composable
internal fun SignInFields(
    state: SignInFieldsState,
    isCar: Boolean,
    isLandscape: Boolean,
    isEmailInvalid: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val currentOnEmailChange by rememberUpdatedState(onEmailChange)
    val currentOnPasswordChange by rememberUpdatedState(onPasswordChange)
    val currentOnSubmit by rememberUpdatedState(onSubmit)
    val spacing = when {
        isCar && !isLandscape -> 56.dp
        isLandscape -> 16.dp
        else -> 8.dp
    }
    AndroidView(
        factory = { context ->
            val emailLayout = signInField(context, R.id.emailLayout, if (isCar) R.style.ThemeOverlay_PocketCasts_CarTextInput else R.style.ThemeOverlay_PocketCasts_TextInput, R.id.txtEmail, LR.string.profile_email, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, isCar)
            val passwordLayout = signInField(context, R.id.passwordLayout, if (isCar) R.style.ThemeOverlay_PocketCasts_CarPasswordTextInput else R.style.ThemeOverlay_PocketCasts_PasswordTextInput, R.id.txtPwd, LR.string.profile_password, InputType.TYPE_TEXT_VARIATION_PASSWORD, isCar)
            val email = emailLayout.findViewById<TextInputEditText>(R.id.txtEmail)
            val password = passwordLayout.findViewById<TextInputEditText>(R.id.txtPwd)
            email.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    password.requestFocus()
                    return@setOnEditorActionListener true
                }
                false
            }
            password.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    currentOnSubmit()
                    return@setOnEditorActionListener true
                }
                false
            }
            if (!isCar) {
                email.showKeyboard()
            }
            email.addOnTextChanged { currentOnEmailChange(it) }
            password.addOnTextChanged { currentOnPasswordChange(it) }
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(emailLayout)
                addView(
                    passwordLayout,
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = context.dimensionPx(spacing.value)
                    },
                )
                updateFieldIcons(this, isEmailInvalid)
                state.restoredViewState?.let(::restoreHierarchyState)
                state.view = this
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun signInField(context: Context, layoutId: Int, overlay: Int, id: Int, hint: Int, variation: Int, isCar: Boolean): TextInputLayout {
    val layout = TextInputLayout(ContextThemeWrapper(context, overlay)).apply {
        this.id = layoutId
    }
    val editText = TextInputEditText(layout.context).apply {
        this.id = id
        if (isCar) {
            setTextAppearance(CR.style.TextAppearance_Car_Body2)
            letterSpacing = 0f
        }
        setTextColor(context.getThemeColor(UR.attr.primary_text_02))
        compoundDrawablePadding = context.dimensionPx(16f)
        setHint(hint)
        val typeface = typeface
        inputType = InputType.TYPE_CLASS_TEXT or variation
        this.typeface = typeface
    }
    layout.addView(editText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    return layout
}

private fun updateFieldIcons(fields: View, isEmailInvalid: Boolean) {
    val email = fields.findViewById<TextInputEditText>(R.id.txtEmail)
    val password = fields.findViewById<TextInputEditText>(R.id.txtPwd)
    val context = email.context
    val size = 32.dpToPx(context)
    fun icon(id: Int, attr: Int) = context.getTintedDrawable(id, context.getThemeColor(attr))?.apply { setBounds(0, 0, size, size) }
    val emailDrawable = icon(IR.drawable.ic_mail, UR.attr.primary_interactive_01)
    val passwordDrawable = icon(IR.drawable.ic_password, UR.attr.primary_interactive_01)
    val tickDrawable = if (!isEmailInvalid) icon(IR.drawable.ic_tick_circle, UR.attr.support_02) else null
    email.setCompoundDrawables(emailDrawable, null, tickDrawable, null)
    password.setCompoundDrawablesRelative(passwordDrawable, null, null, null)
}

private fun Context.dimensionPx(dp: Float) = (dp * resources.displayMetrics.density + 0.5f).toInt()
