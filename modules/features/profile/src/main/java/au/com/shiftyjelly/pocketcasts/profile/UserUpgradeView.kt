package au.com.shiftyjelly.pocketcasts.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.components.ProductAmountView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.images.HorizontalLogoPlus
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.profile.UserUpgradeViewData.WithTrial
import au.com.shiftyjelly.pocketcasts.profile.UserUpgradeViewData.WithoutTrial
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.settings.R as SR

@Composable
fun UserUpgradeView(
    data: UserUpgradeViewData,
    storageLimit: Long,
    onLearnMoreClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .background(MaterialTheme.theme.colors.primaryUi02)
            .padding(vertical = 16.dp, horizontal = 24.dp)
    ) {

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(26.dp)
                .fillMaxWidth()
        ) {
            HorizontalLogoPlus(Modifier.weight(1f, fill = false))

            when (data) {
                is WithTrial -> ProductAmountView(
                    primaryText = data.numFree,
                    secondaryText = data.thenPriceSlashPeriod,
                    emphasized = false
                )
                is WithoutTrial -> ProductAmountView(
                    primaryText = data.pricePerPeriod,
                    emphasized = false
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        PlusFeatureRow(stringResource(LR.string.profile_web_player))
        Spacer(Modifier.height(8.dp))
        PlusFeatureRow(stringResource(LR.string.profile_extra_themes))
        Spacer(Modifier.height(8.dp))
        PlusFeatureRow(stringResource(LR.string.profile_extra_app_icons))
        Spacer(Modifier.height(8.dp))
        PlusFeatureRow(stringResource(LR.string.plus_cloud_storage_limit, storageLimit))

        Spacer(Modifier.height(32.dp))

        TextH50(
            text = stringResource(LR.string.plus_learn_more_about_plus),
            color = MaterialTheme.theme.colors.primaryInteractive01,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onLearnMoreClick() }
        )

        Spacer(Modifier.height(8.dp))

        val buttonText = stringResource(
            when (data) {
                is WithTrial -> LR.string.profile_start_free_trial
                is WithoutTrial -> LR.string.profile_upgrade_to_plus
            }
        )
        RowButton(
            text = buttonText,
            onClick = onUpgradeClick,
            includePadding = false
        )
    }
}

sealed interface UserUpgradeViewData {
    class WithTrial(
        val numFree: String,
        val thenPriceSlashPeriod: String,
    ) : UserUpgradeViewData

    class WithoutTrial(
        val pricePerPeriod: String
    ) : UserUpgradeViewData
}

@Composable
private fun PlusFeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(SR.drawable.ic_plus),
            contentDescription = null,
            modifier = Modifier.padding(top = 3.dp) // for better visual alignment of image with text
        )

        TextH40(
            text = text,
            color = MaterialTheme.theme.colors.primaryText02,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserUpgradeViewPreview_with_trial(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        UserUpgradeView(
            data = WithTrial(
                numFree = "1 month free",
                thenPriceSlashPeriod = "then $0.99 / month",
            ),
            storageLimit = 10L,
            onLearnMoreClick = {},
            onUpgradeClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserUpgradeViewPreview_without_trial(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        UserUpgradeView(
            data = WithoutTrial(pricePerPeriod = "$0.99 per month"),
            storageLimit = 10L,
            onLearnMoreClick = {},
            onUpgradeClick = {}
        )
    }
}
