package au.com.shiftyjelly.pocketcasts.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.GradientIcon
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.plusGradientBrush
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.account.R as AR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TrialFinishedPage(
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.theme.colors.primaryUi01)
            .fillMaxSize()
            .nestedScroll(rememberNestedScrollInteropConnection()),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GradientIcon(
                painter = painterResource(AR.drawable.ic_subscription_cancelled),
                contentDescription = stringResource(LR.string.plus_subscription_finished),
                gradientBrush = Brush.plusGradientBrush,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(140.dp),
            )

            TextH20(
                text = stringResource(LR.string.plus_trial_finished),
                textAlign = TextAlign.Center,
                color = MaterialTheme.theme.colors.primaryText01,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp),
            )

            TextP30(
                text = stringResource(LR.string.plus_trial_finished_detail),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.W400,
                color = MaterialTheme.theme.colors.primaryText02,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
            )

            TrialFinishedNotesCard(
                modifier = Modifier
                    .padding(bottom = 32.dp),
            )
        }

        TrialFinishedFooter(
            onUpgradeClick = onUpgradeClick,
            onDoneClick = onDoneClick,
        )
    }
}

@Composable
private fun TrialFinishedFooter(
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    Card(
        elevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi01)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 8.dp),
        ) {
            RowOutlinedButton(
                text = stringResource(LR.string.plus_upgrade_to_pocket_casts_plus),
                onClick = { onUpgradeClick.invoke() },
                includePadding = false,
                textPadding = 0.dp,
                fontWeight = FontWeight.W500,
                border = null,
                fullWidth = false,
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = MaterialTheme.theme.colors.primaryInteractive01,
                ),
                modifier = Modifier.padding(vertical = 8.dp),
            )

            RowButton(
                text = stringResource(LR.string.done),
                onClick = { onDoneClick.invoke() },
                includePadding = false,
                fontSize = 18.sp,
                textColor = MaterialTheme.theme.colors.primaryInteractive02,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
                ),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrialFinishedPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        TrialFinishedPage(
            onUpgradeClick = {},
            onDoneClick = {},
        )
    }
}
