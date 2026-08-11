package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import coil3.compose.AsyncImage

@Composable
fun TvCategoryTile(
    category: DiscoverCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val contentColor = if (isFocused) MaterialTheme.tvColors.textPrimary else MaterialTheme.tvColors.textSecondary

    TvTile(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.tvColors.backgroundOverlay,
            focusedContainerColor = MaterialTheme.tvColors.backgroundOverlay,
        ),
        modifier = modifier
            .width(280.dp)
            .height(128.dp)
            .onFocusChanged { isFocused = it.isFocused },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AsyncImage(
                model = category.icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = category.name,
                style = MaterialTheme.tvTypography.body,
                color = contentColor,
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvCategoryTilePreview() {
    TvTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.tvColors.backgroundSunken)
                .padding(48.dp),
        ) {
            TvCategoryTile(
                category = DiscoverCategory(id = 1, name = "Comedy", icon = "", source = ""),
                onClick = {},
            )
        }
    }
}
