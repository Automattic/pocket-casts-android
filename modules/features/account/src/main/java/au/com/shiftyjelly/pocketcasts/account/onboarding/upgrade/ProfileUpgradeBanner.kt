package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.IconRow
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ProfileUpgradeBannerViewModel
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ProfileUpgradeBanner(
    onClick: () -> Unit
) {
    val state by hiltViewModel<ProfileUpgradeBannerViewModel>()
        .state
        .collectAsState()

    OnboardingUpgradeHelper.OldPlusBackground {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(24.dp))
            IconRow()

            Spacer(Modifier.height(16.dp))
            TextH60(
                text = stringResource(LR.string.plus_take_your_podcasting_to_next_level),
                color = Color.White,
            )

            state.numPeriodFree?.let { numPeriodFree ->
                Spacer(Modifier.height(16.dp))
                OnboardingUpgradeHelper.TopText(topText = numPeriodFree)
            }

            Spacer(Modifier.height(20.dp))

            // Not using a lazy grid here because lazy grids cannot be used inside a ScrollView
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlusUpgradeFeatureItem.values().toList().chunked(2).forEach { chunk ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        chunk.forEach {
                            FeatureItemComposable(it, Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
            OnboardingUpgradeHelper.PlusRowButton(
                text = stringResource(LR.string.profile_upgrade_to_plus),
                onClick = onClick,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeatureItemComposable(
    item: PlusUpgradeFeatureItem,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(item.image),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(12.dp))
        TextH60(
            text = stringResource(item.title),
            color = Color.White,
        )
    }
}

@Preview
@Composable
private fun ProfileUpgradeBannerPreview() {
    ProfileUpgradeBanner(
        onClick = { }
    )
}
