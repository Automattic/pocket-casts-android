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
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50

@Composable
fun UpgradeFeatureItem(
    item: UpgradeFeatureItem,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
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
            tint = color,
            modifier = modifier
                .size(20.dp)
                .padding(2.dp),
        )
        Spacer(Modifier.width(16.dp))
        TextH50(
            text = stringResource(item.title),
            color = color,
        )
    }
}
