package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.res.Resources
import android.view.MenuItem
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectAction
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodeAction
import org.xmlpull.v1.XmlPullParser

data class MultiSelectToolbarItem(
    val actionId: Int,
    @StringRes val title: Int,
    @DrawableRes val iconRes: Int,
)

sealed interface MultiSelectToolbarOverflow {
    data object None : MultiSelectToolbarOverflow

    data class BottomSheet(
        val actionIds: List<Int>,
        val showRemoveListeningHistory: Boolean,
    ) : MultiSelectToolbarOverflow

    data class Menu(val items: List<MultiSelectToolbarItem>) : MultiSelectToolbarOverflow
}

data class MultiSelectToolbarItems(
    val icons: List<MultiSelectToolbarItem>,
    val overflow: MultiSelectToolbarOverflow,
) {
    companion object {
        val Empty = MultiSelectToolbarItems(emptyList(), MultiSelectToolbarOverflow.None)
    }
}

enum class MultiSelectOverflowStyle {
    BottomSheet,
    Menu,
    None,
}

fun multiSelectToolbarItems(
    actions: List<MultiSelectAction>,
    maxIcons: Int,
    overflowStyle: MultiSelectOverflowStyle,
    sourceView: SourceView?,
): MultiSelectToolbarItems {
    val isListeningHistory = sourceView == SourceView.LISTENING_HISTORY
    val visibleActions = actions.filter { action ->
        if (action is MultiSelectEpisodeAction.RemoveListeningHistory) isListeningHistory else action.isVisible
    }.take(maxIcons)
    val overflowActions = actions - visibleActions
    return MultiSelectToolbarItems(
        icons = visibleActions.map { it.toToolbarItem() },
        overflow = when (overflowStyle) {
            MultiSelectOverflowStyle.BottomSheet -> MultiSelectToolbarOverflow.BottomSheet(
                actionIds = overflowActions.map { it.actionId },
                showRemoveListeningHistory = isListeningHistory,
            )

            MultiSelectOverflowStyle.Menu -> MultiSelectToolbarOverflow.Menu(overflowActions.map { it.toToolbarItem() })

            MultiSelectOverflowStyle.None -> MultiSelectToolbarOverflow.None
        },
    )
}

fun staticMultiSelectToolbarItems(resources: Resources, @MenuRes menuRes: Int): MultiSelectToolbarItems {
    val icons = mutableListOf<MultiSelectToolbarItem>()
    val overflow = mutableListOf<MultiSelectToolbarItem>()
    resources.getXml(menuRes).use { parser ->
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG || parser.name != "item") continue
            val item = MultiSelectToolbarItem(
                actionId = parser.getAttributeResourceValue(ANDROID_NAMESPACE, "id", 0),
                title = parser.getAttributeResourceValue(ANDROID_NAMESPACE, "title", 0),
                iconRes = parser.getAttributeResourceValue(ANDROID_NAMESPACE, "icon", 0),
            )
            val showAsAction = (0 until parser.attributeCount)
                .firstOrNull { parser.getAttributeName(it) == "showAsAction" }
                ?.let { parser.getAttributeIntValue(it, 0) } ?: 0
            if (showAsAction and MenuItem.SHOW_AS_ACTION_ALWAYS != 0) icons += item else overflow += item
        }
    }
    return MultiSelectToolbarItems(icons, MultiSelectToolbarOverflow.Menu(overflow))
}

private fun MultiSelectAction.toToolbarItem() = MultiSelectToolbarItem(actionId, title, iconRes)

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
