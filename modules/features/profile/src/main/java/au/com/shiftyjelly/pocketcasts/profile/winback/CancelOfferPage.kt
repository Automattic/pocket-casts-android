package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import androidx.compose.material.Divider as MaterialDivider
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun CancelOfferPage(
    onSeeAvailablePlans: () -> Unit,
    onSeeHelpAndFeedback: () -> Unit,
    onContinueToCancellation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(rememberNestedScrollInteropConnection())
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            modifier = Modifier.height(24.dp),
        )
        CancelOfferHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )

        AvailablePlans(
            onClick = onSeeAvailablePlans,
            modifier = Modifier.fillMaxWidth(),
        )
        Divider()
        HelpAndFeedback(
            onClick = onSeeHelpAndFeedback,
            modifier = Modifier.fillMaxWidth(),
        )
        Divider()
        Spacer(
            modifier = Modifier.weight(1f),
        )
        ContinueToCancellation(
            onClick = onContinueToCancellation,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(
            modifier = Modifier.height(52.dp),
        )
    }
}

@Composable
private fun CancelOfferHeader(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(LR.string.winback_offer_header),
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 33.5.sp,
        color = MaterialTheme.theme.colors.primaryText01,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun AvailablePlans(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RowWithIcon(
        icon = painterResource(IR.drawable.ic_different_plan),
        modifier = modifier
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        ChevronRow {
            Column {
                TextH40(
                    text = stringResource(LR.string.winback_offer_differnt_plan_title),
                )
                Spacer(
                    modifier = Modifier.height(4.dp),
                )
                TextH60(
                    text = stringResource(LR.string.winback_offer_differnt_plan_description),
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
        }
    }
}

@Composable
private fun HelpAndFeedback(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RowWithIcon(
        icon = painterResource(IR.drawable.ic_help),
        modifier = modifier
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        ChevronRow {
            Column {
                TextH40(
                    text = stringResource(LR.string.winback_offer_help_title),
                )
                Spacer(
                    modifier = Modifier.height(4.dp),
                )
                TextH60(
                    text = stringResource(LR.string.winback_offer_help_description),
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
        }
    }
}

@Composable
private fun ContinueToCancellation(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        border = BorderStroke(2.dp, MaterialTheme.theme.colors.primaryIcon01),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        TextH30(
            text = stringResource(LR.string.winback_offer_cancel_continue_button_label),
            color = MaterialTheme.theme.colors.primaryIcon01,
        )
    }
}

@Composable
private fun RowWithIcon(
    icon: Painter,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        Image(
            painter = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon01),
        )
        content()
    }
}

@Composable
private fun ChevronRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        content()
        Spacer(
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(IR.drawable.ic_chevron_small_right),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon02),
        )
    }
}

@Composable
private fun Divider(
    modifier: Modifier = Modifier,
) {
    MaterialDivider(
        color = MaterialTheme.theme.colors.primaryUi05,
        startIndent = 16.dp,
        modifier = modifier,
    )
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun CancelOfferpageThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppThemeWithBackground(
        themeType = theme,
    ) {
        CancelOfferPage(
            onSeeAvailablePlans = {},
            onSeeHelpAndFeedback = {},
            onContinueToCancellation = {},
        )
    }
}
