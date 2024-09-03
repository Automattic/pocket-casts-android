package au.com.shiftyjelly.pocketcasts.compose.rearrange

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuActionRearrange(
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null,
    menuActions: List<MenuAction>,
    onActionsOrderChanged: (List<MenuAction>) -> Unit,
    onActionMoved: (fromIndex: Int, toIndex: Int, action: MenuAction) -> Unit = { _, _, _ -> },
    chooseCount: Int,
    actionsTitle: String? = null,
    otherActionsTitle: String,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    hoverColor: Color = MaterialTheme.theme.colors.primaryUi02Active,
    textColor: Color = MaterialTheme.theme.colors.primaryText01,
    titleTextColor: Color = MaterialTheme.theme.colors.primaryText02,
    iconColor: Color = MaterialTheme.theme.colors.primaryIcon01,
) {
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val actions = menuActions.toMutableList().apply {
            // adjust for the header items
            var toIndex = to.index - 1
            var fromIndex = from.index - 1
            // adjust for the sticky header after divider
            if (toIndex > chooseCount) {
                toIndex -= 1
            }
            if (fromIndex > chooseCount) {
                fromIndex -= 1
            }
            val action = removeAt(fromIndex)
            onActionMoved(fromIndex, toIndex, action)
            add(toIndex, action)
        }
        onActionsOrderChanged(actions)
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        item {
            Column {
                if (header != null) {
                    header()
                }
                if (actionsTitle != null) {
                    TextH40(
                        text = actionsTitle,
                        color = titleTextColor,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }

        menuActions.forEachIndexed { index, action ->
            if (index == chooseCount) {
                stickyHeader {
                    TextH40(
                        text = otherActionsTitle,
                        color = titleTextColor,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            item(key = action.key) {
                ReorderableItem(reorderableLazyListState, key = action.key) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                    val color = if (isDragging) hoverColor else Color.Transparent
                    val resources = LocalContext.current.resources
                    Surface(elevation = elevation, color = color) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .longPressDraggableHandle()
                                .clickable {}
                                .semantics {
                                    customActions = accessibilityActions(index = index, menuActions = menuActions, onActionsOrderChanged = onActionsOrderChanged, resources = resources)
                                },
                        ) {
                            Spacer(Modifier.width(24.dp))
                            Icon(
                                painter = painterResource(action.icon),
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = stringResource(action.name),
                                color = textColor,
                                modifier = Modifier.padding(16.dp),
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                modifier = Modifier
                                    .draggableHandle()
                                    .clearAndSetSemantics {},
                                onClick = {},
                            ) {
                                Icon(
                                    painter = painterResource(IR.drawable.ic_reorder),
                                    contentDescription = null,
                                    tint = iconColor,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}

fun accessibilityActions(index: Int, menuActions: List<MenuAction>, onActionsOrderChanged: (List<MenuAction>) -> Unit, resources: Resources): List<CustomAccessibilityAction> {
    return listOf(
        CustomAccessibilityAction(
            label = resources.getString(LR.string.move_up),
            action = {
                if (index > 0) {
                    val list = menuActions.toMutableList().apply {
                        add(index - 1, removeAt(index))
                    }
                    onActionsOrderChanged(list)
                    true
                } else {
                    false
                }
            },
        ),
        CustomAccessibilityAction(
            label = resources.getString(LR.string.move_down),
            action = {
                if (index < menuActions.size - 1) {
                    val list = menuActions.toMutableList().apply {
                        add(index + 1, removeAt(index))
                    }
                    onActionsOrderChanged(list)
                    true
                } else {
                    false
                }
            },
        ),
    )
}

data class MenuAction(
    val key: String,
    @StringRes val name: Int,
    @DrawableRes val icon: Int,
)

@ShowkaseComposable(name = "MenuActionRearrange", group = "Rearrange")
@Preview
@Composable
fun MenuActionRearrangePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        MenuActionRearrange(
            header = {
                TextH10(
                    text = "Header",
                    color = MaterialTheme.theme.colors.primaryText01,
                    modifier = Modifier.padding(24.dp),
                )
            },
            menuActions = listOf(
                MenuAction("1", LR.string.archive, IR.drawable.ic_archive),
                MenuAction("2", LR.string.mark_as_played, IR.drawable.ic_markasplayed),
                MenuAction("3", LR.string.play_next, IR.drawable.ic_skip_next),
                MenuAction("4", LR.string.playback_speed, IR.drawable.ic_speed_number),
                MenuAction("5", LR.string.star, IR.drawable.ic_star),
            ),
            onActionsOrderChanged = {},
            chooseCount = 3,
            actionsTitle = "Shortcut on player",
            otherActionsTitle = "Other Actions",
        )
    }
}
