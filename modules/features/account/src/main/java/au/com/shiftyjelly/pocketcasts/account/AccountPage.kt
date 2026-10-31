package au.com.shiftyjelly.pocketcasts.account

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.res.use
import au.com.shiftyjelly.pocketcasts.compose.buttons.ViewMaterialButton
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.cartheme.R as CR
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

internal data class AccountPageState(
    val isSignedIn: Boolean,
    val isCar: Boolean,
    val isLandscape: Boolean,
)

@Composable
internal fun AccountPage(
    state: AccountPageState,
    onCloseClick: () -> Unit,
    onCreateClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    continueWithGoogle: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.theme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryUi01),
    ) {
        if (!state.isCar) {
            AccountHeader(onCloseClick)
        }
        CenteredScroll(modifier = Modifier.weight(1f)) {
            AccountIntro(state, continueWithGoogle, onCreateClick, onSignInClick)
        }
        if (!state.isCar) {
            ViewMaterialButton(
                text = stringResource(if (state.isSignedIn) LR.string.done else LR.string.create_account),
                onClick = onCreateClick,
                backgroundColor = colors.primaryInteractive01,
                textColor = colors.primaryInteractive02,
                fontSize = 18.sp,
                letterSpacing = 0.02.em,
                cornerRadius = 8.dp,
                verticalPadding = 16.dp,
                modifier = Modifier
                    .padding(start = 16.dp, top = 24.dp, end = 16.dp)
                    .fillMaxWidth(),
            )
            if (!state.isSignedIn) {
                ViewMaterialButton(
                    text = stringResource(LR.string.sign_in),
                    onClick = onSignInClick,
                    backgroundColor = Color.Transparent,
                    textColor = colors.primaryInteractive01,
                    fontSize = 18.sp,
                    letterSpacing = 0.02.em,
                    cornerRadius = 8.dp,
                    elevation = 0.dp,
                    verticalPadding = 16.dp,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AccountHeader(onCloseClick: () -> Unit) {
    val context = LocalContext.current
    val logo = remember(context) {
        context.obtainStyledAttributes(intArrayOf(UR.attr.logo_title_horizontal)).use { it.getResourceId(0, 0) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp)
                .size(48.dp)
                .clickable(interactionSource = null, indication = ripple(bounded = false), role = Role.Button, onClick = onCloseClick),
        ) {
            DrawableImage(
                drawableId = IR.drawable.ic_cancel,
                contentDescription = stringResource(LR.string.cancel),
                tint = MaterialTheme.theme.colors.primaryIcon01,
            )
        }
        if (logo != 0) {
            DrawableImage(
                drawableId = logo,
                contentDescription = null,
                modifier = Modifier
                    .align(FloorCenter)
                    .size(width = 170.dp, height = 34.dp),
            )
        }
    }
}

@Composable
private fun AccountIntro(
    state: AccountPageState,
    continueWithGoogle: @Composable () -> Unit,
    onCreateClick: () -> Unit,
    onSignInClick: () -> Unit,
) {
    val colors = MaterialTheme.theme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val iconModifier = when {
            state.isLandscape -> Modifier.size(0.dp)
            state.isCar -> Modifier.size(360.dp)
            else -> Modifier.size(width = 171.dp, height = 160.dp)
        }
        AccountGradientIcon(
            drawableId = if (state.isSignedIn) IR.drawable.ic_alert_small else R.drawable.ic_create_new_account,
            tintColor = if (state.isSignedIn) colors.support05 else null,
            modifier = iconModifier,
        )
        val titleTop = if (state.isCar) dimensionResource(R.dimen.car_create_heading_top_margin) else 16.dp
        AccountText(
            text = stringResource(if (state.isSignedIn) LR.string.profile_alreadysignedin else LR.string.profile_sign_in_or_create_account),
            fontSize = if (state.isCar) 56.sp else 22.sp,
            fontWeight = if (state.isCar) FontWeight.Normal else FontWeight.Medium,
            color = colors.primaryText01,
            modifier = Modifier
                .padding(start = 24.dp, top = titleTop, end = 24.dp)
                .then(if (state.isCar) Modifier else Modifier.fillMaxWidth()),
        )
        AccountText(
            text = stringResource(if (state.isSignedIn) LR.string.profile_alreadysignedindescription else LR.string.profile_save_your_podcasts),
            fontSize = if (state.isCar) 32.sp else 15.sp,
            fontWeight = if (state.isCar) FontWeight.Normal else FontWeight.Medium,
            color = colors.primaryText02,
            modifier = Modifier
                .padding(start = 24.dp, top = if (state.isCar) 32.dp else 8.dp, end = 24.dp)
                .then(if (state.isCar) Modifier.width(670.dp) else Modifier.fillMaxWidth()),
        )
        if (state.isCar) {
            CarButtons(state, continueWithGoogle, onCreateClick, onSignInClick)
        }
    }
}

@Composable
private fun CarButtons(
    state: AccountPageState,
    continueWithGoogle: @Composable () -> Unit,
    onCreateClick: () -> Unit,
    onSignInClick: () -> Unit,
) {
    val colors = MaterialTheme.theme.colors
    val textSize = dimensionResource(CR.dimen.car_body2_size).value.sp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = dimensionResource(R.dimen.car_create_account_btn_top_margin), end = 16.dp)
                .width(592.dp),
        ) {
            continueWithGoogle()
        }
        ViewMaterialButton(
            text = stringResource(if (state.isSignedIn) LR.string.done else LR.string.create_account),
            onClick = onCreateClick,
            backgroundColor = colors.primaryInteractive01,
            textColor = colors.primaryInteractive02,
            fontSize = textSize,
            letterSpacing = 0.02.em,
            cornerRadius = 8.dp,
            verticalPadding = 16.dp,
            modifier = Modifier
                .padding(start = 16.dp, top = 32.dp, end = 16.dp)
                .width(592.dp),
        )
        if (!state.isSignedIn) {
            ViewMaterialButton(
                text = stringResource(LR.string.sign_in),
                onClick = onSignInClick,
                backgroundColor = Color.Transparent,
                textColor = colors.primaryInteractive01,
                fontSize = textSize,
                letterSpacing = 0.02.em,
                cornerRadius = 8.dp,
                elevation = 0.dp,
                verticalPadding = 16.dp,
                modifier = Modifier
                    .padding(start = 16.dp, top = 32.dp, end = 16.dp)
                    .width(592.dp),
            )
        }
    }
}

