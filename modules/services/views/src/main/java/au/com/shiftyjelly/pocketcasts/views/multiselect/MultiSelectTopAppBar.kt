package au.com.shiftyjelly.pocketcasts.views.multiselect

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectController
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectToolbarItem
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectToolbarOverflow
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.MultiSelectViewOverflowMenuShownEvent
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun <T> MultiSelectTopAppBar(
    controller: MultiSelectController<T>,
    eventHorizon: EventHorizon,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val selectedCount by controller.selectedCount.collectAsStateWithLifecycle()
    val items by controller.toolbarItems.collectAsStateWithLifecycle()
    val onItemClick = { itemId: Int ->
        context.getActivity()?.let { activity -> controller.onMenuItemSelected(itemId, resources, activity) }
        Unit
    }
    ThemedTopAppBar(
        title = selectedCount.toString(),
        onNavigationClick = controller::exit,
        iconColor = MaterialTheme.theme.colors.primaryInteractive02,
        textColor = MaterialTheme.theme.colors.primaryInteractive02,
        backgroundColor = MaterialTheme.theme.colors.support01,
        modifier = modifier,
        actions = { tint ->
            items.icons.forEach { item ->
                ActionButton(item.iconRes, stringResource(item.title), tint) { onItemClick(item.actionId) }
            }
            when (val overflow = items.overflow) {
                is MultiSelectToolbarOverflow.BottomSheet -> ActionButton(IR.drawable.ic_more_vert_black_24dp, stringResource(LR.string.more_options), tint) {
                    val helper = controller.helper as? MultiSelectEpisodesHelper
                    if (helper != null) {
                        eventHorizon.track(MultiSelectViewOverflowMenuShownEvent(source = helper.source.analyticsValue))
                        context.getActivity()?.supportFragmentManager?.let { fragmentManager ->
                            MultiSelectBottomSheet.newInstance(overflow.actionIds, overflow.showRemoveListeningHistory)
                                .apply { multiSelectHelper = helper }
                                .show(fragmentManager, "multiselectbottomsheet")
                        }
                    }
                }

                is MultiSelectToolbarOverflow.Menu -> if (overflow.items.isNotEmpty()) {
                    OverflowMenu(overflow.items, tint, onItemClick)
                }

                MultiSelectToolbarOverflow.None -> Unit
            }
        },
    )
}

@Composable
private fun ActionButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(painterResource(iconRes), contentDescription, tint = tint)
    }
}

@Composable
private fun OverflowMenu(
    items: List<MultiSelectToolbarItem>,
    tint: Color,
    onItemClick: (Int) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        ActionButton(IR.drawable.ic_more_vert_black_24dp, stringResource(LR.string.more_options), tint) { isExpanded = true }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        isExpanded = false
                        onItemClick(item.actionId)
                    },
                ) {
                    Text(stringResource(item.title), color = MaterialTheme.theme.colors.primaryText01)
                }
            }
        }
    }
}
