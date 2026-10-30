package au.com.shiftyjelly.pocketcasts.discover.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.bars.AppCompatTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewAlignment
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.text.rememberTextAppearanceColor
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun RegionSelectPage(
    regions: List<DiscoverRegion>,
    selectedRegionCode: String?,
    bottomInset: PaddingValues,
    onRegionClick: (DiscoverRegion) -> Unit,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
    toolbarModifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AppCompatTopAppBar(
            title = stringResource(LR.string.discover_select_a_region),
            navigationButton = NavigationButton.Drawable(IR.drawable.ic_arrow_back, LR.string.back),
            onNavigationClick = onNavigationClick,
            iconColor = MaterialTheme.theme.colors.secondaryIcon01,
            titleColor = MaterialTheme.theme.colors.secondaryText01,
            backgroundColor = MaterialTheme.theme.colors.secondaryUi01,
            bottomShadow = false,
            modifier = toolbarModifier,
        )
        val titleColor = rememberTextAppearanceColor(UR.attr.textSubtitle1)
        LazyColumn(
            contentPadding = bottomInset,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            items(regions, key = { it.code }) { region ->
                RegionRow(
                    region = region,
                    isSelected = region.code == selectedRegionCode,
                    titleColor = titleColor,
                    modifier = Modifier.clickable { onRegionClick(region) },
                )
            }
        }
    }
}

@Composable
private fun RegionRow(
    region: DiscoverRegion,
    isSelected: Boolean,
    titleColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(region.flag).allowHardware(false).build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            contentAlignment = TextViewAlignment.CenterStart,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 16.dp),
        ) {
            TextViewText(
                text = region.name.tryToLocalise(LocalResources.current),
                fontSize = 16.sp,
                color = titleColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (isSelected) {
            DrawableImage(
                drawableId = IR.drawable.ic_tick,
                contentDescription = null,
                tint = MaterialTheme.theme.colors.primaryIcon01,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
