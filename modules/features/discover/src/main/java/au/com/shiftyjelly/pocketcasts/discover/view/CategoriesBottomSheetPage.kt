package au.com.shiftyjelly.pocketcasts.discover.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewAlignment
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import coil3.compose.AsyncImage
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val SelectCategoriesTitleColor = Color(0xFF03A9F4)

@Composable
internal fun CategoriesBottomSheetPage(
    categories: List<DiscoverCategory>,
    contentPadding: PaddingValues,
    onCategoryClick: (DiscoverCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().nestedScroll(rememberNestedScrollInteropConnection())) {
        val title = stringResource(LR.string.discover_select_categories)
        val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]
        TextViewText(
            text = if (locale == null) title.uppercase() else title.uppercase(locale),
            fontSize = 13.sp,
            color = SelectCategoriesTitleColor,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.03.em,
            lineSpacingExtra = 2.sp,
            modifier = Modifier
                .fillMaxWidth()
                .scrollable(rememberScrollableState { 0f }, Orientation.Vertical)
                .padding(start = 16.dp, top = 24.dp, end = 16.dp)
                .semantics { contentDescription = title },
        )
        LazyColumn(
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    onClick = { onCategoryClick(category) },
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: DiscoverCategory,
    onClick: () -> Unit,
) {
    val name = category.name.tryToLocalise(LocalResources.current)
    Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
        ) {
            AsyncImage(
                model = category.icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon02),
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                contentAlignment = TextViewAlignment.CenterStart,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                TextViewText(
                    text = name,
                    fontSize = 16.sp,
                    color = MaterialTheme.theme.colors.primaryText01,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = name },
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.theme.colors.primaryUi05),
        )
    }
}
