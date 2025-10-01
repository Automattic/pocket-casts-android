package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.rememberViewInteropNestedScrollConnection
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatLongStyle
import java.time.Instant
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun CancelConfirmationPage(
    expirationDate: Instant?,
    onKeepSubscription: () -> Unit,
    onCancelSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(rememberViewInteropNestedScrollConnection())
            .padding(top = 24.dp, bottom = 16.dp),
    ) {
        Header(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        )
        Perks(
            expirationDate = expirationDate,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 40.dp, horizontal = 20.dp),
        )
        Buttons(
            onKeepSubscription = onKeepSubscription,
            onCancelSubscription = onCancelSubscription,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(LR.string.winback_cancel_subscription_header_title),
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 33.5.sp,
            color = MaterialTheme.theme.colors.primaryText01,
            textAlign = TextAlign.Center,
        )
        TextP40(
            text = stringResource(LR.string.winback_cancel_subscription_header_description),
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Perks(
    expirationDate: Instant?,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier,
    ) {
        PerkRow(
            image = painterResource(IR.drawable.ic_subscription),
            text = if (expirationDate != null) {
                val formattedDate = expirationDate.let(Date::from).toLocalizedFormatLongStyle()
                val text = stringResource(LR.string.winback_cancel_subscription_perk_active_with_date, formattedDate)
                buildAnnotatedString {
                    append(text)
                    val start = text.indexOf(formattedDate)
                    addStyle(
                        style = SpanStyle(
                            color = MaterialTheme.theme.colors.primaryIcon01,
                        ),
                        start = start,
                        end = start + formattedDate.length,
                    )
                }
            } else {
                AnnotatedString(stringResource(LR.string.winback_cancel_subscription_perk_active_without_date))
            },
        )
        PerkRow(
            image = painterResource(IR.drawable.ic_locked_large),
            text = stringResource(LR.string.winback_cancel_subscription_perk_plus),
        )
        PerkRow(
            image = painterResource(IR.drawable.ic_folder_lock),
            text = stringResource(LR.string.winback_cancel_subscription_perk_folders),
        )
        PerkRow(
            image = painterResource(IR.drawable.ic_remove_from_cloud),
            text = stringResource(LR.string.winback_cancel_subscription_perk_files),
        )
    }
}

@Composable
private fun Buttons(
    onKeepSubscription: () -> Unit,
    onCancelSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp),
            onClick = onKeepSubscription,
        ) {
            TextH30(
                text = stringResource(LR.string.winback_cancel_subscription_stay_button_label),
                color = MaterialTheme.theme.colors.primaryInteractive02,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            border = BorderStroke(2.dp, MaterialTheme.theme.colors.support05),
            shape = RoundedCornerShape(16.dp),
            onClick = onCancelSubscription,
        ) {
            TextH30(
                text = stringResource(LR.string.winback_cancel_subscription_cancel_button_label),
                color = MaterialTheme.theme.colors.support05,
            )
        }
    }
}

@Composable
private fun PerkRow(
    image: Painter,
    text: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        Icon(
            painter = image,
            tint = MaterialTheme.theme.colors.primaryIcon01,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        TextP40(
            text = text,
            fontSize = 15.sp,
            lineHeight = 21.sp,
        )
    }
}

@Composable
private fun PerkRow(
    image: Painter,
    text: String,
    modifier: Modifier = Modifier,
) {
    PerkRow(image, AnnotatedString(text), modifier)
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun CancelConfirmationPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppThemeWithBackground(theme) {
        CancelConfirmationPage(
            expirationDate = Instant.ofEpochSecond(1700000000),
            onKeepSubscription = {},
            onCancelSubscription = {},
        )
    }
}
