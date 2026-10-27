package au.com.shiftyjelly.pocketcasts.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.ViewMaterialButton
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.account.R as AR
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun SubCancelledPage(
    expiryHint: String,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.theme.colors.primaryUi01),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            DrawableImage(
                drawableId = AR.drawable.ic_subscription_cancelled,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 16.dp, top = 32.dp, end = 16.dp)
                    .size(140.dp),
            )
            TextViewText(
                text = stringResource(LR.string.profile_sub_sorry_to_see_you_go),
                fontSize = 18.sp,
                color = MaterialTheme.theme.colors.primaryText01,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 32.dp, top = 32.dp, end = 32.dp)
                    .fillMaxWidth(),
            )
            TextViewText(
                text = stringResource(LR.string.profile_sub_thankyou_for_support),
                fontSize = 18.sp,
                color = MaterialTheme.theme.colors.primaryText02,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 32.dp, top = 8.dp, end = 32.dp, bottom = 64.dp)
                    .fillMaxWidth(),
            )
            NotesPanel(
                expiryHint = expiryHint,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            )
        }
        ViewMaterialButton(
            text = stringResource(LR.string.done),
            onClick = onDoneClick,
            backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
            textColor = MaterialTheme.theme.colors.primaryInteractive02,
            fontSize = 18.sp,
            letterSpacing = 0.02.em,
            cornerRadius = 12.dp,
            elevation = 0.dp,
            verticalPadding = 16.dp,
            strokeColor = (if (MaterialTheme.theme.isLight) Color.Black else Color.White).copy(alpha = 0.12f),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun NotesPanel(
    expiryHint: String,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.theme.colors.primaryText02
    val density = LocalDensity.current
    val strokeWidth = with(density) { 2.dp.roundToPx().toFloat() }
    val radius = with(density) { 8.dp.toPx() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    cornerRadius = CornerRadius(radius),
                    style = Stroke(strokeWidth),
                )
            }
            .padding(bottom = 32.dp),
    ) {
        NoteRow(iconRes = IR.drawable.ic_subscription, text = expiryHint)
        NoteRow(iconRes = IR.drawable.ic_locked_large, text = stringResource(LR.string.profile_sub_cancel_hint1), iconTint = colorResource(UR.color.yellow_50))
        NoteRow(iconRes = R.drawable.ic_cloud_strikethrough, text = stringResource(LR.string.profile_sub_cancel_hint2))
    }
}

@Composable
private fun NoteRow(
    @DrawableRes iconRes: Int,
    text: String,
    iconTint: Color? = null,
) {
    Box(modifier = Modifier.padding(start = 32.dp, top = 30.dp, end = 16.dp)) {
        TextViewText(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.theme.colors.primaryText02,
            modifier = Modifier
                .padding(start = 40.dp)
                .fillMaxWidth(),
        )
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.matchParentSize(),
        ) {
            DrawableImage(
                drawableId = iconRes,
                contentDescription = stringResource(LR.string.profile_plus),
                tint = iconTint,
                modifier = Modifier.requiredSize(24.dp),
            )
        }
    }
}

@Preview
@Composable
private fun SubCancelledPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        SubCancelledPage(
            expiryHint = "",
            onDoneClick = {},
        )
    }
}
