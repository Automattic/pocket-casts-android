package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R

@Composable
fun GiveRatingErrorScreen(
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextH40(
            text = stringResource(R.string.something_went_wrong_to_rate_this_podcast),
            color = MaterialTheme.theme.colors.primaryText01,
            textAlign = TextAlign.Center,
        )

        NavigationIconButton(
            iconColor = MaterialTheme.theme.colors.primaryText01,
            navigationButton = NavigationButton.Close,
            onNavigationClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart),
        )
    }
}