@Composable
private fun AccountText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier,
) {
    TextViewText(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun AccountGradientIcon(drawableId: Int, tintColor: Color?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val drawable = remember(context, drawableId, tintColor) {
        AppCompatResources.getDrawable(context, drawableId)?.mutate()?.apply {
            if (tintColor != null) setTint(tintColor.toArgb())
        }
    }
    val start = MaterialTheme.theme.colors.gradient02A.toArgb()
    val end = MaterialTheme.theme.colors.gradient02E.toArgb()
    Box(
        modifier = modifier.drawWithCache {
            val w = size.width.toInt()
            val h = size.height.toInt()
            val bitmap = if (drawable != null && w > 0 && h > 0) gradientBitmap(drawable, w, h, start, end) else null
            onDrawBehind {
                bitmap?.let { drawContext.canvas.nativeCanvas.drawBitmap(it, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG)) }
            }
        },
    )
}

private fun gradientBitmap(drawable: Drawable, width: Int, height: Int, start: Int, end: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    val paint = Paint().apply {
        shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), start, end, Shader.TileMode.CLAMP)
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    return bitmap
}

@Composable
private fun CenteredScroll(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val viewport = constraints.maxHeight
        Layout(
            content = content,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) { measurables, constraints ->
            val child = measurables.first().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
            layout(constraints.maxWidth, maxOf(viewport, child.height)) {
                child.place((constraints.maxWidth - child.width) / 2, (viewport - child.height) / 2)
            }
        }
    }
}

private val FloorCenter = Alignment { size, space, _ -> IntOffset((space.width - size.width) / 2, (space.height - size.height) / 2) }
