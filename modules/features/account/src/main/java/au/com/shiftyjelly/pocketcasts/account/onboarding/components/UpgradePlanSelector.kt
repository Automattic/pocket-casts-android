package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType

@Composable
fun UpgradePlanSelector(
    plan: String,
    priceAndPeriod: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    savings: String? = null,
    pricePerWeek: String? = null,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val savingsBadge = savings?.let {
            val badgeMeasurable = subcompose("savings") {
                SavingsLabel(
                    savings = savings,
                )
            }
            badgeMeasurable.first().measure(constraints)
        }
        val badgeHeight = savingsBadge?.height ?: 0

        val row = subcompose("selector_row") {
            PlanRow(
                modifier = Modifier.fillMaxWidth(),
                plan = plan,
                priceAndPeriod = priceAndPeriod,
                isSelected = isSelected,
                onSelected = onSelected,
                pricePerWeek = pricePerWeek,
            )
        }
        val placeable = row.first().measure(constraints)
        val rowWidth = placeable.width
        val rowHeight = placeable.height

        val totalHeight = rowHeight + badgeHeight / 2
        layout(rowWidth, totalHeight) {
            placeable.placeRelative(x = 0, y = badgeHeight / 2)
            savingsBadge?.let {
                it.placeRelative(x = (rowWidth - it.width) / 2, y = 0)
            }
        }
    }
}

@Composable
private fun PlanRow(
    plan: String,
    priceAndPeriod: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    pricePerWeek: String? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.theme.colors.primaryInteractive01,
                        shape = RoundedCornerShape(12.dp),
                    )
                } else {
                    Modifier
                },
            )
            .background(
                color = MaterialTheme.theme.colors.primaryUi03,
            )
            .clickable { onSelected() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    if (isSelected) {
                        Modifier.background(color = MaterialTheme.theme.colors.primaryInteractive01)
                    } else {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.theme.colors.primaryIcon02,
                            shape = CircleShape,
                        )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "",
                    tint = MaterialTheme.theme.colors.primaryInteractive02,
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            TextH40(
                text = plan,
                color = MaterialTheme.theme.colors.primaryText01,
                fontWeight = FontWeight.W700,
            )
            TextH40(
                text = priceAndPeriod,
                color = MaterialTheme.theme.colors.secondaryText02,
            )
        }
        pricePerWeek?.let {
            TextH40(
                text = pricePerWeek,
                color = MaterialTheme.theme.colors.secondaryText02,
            )
        }
    }
}

@Composable
private fun SavingsLabel(
    savings: String,
    modifier: Modifier = Modifier,
) {
    TextH50(
        modifier = modifier
            .background(
                color = MaterialTheme.theme.colors.primaryInteractive01,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 2.dp),
        text = savings,
        color = MaterialTheme.theme.colors.primaryInteractive02,
    )
}

@Preview
@Composable
private fun PreviewUpgradePlanSelectors(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UpgradePlanSelector(
                plan = "Annual plan",
                priceAndPeriod = "$33.99/year",
                isSelected = false,
                onSelected = {},
                pricePerWeek = "1.11/week",
            )
            UpgradePlanSelector(
                plan = "Annual plan",
                priceAndPeriod = "$33.99/year",
                isSelected = true,
                onSelected = {},
                pricePerWeek = "1.11/week",
            )
            UpgradePlanSelector(
                plan = "Annual plan",
                priceAndPeriod = "$33.99/year",
                isSelected = false,
                onSelected = {},
                savings = "Save 12%",
            )
            UpgradePlanSelector(
                plan = "Annual plan",
                priceAndPeriod = "$33.99/year",
                isSelected = true,
                onSelected = {},
                savings = "Save 12%",
            )
        }
    }
}
