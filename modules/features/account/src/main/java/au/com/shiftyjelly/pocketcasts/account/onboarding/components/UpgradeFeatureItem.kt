package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.compose.text.HtmlText
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun UpgradeFeatureItem(
    item: UpgradeFeatureItem,
    modifier: Modifier = Modifier,
    iconColor: Color = Color.Black,
    textColor: Color = Color.Black,
    iconSize: Dp = 20.dp,
    spacing: Dp = 16.dp,
) {
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(item.image),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(iconSize)
                .padding(2.dp),
        )
        Spacer(Modifier.width(spacing))
        HtmlText(
            html = stringResource(id = item.title()),
            color = textColor,
            linkColor = textColor,
            textStyleResId = UR.style.H50,
        )
    }
}
